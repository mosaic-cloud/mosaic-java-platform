
package eu.mosaic_cloud.components.implementations.basic;


import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.tools.DefaultJsonMessageCoder;
import eu.mosaic_cloud.transcript.core.Transcript;


public final class BasicComponentContainerMain
		extends Object
{
	private BasicComponentContainerMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final ComponentCallbacks callbacks)
	{
		final long pollTimeout = 100;
		final ReadableByteChannel input;
		final WritableByteChannel output;
		final Piper inputPiper;
		final Piper outputPiper;
		try {
			final Pipe inputPipe = Pipe.open ();
			final Pipe outputPipe = Pipe.open ();
			final ReadableByteChannel stdin = Channels.newChannel (BasicComponentContainerPreMain.stdin);
			final WritableByteChannel stdout = Channels.newChannel (BasicComponentContainerPreMain.stdout);
			inputPiper = new Piper (stdin, inputPipe.sink ());
			outputPiper = new Piper (outputPipe.source (), stdout);
			input = inputPipe.source ();
			output = outputPipe.sink ();
		} catch (final Exception exception) {
			throw (new IllegalStateException (exception));
		}
		inputPiper.start ();
		outputPiper.start ();
		final DefaultJsonMessageCoder coder = DefaultJsonMessageCoder.create ();
		final BasicCallbackReactor reactor = BasicCallbackReactor.create ();
		reactor.initialize ();
		final BasicChannel channel = BasicChannel.create (input, output, coder, reactor, null);
		final BasicComponent component = BasicComponent.create (channel, reactor, null);
		channel.initialize ();
		component.initialize ();
		component.assign (callbacks);
		while (true) {
			if (!component.isActive ())
				break;
			try {
				Thread.sleep (pollTimeout);
			} catch (final InterruptedException exception) {
				throw (new IllegalStateException (exception));
			}
		}
		component.terminate ();
		channel.terminate ();
		reactor.terminate ();
		try {
			inputPiper.join ();
			outputPiper.join ();
		} catch (final InterruptedException exception) {
			throw (new IllegalStateException (exception));
		}
	}
	
	public static final void main (final String componentArgument, final String classpathArgument)
	{
		Preconditions.checkNotNull (componentArgument);
		final ClassLoader classLoader;
		if (classpathArgument != null) {
			final LinkedList<URL> classLoaderUrls = new LinkedList<URL> ();
			for (final String classpathPart : classpathArgument.split ("\\|"))
				if (classpathPart.length () > 0) {
					final URL classpathUrl;
					if (classpathPart.startsWith ("http:") || classpathPart.startsWith ("file:"))
						try {
							classpathUrl = new URL (classpathPart);
						} catch (final Exception exception) {
							throw (new IllegalArgumentException (String.format ("invalid class-path URL `%s`", classpathPart), exception));
						}
					else
						throw (new IllegalArgumentException (String.format ("invalid class-path URL `%s`", classpathPart)));
					classLoaderUrls.add (classpathUrl);
				}
			classLoader = new URLClassLoader (classLoaderUrls.toArray (new URL[0]), BasicComponentContainerMain.class.getClassLoader ());
		} else
			classLoader = ClassLoader.getSystemClassLoader ();
		final Class<?> componentClass;
		try {
			componentClass = classLoader.loadClass (componentArgument);
		} catch (final Exception exception) {
			throw (new IllegalArgumentException (String.format ("invalid component class `%s` (error encountered while resolving)", componentArgument), exception));
		}
		Preconditions.checkArgument (ComponentCallbacks.class.isAssignableFrom (componentClass), "invalid component class `%s` (not an instance of `ComponentCallbacks`)", componentClass.getName ());
		final ComponentCallbacks callbacks;
		try {
			callbacks = (ComponentCallbacks) componentClass.newInstance ();
		} catch (final Exception exception) {
			throw (new IllegalArgumentException (String.format ("invalid component class `%s` (error encountered while instantiating)", componentClass.getName ()), exception));
		}
		BasicComponentContainerMain.main (callbacks);
	}
	
	public static final void main (final String[] arguments)
	{
		Preconditions.checkNotNull (arguments);
		Preconditions.checkArgument ((arguments.length >= 1) && (arguments.length <= 2), "invalid arguments; aborting! (expected `<component-callbacks-class-name> <class-path-urls>`");
		final String componentArgument;
		final String classpathArgument;
		if (arguments.length == 1) {
			componentArgument = arguments[0];
			classpathArgument = null;
		} else if (arguments.length == 2) {
			componentArgument = arguments[0];
			classpathArgument = arguments[1];
		} else
			throw (new AssertionError ());
		BasicComponentContainerMain.main (componentArgument, classpathArgument);
	}
	
	private static final class Piper
			extends Thread
			implements
				UncaughtExceptionHandler
	{
		Piper (final ReadableByteChannel source, final WritableByteChannel sink)
		{
			super ();
			this.transcript = Transcript.create (this);
			this.source = source;
			this.sink = sink;
			this.setDaemon (true);
			this.setName (String.format ("Piper#%08x", Integer.valueOf (System.identityHashCode (this))));
			this.setUncaughtExceptionHandler (this);
		}
		
		@Override
		public final void run ()
		{
			try {
				final ByteBuffer buffer = ByteBuffer.allocateDirect (1024 * 1024);
				while (true) {
					try {
						final int outcome = this.source.read (buffer);
						if (outcome == -1)
							break;
					} catch (final IOException exception) {
						this.transcript.traceDeferredException (exception, "error encountered while reading from the source channel; aborting!");
						throw (new Error (exception));
					}
					buffer.flip ();
					try {
						this.sink.write (buffer);
					} catch (final IOException exception) {
						this.transcript.traceDeferredException (exception, "error encountered while writing to the sink channel; aborting!");
						throw (new Error (exception));
					}
					buffer.compact ();
				}
			} finally {
				this.close ();
			}
		}
		
		@Override
		public void uncaughtException (final Thread thread, final Throwable exception)
		{
			Preconditions.checkArgument (this == thread);
			this.close ();
			this.transcript.traceIgnoredException (exception);
		}
		
		private final void close ()
		{
			try {
				this.source.close ();
			} catch (final Throwable exception) {
				this.transcript.traceIgnoredException (exception);
			}
			try {
				this.sink.close ();
			} catch (final Throwable exception) {
				this.transcript.traceIgnoredException (exception);
			}
		}
		
		private final WritableByteChannel sink;
		private final ReadableByteChannel source;
		private final Transcript transcript;
	}
}
