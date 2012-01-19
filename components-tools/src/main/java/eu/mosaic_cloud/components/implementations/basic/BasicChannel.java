/*
 * #%L
 * mosaic-components-tools
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractService;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelFlow;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageCoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.core.IgnoredException;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ThreadConfiguration;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;


public final class BasicChannel
		extends Object
		implements
			eu.mosaic_cloud.components.core.Channel
{
	private BasicChannel (final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final CallbackReactor callbackReactor, final ChannelCallbacks callbacks, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		super ();
		this.delegate = new Channel (this, input, output, coder, callbackReactor, callbacks, threading, exceptions);
	}
	
	@Override
	public void assign (final ChannelCallbacks callbacks)
	{
		this.delegate.assign (callbacks);
	}
	
	@Override
	public final void close (final ChannelFlow flow)
	{
		this.delegate.close (flow);
	}
	
	public void initialize ()
	{
		this.delegate.startAndWait ();
	}
	
	public final boolean isActive ()
	{
		return (this.delegate.dispatcher.isRunning () || this.delegate.ioputer.isRunning ());
	}
	
	@Override
	public void send (final ChannelMessage message)
	{
		this.delegate.send (message);
	}
	
	@Override
	public void terminate ()
	{
		this.delegate.stop ();
	}
	
	final Channel delegate;
	
	public static final BasicChannel create (final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final CallbackReactor reactor, final ChannelCallbacks callbacks, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new BasicChannel (input, output, coder, reactor, callbacks, threading, exceptions));
	}
	
	public static final BasicChannel create (final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new BasicChannel (input, output, coder, reactor, null, threading, exceptions));
	}
	
	private static final long defaultPollTimeout = 1000;
	
	private static final class Channel
			extends AbstractService
	{
		Channel (final BasicChannel facade, final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final CallbackReactor callbackReactor, final ChannelCallbacks callbacks, final ThreadingContext threading, final ExceptionTracer exceptions)
		{
			super ();
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (input);
			Preconditions.checkArgument (input instanceof SelectableChannel);
			Preconditions.checkNotNull (output);
			Preconditions.checkArgument (output instanceof SelectableChannel);
			Preconditions.checkNotNull (coder);
			Preconditions.checkNotNull (callbackReactor);
			Preconditions.checkNotNull (threading);
			this.facade = facade;
			this.monitor = Monitor.create (this.facade);
			synchronized (this.monitor) {
				this.threading = threading;
				this.transcript = Transcript.create (this.facade);
				this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
				this.input = input;
				this.output = output;
				final Selector selector;
				try {
					selector = Selector.open ();
				} catch (final Throwable exception) {
					this.exceptions.traceDeferredException (exception);
					throw (new Error (exception));
				}
				this.selector = selector;
				this.coder = coder;
				this.callbackReactor = callbackReactor;
				this.callbackTrigger = this.callbackReactor.register (ChannelCallbacks.class, callbacks);
				this.executor = this.threading.newCachedThreadPool (new ThreadConfiguration (this.facade, "services", true, this.exceptions.catcher));
				this.inboundPackets = new LinkedBlockingQueue<ByteBuffer> ();
				this.outboundPackets = new LinkedBlockingQueue<ByteBuffer> ();
				this.inboundMessages = new LinkedBlockingQueue<ChannelMessage> ();
				this.outboundMessages = new LinkedBlockingQueue<ChannelMessage> ();
				this.ioputer = new Ioputer (this);
				this.encoder = new Encoder (this);
				this.decoder = new Decoder (this);
				this.dispatcher = new Dispatcher (this);
			}
		}
		
		@Override
		protected void doStart ()
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("opening channel...");
				this.ioputer.start ();
				this.encoder.start ();
				this.decoder.start ();
				this.dispatcher.start ();
				this.notifyStarted ();
			}
		}
		
		@Override
		protected void doStop ()
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("closing channel...");
				this.ioputer.stop ();
				this.encoder.stop ();
				this.decoder.stop ();
				this.dispatcher.stop ();
				this.notifyStopped ();
				try {
					this.input.close ();
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception);
				}
				try {
					this.output.close ();
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception);
				}
				try {
					this.selector.wakeup ();
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception);
				}
			}
		}
		
		final void assign (final ChannelCallbacks callbacks)
		{
			this.callbackReactor.assign (this.callbackTrigger, callbacks);
		}
		
		final void close (final ChannelFlow flow)
		{
			Preconditions.checkNotNull (flow);
			synchronized (this.monitor) {
				switch (flow) {
					case Inbound :
						try {
							this.input.close ();
						} catch (final Throwable exception) {
							this.exceptions.traceIgnoredException (exception);
						}
						break;
					case Outbound :
						try {
							this.output.close ();
						} catch (final Throwable exception) {
							this.exceptions.traceIgnoredException (exception);
						}
						break;
					default:
						throw (new AssertionError ());
				}
			}
		}
		
		final void send (final ChannelMessage message)
		{
			Preconditions.checkNotNull (message);
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("sending message...");
				this.outboundMessages.add (message);
			}
		}
		
		final CallbackReactor callbackReactor;
		final ChannelCallbacks callbackTrigger;
		final ChannelMessageCoder coder;
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
		final Selector selector;
		final ThreadingContext threading;
		final Transcript transcript;
	}
	
	private static final class Decoder
			extends Worker
	{
		Decoder (final Channel channel)
		{
			super (channel);
			this.coder = this.channel.coder;
			this.inboundMessages = this.channel.inboundMessages;
			this.inboundPackets = this.channel.inboundPackets;
		}
		
		@Override
		protected void loop_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("executing decoder worker...");
			final ByteBuffer packet;
			try {
				packet = this.inboundPackets.poll (this.pollTimeout, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException exception) {
				throw (new IgnoredException (exception, "unexpected error encountered while dequeueing inbound packet; aborting!"));
			}
			if (packet != null) {
				this.transcript.traceDebugging ("decoding inbound message...");
				final ChannelMessage message;
				try {
					message = this.coder.decode (packet);
					Preconditions.checkNotNull (message);
				} catch (final Throwable exception) {
					throw (new IgnoredException (exception, "unexpected error encountered while decoding the inbound packet; aborting!"));
				}
				try {
					this.inboundMessages.add (message);
				} catch (final IllegalStateException exception) {
					throw (new IgnoredException (exception, "queue overflow error encountered while enqueueing inbound message; aborting!"));
				}
			}
		}
		
		@Override
		protected final void shutDown_1 ()
		{
			this.transcript.traceDebugging ("finalizing decoder worker...");
		}
		
		@Override
		protected final void startUp_1 ()
		{
			this.transcript.traceDebugging ("initializing decoder worker...");
		}
		
		final ChannelMessageCoder coder;
		final LinkedBlockingQueue<ChannelMessage> inboundMessages;
		final LinkedBlockingQueue<ByteBuffer> inboundPackets;
	}
	
	private static final class Dispatcher
			extends Worker
	{
		Dispatcher (final Channel channel)
		{
			super (channel);
			this.callbackTrigger = this.channel.callbackTrigger;
			this.inboundMessages = this.channel.inboundMessages;
			this.inboundActive = true;
			this.outboundActive = true;
		}
		
		@Override
		protected final void loop_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("executing dispatcher worker...");
			final ChannelMessage message;
			try {
				message = this.inboundMessages.poll (this.pollTimeout, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException exception) {
				throw (new IgnoredException (exception, "unexpected error encountered while dequeueing inbound message; aborting!"));
			}
			if (message != null) {
				if (this.inboundActive) {
					this.transcript.traceDebugging ("dispatching receive callback...");
					try {
						this.callbackTrigger.received (this.channel.facade, message);
					} catch (final Throwable exception) {
						throw (new IgnoredException (exception, "unexpected error encountered while dispatching receive callback; aborting!"));
					}
				} else
					this.transcript.traceError ("discarding receive callback due to closed inbound flow;");
			} else {
				if (this.inboundActive && !this.channel.input.isOpen ()) {
					this.transcript.traceDebugging ("dispatching close inbound callback...");
					this.inboundActive = false;
					try {
						this.callbackTrigger.closed (this.channel.facade, ChannelFlow.Inbound);
					} catch (final Throwable exception) {
						throw (new IgnoredException (exception, "unexpected error encountered while dispatching close inbound callback; aborting!"));
					}
				}
			}
			if (this.outboundActive && !this.channel.output.isOpen ()) {
				this.transcript.traceDebugging ("dispatching close outbound callback...");
				this.outboundActive = false;
				try {
					this.callbackTrigger.closed (this.channel.facade, ChannelFlow.Outbound);
				} catch (final Throwable exception) {
					throw (new IgnoredException (exception, "unexpected error encountered while dispatching close outbound callback; aborting!"));
				}
			}
			if (!this.outboundActive && !this.inboundActive)
				this.triggerShutdown ();
		}
		
		@Override
		protected final void shutDown_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("finalizing dispatcher worker...");
			this.transcript.traceDebugging ("dispatching terminate callback...");
			try {
				this.callbackTrigger.terminated (this.channel.facade);
			} catch (final Throwable exception) {
				throw (new IgnoredException (exception, "unexpected error encountered while dispatching close callback; aborting!"));
			}
		}
		
		@Override
		protected final void startUp_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("initializing dispatcher worker...");
			this.transcript.traceDebugging ("dispatching initialize callback...");
			try {
				this.callbackTrigger.initialized (this.channel.facade);
			} catch (final Throwable exception) {
				throw (new IgnoredException (exception, "unexpected error encountered while dispatching open callback; aborting!"));
			}
		}
		
		final ChannelCallbacks callbackTrigger;
		boolean inboundActive;
		final LinkedBlockingQueue<ChannelMessage> inboundMessages;
		boolean outboundActive;
	}
	
	private static final class Encoder
			extends Worker
	{
		Encoder (final Channel channel)
		{
			super (channel);
			this.coder = this.channel.coder;
			this.outboundMessages = this.channel.outboundMessages;
			this.outboundPackets = this.channel.outboundPackets;
		}
		
		@Override
		protected void loop_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("executing encoder worker...");
			final ChannelMessage message;
			try {
				message = this.outboundMessages.poll (this.pollTimeout, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException exception) {
				throw (new IgnoredException (exception, "unexpected error encountered while polling outbound messages; aborting!"));
			}
			if (message != null) {
				this.transcript.traceDebugging ("encoding outbound message...");
				final ByteBuffer packet;
				try {
					packet = this.coder.encode (message);
					Preconditions.checkNotNull (packet);
				} catch (final Throwable exception) {
					throw (new IgnoredException (exception, "unexpected error encountered while encoding the outbound message; aborting!"));
				}
				try {
					this.outboundPackets.add (packet);
				} catch (final IllegalStateException exception) {
					throw (new IgnoredException (exception, "unexpected queue overflow error encountered while enqueueing outbound packet; aborting!"));
				}
				this.channel.selector.wakeup ();
			}
		}
		
		@Override
		protected final void shutDown_1 ()
		{
			this.transcript.traceDebugging ("finalizing encoder worker...");
		}
		
		@Override
		protected final void startUp_1 ()
		{
			this.transcript.traceDebugging ("initializing encoder worker...");
		}
		
		final ChannelMessageCoder coder;
		final LinkedBlockingQueue<ChannelMessage> outboundMessages;
		final LinkedBlockingQueue<ByteBuffer> outboundPackets;
	}
	
	private static final class Ioputer
			extends Worker
	{
		Ioputer (final Channel channel)
		{
			super (channel);
			this.input = this.channel.input;
			this.output = this.channel.output;
			this.selector = this.channel.selector;
			this.inboundPackets = this.channel.inboundPackets;
			this.outboundPackets = this.channel.outboundPackets;
			this.inputBufferSize = 1024;
			this.inputKey = null;
			this.outputKey = null;
			this.inputPending = null;
			this.inputPendingSize = -1;
			this.outputPending = null;
		}
		
		@Override
		protected void loop_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("executing channels worker...");
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
				throw (new IgnoredException (exception, "i/o error encountered while polling streams; aborting!"));
			}
			try {
				this.selector.select (this.pollTimeout);
			} catch (final IOException exception) {
				throw (new IgnoredException (exception, "i/o error encountered while polling streams; aborting!"));
			}
			final boolean inputValid;
			final boolean outputValid;
			if (this.inputKey != null) {
				inputValid = this.inputKey.isValid () && this.input.isOpen ();
				if (inputValid && this.inputKey.isReadable ()) {
					this.transcript.traceDebugging ("accessing input stream...");
					try {
						final int outcome = this.input.read (this.inputPending);
						if (outcome == -1)
							this.input.close ();
					} catch (final IOException exception) {
						throw (new IgnoredException (exception, "i/o error encountered while accessing input stream; aborting!"));
					}
				}
			} else
				inputValid = true;
			if (this.outputKey != null) {
				outputValid = this.outputKey.isValid () && this.output.isOpen ();
				if (outputValid && this.outputKey.isWritable ()) {
					this.transcript.traceDebugging ("accessing output stream...");
					try {
						this.output.write (this.outputPending);
					} catch (final IOException exception) {
						throw (new IgnoredException (exception, "i/o error encountered while accessing output stream; aborting!"));
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
							try {
								this.inboundPackets.add (packet.asReadOnlyBuffer ());
							} catch (final IllegalStateException exception) {
								throw (new IgnoredException (exception, "unexpected queue overflow error encountered while enqueueing inbound packet; aborting!"));
							}
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
		}
		
		@Override
		protected final void shutDown_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("finalizing channels worker...");
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
		}
		
		@Override
		protected final void startUp_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("initializing channels worker...");
			try {
				((SelectableChannel) this.input).configureBlocking (false);
				((SelectableChannel) this.output).configureBlocking (false);
			} catch (final IOException exception) {
				throw (new IgnoredException (exception, "i/o error encountered while configuring streams; aborting!"));
			}
		}
		
		final LinkedBlockingQueue<ByteBuffer> inboundPackets;
		final ReadableByteChannel input;
		final int inputBufferSize;
		SelectionKey inputKey;
		ByteBuffer inputPending;
		int inputPendingSize;
		final LinkedBlockingQueue<ByteBuffer> outboundPackets;
		final WritableByteChannel output;
		SelectionKey outputKey;
		ByteBuffer outputPending;
		final Selector selector;
	}
	
	private static abstract class Worker
			extends AbstractExecutionThreadService
	{
		protected Worker (final Channel channel)
		{
			super ();
			this.channel = channel;
			this.transcript = this.channel.transcript;
			this.exceptions = this.channel.exceptions;
			this.executor = this.channel.executor;
			this.pollTimeout = BasicChannel.defaultPollTimeout;
		}
		
		@Override
		protected final Executor executor ()
		{
			return (this.executor);
		}
		
		protected abstract void loop_1 ()
				throws CaughtException;
		
		@Override
		protected final void run ()
		{
			try {
				while (true) {
					if (!this.channel.isRunning ())
						this.triggerShutdown ();
					if (!this.isRunning ())
						break;
					this.loop_1 ();
				}
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		
		@Override
		protected final void shutDown ()
		{
			try {
				this.shutDown_1 ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		
		protected abstract void shutDown_1 ()
				throws CaughtException;
		
		@Override
		protected final void startUp ()
		{
			try {
				this.startUp_1 ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		
		protected abstract void startUp_1 ()
				throws CaughtException;
		
		final Channel channel;
		final TranscriptExceptionTracer exceptions;
		final ExecutorService executor;
		final long pollTimeout;
		final Transcript transcript;
	}
}
