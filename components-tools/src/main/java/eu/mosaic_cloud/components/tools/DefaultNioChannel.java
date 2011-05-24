
package eu.mosaic_cloud.components.tools;


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
import eu.mosaic_cloud.components.core.Channel;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageCoder;
import eu.mosaic_cloud.tools.CaughtException;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.tools.UnexpectedException;
import eu.mosaic_cloud.tools.UnhandledException;
import eu.mosaic_cloud.transcript.core.Transcript;


public final class DefaultNioChannel
		extends AbstractService
		implements
			Channel
{
	public DefaultNioChannel (final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final ChannelCallbacks callbacks, final ExecutorService executor)
	{
		super ();
		Preconditions.checkNotNull (input);
		Preconditions.checkArgument (input instanceof SelectableChannel);
		Preconditions.checkNotNull (output);
		Preconditions.checkArgument (output instanceof SelectableChannel);
		Preconditions.checkNotNull (coder);
		Preconditions.checkNotNull (callbacks);
		Preconditions.checkNotNull (executor);
		this.transcript = Transcript.create (this);
		final Selector selector;
		try {
			selector = Selector.open ();
		} catch (final IOException exception) {
			this.transcript.traceRethrownException (exception, "encountered i/o error while creating selector; aborting!");
			throw (new IllegalStateException (exception));
		}
		this.monitor = Monitor.create (this);
		synchronized (this.monitor) {
			this.input = input;
			this.output = output;
			this.selector = selector;
			this.coder = coder;
			this.callbacks = callbacks;
			this.executor = executor;
			this.inboundPackets = new LinkedBlockingQueue<ByteBuffer> ();
			this.outboundPackets = new LinkedBlockingQueue<ByteBuffer> ();
			this.inboundMessages = new LinkedBlockingQueue<ChannelMessage> ();
			this.outboundMessages = new LinkedBlockingQueue<ChannelMessage> ();
			this.channelsThread = new ChannelsThread ();
			this.encoderThread = new EncoderThread ();
			this.decoderThread = new DecoderThread ();
			this.dispatcherThread = new DispatcherThread ();
		}
	}
	
	public DefaultNioChannel (final ReadableByteChannel input, final WritableByteChannel output, final ChannelMessageCoder coder, final ExecutorService executor)
	{
		this (input, output, coder, ThrowingChannelCallbacks.defaultInstance, executor);
	}
	
	@Override
	public final void close ()
	{
		this.stop ();
	}
	
	@Override
	public final ChannelCallbacks getCallbacks ()
	{
		synchronized (this.monitor) {
			return (this.callbacks);
		}
	}
	
	public final void open ()
	{
		this.start ();
	}
	
	@Override
	public final void send (final ChannelMessage message)
	{
		Preconditions.checkNotNull (message);
		synchronized (this.monitor) {
			this.transcript.traceDebugging ("sending message...");
			this.outboundMessages.add (message);
		}
	}
	
	@Override
	public final void setCallbacks (final ChannelCallbacks callbacks)
	{
		Preconditions.checkNotNull (callbacks);
		synchronized (this.monitor) {
			this.callbacks = callbacks;
		}
	}
	
	@Override
	protected void doStart ()
	{
		synchronized (this.monitor) {
			this.transcript.traceDebugging ("opening channel...");
			this.channelsThread.start ();
			this.encoderThread.start ();
			this.decoderThread.start ();
			this.dispatcherThread.start ();
		}
	}
	
	@Override
	protected void doStop ()
	{
		synchronized (this.monitor) {
			this.transcript.traceDebugging ("closing channel...");
			this.selector.wakeup ();
			this.channelsThread.stop ();
			this.encoderThread.stop ();
			this.decoderThread.stop ();
			this.dispatcherThread.stop ();
		}
	}
	
	final ChannelMessageCoder coder;
	final ExecutorService executor;
	final LinkedBlockingQueue<ChannelMessage> inboundMessages;
	final LinkedBlockingQueue<ByteBuffer> inboundPackets;
	final ReadableByteChannel input;
	final LinkedBlockingQueue<ChannelMessage> outboundMessages;
	final LinkedBlockingQueue<ByteBuffer> outboundPackets;
	final WritableByteChannel output;
	final Selector selector;
	final Transcript transcript;
	private ChannelCallbacks callbacks;
	private final ChannelsThread channelsThread;
	private final DecoderThread decoderThread;
	private final DispatcherThread dispatcherThread;
	private final EncoderThread encoderThread;
	private final Monitor monitor;
	
	private abstract class BaseThread
			extends AbstractExecutionThreadService
	{
		protected BaseThread ()
		{
			this.transcript = DefaultNioChannel.this.transcript;
			this.executor = DefaultNioChannel.this.executor;
			this.pollTimeout = 100;
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
			while (true) {
				if (!this.isRunning ())
					break;
				try {
					try {
						this.loop_1 ();
					} catch (final CaughtException exception) {
						throw (exception);
					} catch (final Throwable exception) {
						throw (new UnexpectedException (exception, "unexpected error encountered; aborting!"));
					}
				} catch (final CaughtException exception) {
					this.transcript.traceException (exception.getResolution (), exception.getCause (), exception.getMessageFormat (), exception.getMessageArguments ());
					this.failure = exception;
					this.triggerShutdown ();
					break;
				}
			}
		}
		
		@Override
		protected final void shutDown ()
		{
			try {
				try {
					this.shutDown_1 ();
				} catch (final CaughtException exception) {
					throw (exception);
				} catch (final Throwable exception) {
					throw (new UnexpectedException (exception, "unexpected error encountered; aborting!"));
				}
			} catch (final CaughtException exception) {
				this.transcript.traceException (exception.getResolution (), exception.getCause (), exception.getMessageFormat (), exception.getMessageArguments ());
				if (this.failure != null)
					this.failure = exception;
			}
			if (this.failure != null)
				DefaultNioChannel.this.stop ();
		}
		
		protected abstract void shutDown_1 ()
				throws CaughtException;
		
		@Override
		protected final void startUp ()
		{
			try {
				try {
					this.startUp_1 ();
				} catch (final CaughtException exception) {
					throw (exception);
				} catch (final Throwable exception) {
					throw (new UnexpectedException (exception, "unexpected error encountered; aborting!"));
				}
			} catch (final CaughtException exception) {
				this.transcript.traceException (exception.getResolution (), exception.getCause (), exception.getMessageFormat (), exception.getMessageArguments ());
				this.failure = exception;
				this.triggerShutdown ();
			}
		}
		
		protected abstract void startUp_1 ()
				throws CaughtException;
		
		protected final ExecutorService executor;
		protected final long pollTimeout;
		protected final Transcript transcript;
		private CaughtException failure;
	}
	
	private final class ChannelsThread
			extends BaseThread
	{
		ChannelsThread ()
		{
			super ();
			this.input = DefaultNioChannel.this.input;
			this.output = DefaultNioChannel.this.output;
			this.selector = DefaultNioChannel.this.selector;
			this.inboundPackets = DefaultNioChannel.this.inboundPackets;
			this.outboundPackets = DefaultNioChannel.this.outboundPackets;
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
			if (this.inputPending == null) {
				this.inputPending = ByteBuffer.allocate (this.inputBufferSize);
				this.inputPendingSize = -1;
			}
			if (this.outputPending == null) {
				final ByteBuffer packet = this.outboundPackets.poll ();
				if (packet != null) {
					try {
						Preconditions.checkArgument (packet.order () == ByteOrder.BIG_ENDIAN, "invalid packet byte-order");
						Preconditions.checkArgument (packet.remaining () >= 4, "invalid packet framing");
						packet.mark ();
						final int packetSize = packet.getInt ();
						Preconditions.checkArgument (packetSize == packet.remaining (), "invalid outbound packet encoding");
						packet.reset ();
					} catch (final IllegalArgumentException exception) {
						throw (new UnexpectedException (exception, "unexpected validation error encountered while polling outbound packet; aborting!"));
					}
					this.outputPending = packet;
				}
			}
			try {
				if ((this.inputPending != null) && (this.inputKey == null))
					this.inputKey = ((SelectableChannel) this.input).register (this.selector, SelectionKey.OP_READ);
				else if ((this.inputKey != null) && (this.inputPending == null)) {
					this.inputKey.cancel ();
					this.inputKey = null;
				}
				if ((this.outputPending != null) && (this.outputKey == null))
					this.outputKey = ((SelectableChannel) this.output).register (this.selector, SelectionKey.OP_WRITE);
				else if ((this.outputKey != null) && (this.outputPending == null)) {
					this.outputKey.cancel ();
					this.outputKey = null;
				}
			} catch (final IOException exception) {
				throw (new UnexpectedException (exception, "unexpected i/o error encountered while polling streams; aborting!"));
			}
			try {
				this.selector.select (this.pollTimeout);
			} catch (final IOException exception) {
				throw (new UnexpectedException (exception, "unexpected i/o error encountered while polling streams; aborting!"));
			}
			final boolean inputValid;
			final boolean outputValid;
			if (this.inputKey != null) {
				inputValid = this.inputKey.isValid ();
				if (inputValid && this.inputKey.isReadable ()) {
					this.transcript.traceDebugging ("accessing input stream...");
					try {
						this.input.read (this.inputPending);
					} catch (final IOException exception) {
						throw (new UnhandledException (exception, "i/o error encountered while accessing input stream; aborting!"));
					}
				}
			} else
				inputValid = true;
			if (this.outputKey != null) {
				outputValid = this.outputKey.isValid ();
				if (outputValid && this.outputKey.isWritable ()) {
					this.transcript.traceDebugging ("accessing output stream...");
					try {
						this.output.write (this.outputPending);
					} catch (final IOException exception) {
						throw (new UnhandledException (exception, "i/o error encountered while accessing output stream; aborting!"));
					}
				}
			} else
				outputValid = true;
			if (this.inputPending != null) {
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
					} else if (this.inputPending.position () >= this.inputPendingSize) {
						this.inputPending.flip ();
						packet = ByteBuffer.allocate (this.inputPendingSize);
						packet.put (this.inputPending);
						this.inputPending.compact ();
					} else
						packet = null;
					if (packet != null) {
						packet.flip ();
						try {
							this.inboundPackets.add (packet);
						} catch (final IllegalStateException exception) {
							throw (new UnexpectedException (exception, "unexpected queue overflow error encountered while enqueueing inbound packet; aborting!"));
						}
					}
				}
			}
			if (this.outputPending != null) {
				if (this.outputPending.remaining () == 0)
					this.outputPending = null;
			}
			if (!inputValid && !outputValid)
				throw (new AssertionError ());
		}
		
		@Override
		protected final void shutDown_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("finalizing channels worker...");
			try {
				this.selector.close ();
			} catch (final IOException exception) {
				throw (new UnhandledException (exception, "i/o error encountered while closing the selector; ignoring!"));
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
				throw (new UnhandledException (exception, "i/o error encountered while configuring streams; aborting!"));
			}
		}
		
		private final LinkedBlockingQueue<ByteBuffer> inboundPackets;
		private final ReadableByteChannel input;
		private final int inputBufferSize;
		private SelectionKey inputKey;
		private ByteBuffer inputPending;
		private int inputPendingSize;
		private final LinkedBlockingQueue<ByteBuffer> outboundPackets;
		private final WritableByteChannel output;
		private SelectionKey outputKey;
		private ByteBuffer outputPending;
		private final Selector selector;
	}
	
	private final class DecoderThread
			extends BaseThread
	{
		DecoderThread ()
		{
			super ();
			this.coder = DefaultNioChannel.this.coder;
			this.inboundMessages = DefaultNioChannel.this.inboundMessages;
			this.inboundPackets = DefaultNioChannel.this.inboundPackets;
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
				throw (new UnexpectedException (exception, "unexpected error encountered while dequeueing inbound packet; aborting!"));
			}
			if (packet != null) {
				this.transcript.traceDebugging ("decoding inbound message...");
				final ChannelMessage message;
				try {
					message = this.coder.decode (packet);
					Preconditions.checkNotNull (message);
				} catch (final Throwable exception) {
					throw (new UnexpectedException (exception, "unexpected error encountered while decoding the inbound packet; aborting!"));
				}
				try {
					this.inboundMessages.add (message);
				} catch (final IllegalStateException exception) {
					throw (new UnexpectedException (exception, "queue overflow error encountered while enqueueing inbound message; aborting!"));
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
		
		private final ChannelMessageCoder coder;
		private final LinkedBlockingQueue<ChannelMessage> inboundMessages;
		private final LinkedBlockingQueue<ByteBuffer> inboundPackets;
	}
	
	private final class DispatcherThread
			extends BaseThread
	{
		DispatcherThread ()
		{
			this.inboundMessages = DefaultNioChannel.this.inboundMessages;
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
				throw (new UnexpectedException (exception, "unexpected error encountered while dequeueing inbound message; aborting!"));
			}
			if (message != null) {
				this.transcript.traceDebugging ("dispatching receive callback...");
				try {
					final ChannelCallbacks callbacks = DefaultNioChannel.this.getCallbacks ();
					callbacks.received (DefaultNioChannel.this, message);
				} catch (final Throwable exception) {
					throw (new UnexpectedException (exception, "unexpected error encountered while dispatching receive callback; aborting!"));
				}
			}
		}
		
		@Override
		protected final void shutDown_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("finalizing dispatcher worker...");
			this.transcript.traceDebugging ("dispatching close callback...");
			try {
				final ChannelCallbacks callbacks = DefaultNioChannel.this.getCallbacks ();
				callbacks.closed (DefaultNioChannel.this);
			} catch (final Throwable exception) {
				throw (new UnexpectedException (exception, "unexpected error encountered while dispatching close callback; aborting!"));
			}
		}
		
		@Override
		protected final void startUp_1 ()
				throws CaughtException
		{
			this.transcript.traceDebugging ("initializing dispatcher worker...");
			this.transcript.traceDebugging ("dispatching open callback...");
			try {
				final ChannelCallbacks callbacks = DefaultNioChannel.this.getCallbacks ();
				callbacks.opened (DefaultNioChannel.this);
			} catch (final Throwable exception) {
				throw (new UnexpectedException (exception, "unexpected error encountered while dispatching open callback; aborting!"));
			}
		}
		
		private final LinkedBlockingQueue<ChannelMessage> inboundMessages;
	}
	
	private final class EncoderThread
			extends BaseThread
	{
		EncoderThread ()
		{
			super ();
			this.coder = DefaultNioChannel.this.coder;
			this.outboundMessages = DefaultNioChannel.this.outboundMessages;
			this.outboundPackets = DefaultNioChannel.this.outboundPackets;
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
				throw (new UnexpectedException (exception, "unexpected error encountered while polling outbound messages; aborting!"));
			}
			if (message != null) {
				this.transcript.traceDebugging ("encoding outbound message...");
				final ByteBuffer packet;
				try {
					packet = this.coder.encode (message);
					Preconditions.checkNotNull (packet);
				} catch (final Throwable exception) {
					throw (new UnexpectedException (exception, "unexpected error encountered while encoding the outbound message; aborting!"));
				}
				try {
					this.outboundPackets.add (packet);
				} catch (final IllegalStateException exception) {
					throw (new UnexpectedException (exception, "unexpected queue overflow error encountered while enqueueing outbound packet; aborting!"));
				}
				DefaultNioChannel.this.selector.wakeup ();
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
		
		private final ChannelMessageCoder coder;
		private final LinkedBlockingQueue<ChannelMessage> outboundMessages;
		private final LinkedBlockingQueue<ByteBuffer> outboundPackets;
	}
}
