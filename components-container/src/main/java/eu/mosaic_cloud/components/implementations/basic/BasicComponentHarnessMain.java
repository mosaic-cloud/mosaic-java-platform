/*
 * #%L
 * mosaic-components-container
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.net.SocketAppender;
import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;
import org.slf4j.LoggerFactory;


public final class BasicComponentHarnessMain
		extends Object
{
	private BasicComponentHarnessMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final ComponentCallbacks callbacks, final InputStream input, final OutputStream output, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		Preconditions.checkNotNull (callbacks);
		Preconditions.checkNotNull (input);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final Pipe inputPipe;
		final Pipe outputPipe;
		try {
			inputPipe = Pipe.open ();
			outputPipe = Pipe.open ();
		} catch (final IOException exception) {
			exceptions.trace (ExceptionResolution.Deferred, exception);
			throw (new Error (exception));
		}
		final Piper inputPiper = new Piper (Channels.newChannel (input), inputPipe.sink (), threading, exceptions);
		final Piper outputPiper = new Piper (outputPipe.source (), Channels.newChannel (output), threading, exceptions);
		try {
			BasicComponentHarnessMain.main (callbacks, inputPipe.source (), outputPipe.sink (), threading, exceptions);
		} catch (final Error exception) {
			exceptions.trace (ExceptionResolution.Deferred, exception);
			throw (exception);
		} finally {
			inputPiper.join ();
			outputPiper.join ();
		}
	}
	
	public static final void main (final ComponentCallbacks callbacks, final ReadableByteChannel input, final WritableByteChannel output, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		Preconditions.checkNotNull (callbacks);
		Preconditions.checkNotNull (input);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.create ();
		final BasicCallbackReactor reactor = BasicCallbackReactor.create (threading, exceptions);
		final BasicChannel channel = BasicChannel.create (input, output, coder, reactor, threading, exceptions);
		final BasicComponent component = BasicComponent.create (channel, reactor, exceptions);
		reactor.initialize ();
		channel.initialize ();
		component.initialize ();
		component.assign (callbacks);
		while (true) {
			if (!component.isActive ())
				break;
			if (!Threading.sleep (BasicComponentHarnessMain.sleepTimeout))
				break;
		}
		component.terminate ();
		channel.terminate ();
		reactor.terminate ();
	}
	
	public static final void main (final ComponentCallbacks callbacks, final SocketAddress address, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		Preconditions.checkNotNull (callbacks);
		Preconditions.checkNotNull (address);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final Socket connection;
		final InputStream input;
		final OutputStream output;
		try {
			final ServerSocket acceptor = new ServerSocket ();
			acceptor.setReuseAddress (true);
			acceptor.setSoTimeout (6 * 1000);
			acceptor.bind (address, 1);
			connection = acceptor.accept ();
			acceptor.close ();
			input = connection.getInputStream ();
			output = connection.getOutputStream ();
		} catch (final IOException exception) {
			exceptions.trace (ExceptionResolution.Deferred, exception);
			throw (new Error (exception));
		}
		try {
			BasicComponentHarnessMain.main (callbacks, input, output, threading, exceptions);
		} catch (final Error exception) {
			exceptions.trace (ExceptionResolution.Deferred, exception);
			throw (exception);
		} finally {
			try {
				connection.close ();
			} catch (final IOException exception) {
				exceptions.trace (ExceptionResolution.Deferred, exception);
				throw (new Error (exception));
			}
		}
	}
	
	public static final void main (final String componentArgument, final String classpathArgument, final String channelArgument, final String loggerArgument)
	{
		final BaseExceptionTracer exceptions = AbortingExceptionTracer.defaultInstance;
		BasicThreadingSecurityManager.initialize ();
		final BasicThreadingContext threading = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions.catcher);
		BasicComponentHarnessMain.main (componentArgument, classpathArgument, channelArgument, loggerArgument, threading, exceptions);
		threading.join ();
	}
	
	public static final void main (final String componentArgument, final String classpathArgument, final String channelArgument, final String loggerArgument, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		Preconditions.checkNotNull (componentArgument);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
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
							exceptions.trace (ExceptionResolution.Deferred, exception);
							throw (new IllegalArgumentException (String.format ("invalid class-path URL `%s`", classpathPart), exception));
						}
					else {
						throw (new IllegalArgumentException (String.format ("invalid class-path URL `%s`", classpathPart)));
					}
					classLoaderUrls.add (classpathUrl);
				}
			classLoader = new URLClassLoader (classLoaderUrls.toArray (new URL[0]), BasicComponentHarnessMain.class.getClassLoader ());
		} else
			classLoader = ClassLoader.getSystemClassLoader ();
		final Class<?> componentClass;
		try {
			componentClass = classLoader.loadClass (componentArgument);
		} catch (final Exception exception) {
			exceptions.trace (ExceptionResolution.Deferred, exception);
			throw (new IllegalArgumentException (String.format ("invalid component class `%s` (error encountered while resolving)", componentArgument), exception));
		}
		Preconditions.checkArgument (ComponentCallbacks.class.isAssignableFrom (componentClass), "invalid component class `%s` (not an instance of `ComponentCallbacks`)", componentClass.getName ());
		final ComponentCallbacks callbacks;
		try {
			callbacks = (ComponentCallbacks) componentClass.newInstance ();
		} catch (final Exception exception) {
			exceptions.trace (ExceptionResolution.Deferred, exception);
			throw (new IllegalArgumentException (String.format ("invalid component class `%s` (error encountered while instantiating)", componentClass.getName ()), exception));
		}
		if (loggerArgument != null) {
			final Logger logger = (Logger) LoggerFactory.getLogger (BasicComponentHarnessMain.class);
			final String[] loggerParts = loggerArgument.split (":");
			Preconditions.checkArgument (loggerParts.length == 2);
			final SocketAppender appender = new SocketAppender ();
			appender.setName ("remote");
			appender.setContext (logger.getLoggerContext ());
			appender.setRemoteHost (loggerParts[0]);
			appender.setPort (Integer.parseInt (loggerParts[1]));
			appender.start ();
			appender.setReconnectionDelay (1000);
			logger.addAppender (appender);
		}
		if ((channelArgument == null) || (channelArgument.equals ("stdio")))
			BasicComponentHarnessMain.main (callbacks, BasicComponentHarnessPreMain.stdin, BasicComponentHarnessPreMain.stdout, threading, exceptions);
		else {
			final String[] channelParts = channelArgument.split (":");
			Preconditions.checkArgument (channelParts.length == 2);
			final InetSocketAddress channelAddress = new InetSocketAddress (channelParts[0], Integer.parseInt (channelParts[1]));
			BasicComponentHarnessMain.main (callbacks, channelAddress, threading, exceptions);
		}
	}
	
	public static final void main (final String[] arguments)
	{
		Preconditions.checkNotNull (arguments);
		Preconditions.checkArgument ((arguments.length >= 1) && (arguments.length <= 4), "invalid arguments; aborting! (expected `<component-callbacks-class-name> <class-path-urls> <channel-endpoint> <logger-endpoint>`");
		final String componentArgument;
		final String classpathArgument;
		final String channelArgument;
		final String loggerArgument;
		if (arguments.length == 1) {
			componentArgument = arguments[0];
			classpathArgument = null;
			channelArgument = null;
			loggerArgument = null;
		} else if (arguments.length == 2) {
			componentArgument = arguments[0];
			classpathArgument = arguments[1];
			channelArgument = null;
			loggerArgument = null;
		} else if (arguments.length == 3) {
			componentArgument = arguments[0];
			classpathArgument = arguments[1];
			channelArgument = arguments[2];
			loggerArgument = null;
		} else if (arguments.length == 4) {
			componentArgument = arguments[0];
			classpathArgument = arguments[1];
			channelArgument = arguments[2];
			loggerArgument = arguments[3];
		} else
			throw (new AssertionError ());
		BasicComponentHarnessMain.main (componentArgument, classpathArgument, channelArgument, loggerArgument);
	}
	
	private static final long sleepTimeout = 100;
	
	private static final class Piper
			extends Object
			implements
				Runnable,
				Thread.UncaughtExceptionHandler
	{
		Piper (final ReadableByteChannel source, final WritableByteChannel sink, final ThreadingContext threading, final ExceptionTracer exceptions)
		{
			super ();
			this.threading = threading;
			this.transcript = Transcript.create (this);
			this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
			this.source = source;
			this.sink = sink;
			this.thread = this.threading.newThread (new ThreadConfiguration (this, "piper", this), this);
			this.thread.start ();
		}
		
		public final boolean join ()
		{
			return (Threading.join (this.thread));
		}
		
		@Override
		public final void run ()
		{
			final ByteBuffer buffer = ByteBuffer.allocateDirect (1024 * 1024);
			loop : while (true) {
				if ((!this.source.isOpen () && (buffer.remaining () == 0)) || !this.sink.isOpen ())
					break;
				buffer.position (0);
				buffer.limit (buffer.capacity ());
				if (this.source.isOpen ()) {
					this.transcript.traceDebugging ("accessing source...");
					try {
						final int outcome = this.source.read (buffer);
						if (outcome == -1)
							this.source.close ();
					} catch (final IOException exception) {
						this.exceptions.traceHandledException (exception, "i/o error encountered while reading from the source channel; aborting!");
						break loop;
					}
					buffer.flip ();
				}
				while (this.sink.isOpen ()) {
					this.transcript.traceDebugging ("accessing sink...");
					try {
						this.sink.write (buffer);
					} catch (final IOException exception) {
						this.exceptions.traceHandledException (exception, "i/o error encountered while writing to the sink channel; aborting!");
						break loop;
					}
					if (buffer.remaining () == 0)
						break;
				}
			}
			this.close ();
		}
		
		@Override
		public final void uncaughtException (final Thread thread, final Throwable exception)
		{
			Preconditions.checkArgument (this.thread == thread);
			this.exceptions.traceIgnoredException (exception);
			this.close ();
		}
		
		private final void close ()
		{
			this.transcript.traceDebugging ("closing source...");
			try {
				this.source.close ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
			this.transcript.traceDebugging ("closing sink...");
			try {
				this.sink.close ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		
		private final TranscriptExceptionTracer exceptions;
		private final WritableByteChannel sink;
		private final ReadableByteChannel source;
		private final Thread thread;
		private final ThreadingContext threading;
		private final Transcript transcript;
	}
}
