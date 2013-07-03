/*
 * #%L
 * mosaic-components-tools
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.components.implementations.basic;


import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelController;
import eu.mosaic_cloud.components.core.ChannelFlow;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageCoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.core.IgnoredException;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Futures;


public final class BasicChannel
			extends Object
{
	private BasicChannel (final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions) {
		super ();
		this.backend = new Backend (this, input, output, coder, reactor, threading, exceptions);
	}
	
	public void destroy () {
		Preconditions.checkState (this.destroy (-1));
	}
	
	public final boolean destroy (final long timeout) {
		return (this.backend.destroy (timeout));
	}
	
	public final ChannelController getController () {
		return (this.backend.controllerProxy);
	}
	
	public final void initialize () {
		Preconditions.checkState (this.initialize (-1));
	}
	
	public final boolean initialize (final long timeout) {
		return (this.backend.initialize (timeout));
	}
	
	final Backend backend;
	
	public static final BasicChannel create (final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions) {
		return (new BasicChannel (input, output, coder, reactor, threading, exceptions));
	}
	
	static final int defaultBufferSize = 1024;
	static final int defaultMaximumPacketSize = 1024 * 1024;
	static final long defaultPollTimeout = 100;
	
	private static final class Backend
				extends AbstractService
				implements
					ChannelController,
					CallbackProxy
	{
		Backend (final BasicChannel facade, final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions) {
			super ();
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (input);
			Preconditions.checkArgument (input instanceof SelectableChannel);
			Preconditions.checkNotNull (output);
			Preconditions.checkArgument (output instanceof SelectableChannel);
			Preconditions.checkNotNull (coder);
			Preconditions.checkNotNull (reactor);
			Preconditions.checkNotNull (threading);
			this.facade = facade;
			this.monitor = Monitor.create (this.facade);
			synchronized (this.monitor) {
				{
					this.transcript = Transcript.create (this.facade, true);
					this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
				}
				{
					this.input = input;
					this.output = output;
					this.coder = coder;
					final Selector selector;
					try {
						selector = Selector.open ();
					} catch (final Throwable exception) {
						this.exceptions.traceDeferredException (exception);
						throw (new AssertionError (exception));
					}
					this.selector = selector;
				}
				{
					this.reactor = reactor;
					this.controllerProxy = this.reactor.createProxy (ChannelController.class);
					this.callbacksProxy = this.reactor.createProxy (ChannelCallbacks.class);
				}
				{
					this.threading = threading;
					this.executor = this.threading.createCachedThreadPool (this.threading.getThreadConfiguration ().override (this.facade, "workers", true, this.exceptions, this.exceptions.catcher));
					this.inboundPackets = new LinkedBlockingQueue<ByteBuffer> ();
					this.outboundPackets = new LinkedBlockingQueue<ByteBuffer> ();
					this.inboundMessages = new LinkedBlockingQueue<ChannelMessage> ();
					this.outboundMessages = new LinkedBlockingQueue<ChannelMessage> ();
					this.ioputer = new Ioputer (this);
					this.encoder = new Encoder (this);
					this.decoder = new Decoder (this);
					this.dispatcher = new Dispatcher (this);
					this.pollTimeout = BasicChannel.defaultPollTimeout;
				}
				this.transcript.traceDebugging ("created channel.");
			}
		}
		
		@Override
		public final CallbackCompletion<Void> bind (final ChannelCallbacks delegate) {
			this.transcript.traceDebugging ("assigning callbacks...");
			return (this.reactor.assignDelegate (this.callbacksProxy, delegate));
		}
		
		@Override
		public final CallbackCompletion<Void> close (final ChannelFlow flow) {
			try {
				Preconditions.checkNotNull (flow);
				synchronized (this.monitor) {
					switch (flow) {
						case Inbound :
							this.transcript.traceDebugging ("closing inbound flow...");
							try {
								this.input.close ();
							} catch (final Throwable exception) {
								this.exceptions.traceIgnoredException (exception);
							}
							break;
						case Outbound :
							this.transcript.traceDebugging ("closing outbound flow...");
							try {
								this.output.close ();
							} catch (final Throwable exception) {
								this.exceptions.traceIgnoredException (exception);
							}
							break;
						default :
							throw (new AssertionError ());
					}
				}
				return (CallbackCompletion.createOutcome ());
			} catch (final Throwable exception) {
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final CallbackCompletion<Void> send (final ChannelMessage message) {
			try {
				this.transcript.traceDebugging ("sending message...");
				Preconditions.checkNotNull (message);
				synchronized (this.monitor) {
					if (!Threading.offer (this.outboundMessages, message, this.pollTimeout))
						throw (new BufferOverflowException ());
				}
				return (CallbackCompletion.createOutcome ());
			} catch (final Throwable exception) {
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final CallbackCompletion<Void> terminate () {
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("terminating...");
				return (CallbackCompletion.createDeferred (Futures.transform (this.stop (), Functions.constant ((Void) null))));
			}
		}
		
		@Override
		protected final void doStart () {
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("initializing...");
				Preconditions.checkState (this.reactor.assignDelegate (this.controllerProxy, this).await ());
				Preconditions.checkState (this.encoder.startAndWait () == State.RUNNING);
				Preconditions.checkState (this.decoder.startAndWait () == State.RUNNING);
				Preconditions.checkState (this.ioputer.startAndWait () == State.RUNNING);
				Preconditions.checkState (this.dispatcher.startAndWait () == State.RUNNING);
				this.notifyStarted ();
				this.transcript.traceDebugging ("initialized.");
			}
		}
		
		@Override
		protected final void doStop () {
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("destroying...");
				Preconditions.checkState (this.dispatcher.stopAndWait () == State.TERMINATED);
				Preconditions.checkState (this.ioputer.stopAndWait () == State.TERMINATED);
				Preconditions.checkState (this.encoder.stopAndWait () == State.TERMINATED);
				Preconditions.checkState (this.decoder.stopAndWait () == State.TERMINATED);
				this.executor.shutdown ();
				Preconditions.checkState (Threading.join (this.executor));
				Preconditions.checkState (this.reactor.destroyProxy (this.controllerProxy).await ());
				Preconditions.checkState (this.reactor.destroyProxy (this.callbacksProxy).await ());
				this.notifyStopped ();
				this.transcript.traceDebugging ("destroyed.");
			}
		}
		
		final boolean destroy (final long timeout) {
			return (Threading.awaitOrCatch (this.stop (), timeout) == State.TERMINATED);
		}
		
		final boolean initialize (final long timeout) {
			return (Threading.awaitOrCatch (this.start (), timeout) == State.RUNNING);
		}
		
		final ChannelCallbacks callbacksProxy;
		final ChannelMessageCoder coder;
		final ChannelController controllerProxy;
		final Decoder decoder;
		final Dispatcher dispatcher;
		final Encoder encoder;
		final TranscriptExceptionTracer exceptions;
		final ExecutorService executor;
		final BasicChannel facade;
		final LinkedBlockingQueue<ChannelMessage> inboundMessages;
		final LinkedBlockingQueue<ByteBuffer> inboundPackets;
		final ReadableByteChannel input;
		final Ioputer ioputer;
		final Monitor monitor;
		final LinkedBlockingQueue<ChannelMessage> outboundMessages;
		final LinkedBlockingQueue<ByteBuffer> outboundPackets;
		final WritableByteChannel output;
		final long pollTimeout;
		final CallbackReactor reactor;
		final Selector selector;
		final ThreadingContext threading;
		final Transcript transcript;
	}
	
	private static final class Decoder
				extends Worker
	{
		Decoder (final Backend channel) {
			super (channel);
			this.coder = this.channel.coder;
			this.inboundMessages = this.channel.inboundMessages;
			this.inboundPackets = this.channel.inboundPackets;
		}
		
		@Override
		protected void loop_1 ()
					throws CaughtException {
			this.transcript.traceDebugging ("executing decoder...");
			final ByteBuffer packet = Threading.poll (this.inboundPackets, this.pollTimeout);
			if (packet != null) {
				this.transcript.traceDebugging ("decoding inbound message...");
				final ChannelMessage message;
				try {
					message = this.coder.decode (packet);
					Preconditions.checkNotNull (message);
				} catch (final Throwable exception) {
					throw (new IgnoredException (exception, "unexpected error encountered while decoding the inbound packet; aborting!"));
				}
				if (!Threading.offer (this.inboundMessages, message, this.pollTimeout))
					throw (new IgnoredException (new BufferOverflowException (), "queue overflow error encountered while enqueueing inbound message; aborting!"));
			}
			this.transcript.traceDebugging ("executed decoder.");
		}
		
		@Override
		protected final void shutDown_1 () {
			this.transcript.traceDebugging ("destroying decoder...");
			this.transcript.traceDebugging ("destroyed decoder.");
		}
		
		@Override
		protected final void startUp_1 () {
			this.transcript.traceDebugging ("initializing decoder...");
			this.transcript.traceDebugging ("initialized decoder.");
		}
		
		final ChannelMessageCoder coder;
		final LinkedBlockingQueue<ChannelMessage> inboundMessages;
		final LinkedBlockingQueue<ByteBuffer> inboundPackets;
	}
	
	private static final class Dispatcher
				extends Worker
	{
		Dispatcher (final Backend channel) {
			super (channel);
			this.channelCallbacks = this.channel.callbacksProxy;
			this.inboundMessages = this.channel.inboundMessages;
			this.inboundActive = true;
			this.outboundActive = true;
		}
		
		@Override
		protected final void loop_1 ()
					throws CaughtException {
			this.transcript.traceDebugging ("executing dispatcher...");
			final ChannelMessage message = Threading.poll (this.inboundMessages, this.pollTimeout);
			if (message != null) {
				if (this.inboundActive) {
					this.transcript.traceDebugging ("dispatching received callback...");
					try {
						this.channelCallbacks.received (this.channel.controllerProxy, message);
					} catch (final Throwable exception) {
						throw (new IgnoredException (exception, "unexpected error encountered while dispatching received callback; aborting!"));
					}
				} else
					this.transcript.traceError ("discarding received callback due to closed inbound flow;");
			} else {
				if (this.inboundActive && !this.channel.input.isOpen ()) {
					this.transcript.traceDebugging ("dispatching closed inbound flow callback...");
					this.inboundActive = false;
					try {
						this.channelCallbacks.closed (this.channel.controllerProxy, ChannelFlow.Inbound);
					} catch (final Throwable exception) {
						throw (new IgnoredException (exception, "unexpected error encountered while dispatching closed inbound flow callback; aborting!"));
					}
				}
			}
			if (this.outboundActive && !this.channel.output.isOpen ()) {
				this.transcript.traceDebugging ("dispatching closed outbound flow callback...");
				this.outboundActive = false;
				try {
					this.channelCallbacks.closed (this.channel.controllerProxy, ChannelFlow.Outbound);
				} catch (final Throwable exception) {
					throw (new IgnoredException (exception, "unexpected error encountered while dispatching closed outbound flow callback; aborting!"));
				}
			}
			if (!this.outboundActive && !this.inboundActive)
				this.triggerShutdown ();
			this.transcript.traceDebugging ("executed dispatcher.");
		}
		
		@Override
		protected final void shutDown_1 ()
					throws CaughtException {
			this.transcript.traceDebugging ("destroying dispatcher...");
			this.transcript.traceDebugging ("dispatching terminated callback...");
			try {
				this.channelCallbacks.terminated (this.channel.controllerProxy);
			} catch (final Throwable exception) {
				throw (new IgnoredException (exception, "unexpected error encountered while dispatching terminated callback; aborting!"));
			}
			this.transcript.traceDebugging ("destroyed dispatcher.");
		}
		
		@Override
		protected final void startUp_1 ()
					throws CaughtException {
			this.transcript.traceDebugging ("initializing dispatcher...");
			this.transcript.traceDebugging ("dispatching initialized callback...");
			try {
				this.channelCallbacks.initialized (this.channel.controllerProxy);
			} catch (final Throwable exception) {
				throw (new IgnoredException (exception, "unexpected error encountered while dispatching initialized callback; aborting!"));
			}
			this.transcript.traceDebugging ("initialized dispatcher.");
		}
		
		final ChannelCallbacks channelCallbacks;
		boolean inboundActive;
		final LinkedBlockingQueue<ChannelMessage> inboundMessages;
		boolean outboundActive;
	}
	
	private static final class Encoder
				extends Worker
	{
		Encoder (final Backend channel) {
			super (channel);
			this.coder = this.channel.coder;
			this.outboundMessages = this.channel.outboundMessages;
			this.outboundPackets = this.channel.outboundPackets;
		}
		
		@Override
		protected void loop_1 ()
					throws CaughtException {
			this.transcript.traceDebugging ("executing encoder...");
			final ChannelMessage message = Threading.poll (this.outboundMessages, this.pollTimeout);
			if (message != null) {
				this.transcript.traceDebugging ("encoding outbound message...");
				final ByteBuffer packet;
				try {
					packet = this.coder.encode (message);
					Preconditions.checkNotNull (packet);
				} catch (final Throwable exception) {
					throw (new IgnoredException (exception, "unexpected error encountered while encoding the outbound message; aborting!"));
				}
				if (!Threading.offer (this.outboundPackets, packet, this.pollTimeout))
					throw (new IgnoredException (new BufferOverflowException (), "unexpected queue overflow error encountered while enqueueing outbound packet; aborting!"));
				this.channel.selector.wakeup ();
			}
			this.transcript.traceDebugging ("executed encoder.");
		}
		
		@Override
		protected final void shutDown_1 () {
			this.transcript.traceDebugging ("destroying encoder...");
			this.transcript.traceDebugging ("destroyed encoder.");
		}
		
		@Override
		protected final void startUp_1 () {
			this.transcript.traceDebugging ("initializing encoder...");
			this.transcript.traceDebugging ("initialized encoder.");
		}
		
		final ChannelMessageCoder coder;
		final LinkedBlockingQueue<ChannelMessage> outboundMessages;
		final LinkedBlockingQueue<ByteBuffer> outboundPackets;
	}
	
	private static final class Ioputer
				extends Worker
	{
		Ioputer (final Backend channel) {
			super (channel);
			this.input = this.channel.input;
			this.output = this.channel.output;
			this.selector = this.channel.selector;
			this.inboundPackets = this.channel.inboundPackets;
			this.outboundPackets = this.channel.outboundPackets;
			this.maximumPacketSize = BasicChannel.defaultMaximumPacketSize;
			this.inputBufferSize = BasicChannel.defaultBufferSize;
			this.inputKey = null;
			this.outputKey = null;
			this.inputPending = null;
			this.inputPendingSize = -1;
			this.outputPending = null;
		}
		
		@Override
		protected void loop_1 ()
					throws CaughtException {
			this.transcript.traceDebugging ("executing flows...");
			if (this.input.isOpen ()) {
				if (this.inputPending == null) {
					this.inputPending = ByteBuffer.allocate (this.inputBufferSize);
					this.inputPendingSize = -1;
				}
			} else {
				if (this.inputPending != null) {
					if (this.inputPending.position () > 0)
						this.transcript.traceError ("discarding inbound packet due to closed inbound flow;");
					this.inputPending = null;
				}
			}
			if (this.output.isOpen ()) {
				if (this.outputPending == null) {
					final ByteBuffer packet_ = this.outboundPackets.poll ();
					if (packet_ != null) {
						final ByteBuffer packet;
						try {
							packet = packet_.asReadOnlyBuffer ();
							Preconditions.checkArgument (packet.order () == ByteOrder.BIG_ENDIAN, "invalid packet byte-order");
							Preconditions.checkArgument (packet.remaining () >= 4, "invalid packet framing");
							packet.mark ();
							final int packetSize = packet.getInt ();
							Preconditions.checkArgument (packetSize == packet.remaining (), "invalid outbound packet encoding");
							packet.reset ();
						} catch (final IllegalArgumentException exception) {
							throw (new IgnoredException (exception, "unexpected validation error encountered while polling outbound packet; aborting!"));
						}
						this.outputPending = packet;
					}
				}
			} else {
				if (this.outputPending != null) {
					this.transcript.traceError ("discarding outbound packet due to closed outbound flow;");
					this.outputPending = null;
				}
			}
			try {
				if ((this.inputPending != null) && (this.inputKey == null))
					this.inputKey = ((SelectableChannel) this.input).register (this.selector, SelectionKey.OP_READ);
				else if ((this.inputKey != null) && (this.inputPending == null)) {
					this.inputKey.cancel ();
					this.inputKey = null;
				}
				if ((this.outputPending != null) && (this.outputKey == null) && this.output.isOpen ())
					this.outputKey = ((SelectableChannel) this.output).register (this.selector, SelectionKey.OP_WRITE);
				else if ((this.outputKey != null) && (this.outputPending == null)) {
					this.outputKey.cancel ();
					this.outputKey = null;
				}
			} catch (final IOException exception) {
				throw (new IgnoredException (exception, "i/o error encountered while polling flows; aborting!"));
			}
			try {
				this.selector.select (this.pollTimeout);
			} catch (final IOException exception) {
				throw (new IgnoredException (exception, "i/o error encountered while polling flows; aborting!"));
			}
			final boolean inputValid;
			final boolean outputValid;
			if (this.inputKey != null) {
				inputValid = this.inputKey.isValid () && this.input.isOpen ();
				if (inputValid && this.inputKey.isReadable ()) {
					this.transcript.traceDebugging ("accessing input flow...");
					try {
						try {
							final int outcome = this.input.read (this.inputPending);
							if (outcome == -1)
								this.input.close ();
						} catch (final ClosedChannelException exception) {
							this.exceptions.traceIgnoredException (exception);
						}
					} catch (final IOException exception) {
						throw (new IgnoredException (exception, "i/o error encountered while accessing input flow; aborting!"));
					}
				}
			} else
				inputValid = true;
			if (this.outputKey != null) {
				outputValid = this.outputKey.isValid () && this.output.isOpen ();
				if (outputValid && this.outputKey.isWritable ()) {
					this.transcript.traceDebugging ("accessing output flow...");
					try {
						try {
							this.output.write (this.outputPending);
						} catch (final ClosedChannelException exception) {
							this.exceptions.traceIgnoredException (exception);
						}
					} catch (final IOException exception) {
						throw (new IgnoredException (exception, "i/o error encountered while accessing output flow; aborting!"));
					}
				}
			} else
				outputValid = true;
			if (this.inputPending != null) {
				while (true) {
					if (this.inputPendingSize == -1) {
						if (this.inputPending.position () >= 4) {
							this.inputPendingSize = this.inputPending.getInt (0) + 4;
							if (this.inputPending.capacity () < this.inputPendingSize) {
								if (this.inputPendingSize > this.maximumPacketSize)
									throw (new IgnoredException (new BufferOverflowException (), "unexpected inbound packet size; aborting!"));
								final ByteBuffer buffer = ByteBuffer.allocate (this.inputPendingSize);
								this.inputPending.compact ();
								buffer.put (this.inputPending);
								this.inputPending = buffer;
							}
						}
					}
					if (this.inputPendingSize != -1) {
						final ByteBuffer packet;
						if (this.inputPending.position () == this.inputPendingSize) {
							packet = this.inputPending;
							this.inputPending = null;
							this.inputPendingSize = -1;
						} else if (this.inputPending.position () > this.inputPendingSize) {
							this.inputPending.flip ();
							packet = ByteBuffer.allocate (this.inputPendingSize);
							final ByteBuffer inputPendingSlice = this.inputPending.asReadOnlyBuffer ();
							inputPendingSlice.limit (this.inputPendingSize);
							packet.put (inputPendingSlice);
							this.inputPending.position (this.inputPendingSize);
							this.inputPending.compact ();
							this.inputPendingSize = -1;
						} else
							packet = null;
						if (packet != null) {
							packet.flip ();
							if (!Threading.offer (this.inboundPackets, packet.asReadOnlyBuffer (), this.pollTimeout))
								throw (new IgnoredException (new BufferOverflowException (), "unexpected queue overflow error encountered while enqueueing inbound packet; aborting!"));
						}
					}
					if ((this.inputPending == null) || (this.inputPending.position () < 4) || ((this.inputPendingSize != -1) && (this.inputPendingSize > this.inputPending.position ())))
						break;
				}
			}
			if (this.outputPending != null) {
				if (this.outputPending.remaining () == 0)
					this.outputPending = null;
			}
			if (!inputValid && !outputValid)
				this.triggerShutdown ();
			this.transcript.traceDebugging ("executed flows.");
		}
		
		@Override
		protected final void shutDown_1 () {
			this.transcript.traceDebugging ("destroying flows...");
			if (this.inputKey != null)
				try {
					this.inputKey.cancel ();
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception);
				} finally {
					this.inputKey = null;
				}
			if (this.outputKey != null)
				try {
					this.outputKey.cancel ();
					this.outputKey = null;
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception);
				} finally {
					this.outputKey = null;
				}
			try {
				this.selector.close ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
			this.transcript.traceDebugging ("destroyed flows.");
		}
		
		@Override
		protected final void startUp_1 ()
					throws CaughtException {
			this.transcript.traceDebugging ("initializing flows...");
			try {
				((SelectableChannel) this.input).configureBlocking (false);
				((SelectableChannel) this.output).configureBlocking (false);
			} catch (final IOException exception) {
				throw (new IgnoredException (exception, "i/o error encountered while configuring flows; aborting!"));
			}
			this.transcript.traceDebugging ("initialized flows.");
		}
		
		final LinkedBlockingQueue<ByteBuffer> inboundPackets;
		final ReadableByteChannel input;
		final int inputBufferSize;
		SelectionKey inputKey;
		ByteBuffer inputPending;
		int inputPendingSize;
		final int maximumPacketSize;
		final LinkedBlockingQueue<ByteBuffer> outboundPackets;
		final WritableByteChannel output;
		SelectionKey outputKey;
		ByteBuffer outputPending;
		final Selector selector;
	}
	
	private static abstract class Worker
				extends AbstractExecutionThreadService
	{
		protected Worker (final Backend channel) {
			super ();
			this.channel = channel;
			this.transcript = this.channel.transcript;
			this.exceptions = this.channel.exceptions;
			this.executor = this.channel.executor;
			this.pollTimeout = BasicChannel.defaultPollTimeout;
		}
		
		@Override
		protected final Executor executor () {
			return (this.executor);
		}
		
		protected abstract void loop_1 ()
					throws CaughtException;
		
		@Override
		protected final void run () {
			try {
				while (true) {
					if (!this.isRunning ())
						break;
					this.loop_1 ();
				}
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		
		@Override
		protected final void shutDown () {
			try {
				this.shutDown_1 ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		
		protected abstract void shutDown_1 ()
					throws CaughtException;
		
		@Override
		protected final void startUp () {
			try {
				this.startUp_1 ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		
		protected abstract void startUp_1 ()
					throws CaughtException;
		
		final Backend channel;
		final TranscriptExceptionTracer exceptions;
		final ExecutorService executor;
		final long pollTimeout;
		final Transcript transcript;
	}
}
