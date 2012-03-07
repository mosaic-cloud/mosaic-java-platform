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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;

import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCallbacksProvider;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.components.tools.DefaultChannelMessageCoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.UncaughtExceptionHandler;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.net.SocketAppender;


public final class BasicComponentHarnessMain
		extends Object
{
	private BasicComponentHarnessMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final ArgumentsProvider arguments, final ClassLoader classLoader, final ThreadingContext threading, final Transcript transcript, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkNotNull (arguments);
		final Environment environment = BasicComponentHarnessMain.prepareEnvironment (arguments, classLoader, threading, transcript, exceptions);
		BasicComponentHarnessMain.main (environment, arguments);
	}
	
	public static final void main (final Environment environment, final ArgumentsProvider arguments)
			throws Throwable
	{
		Preconditions.checkNotNull (environment);
		Preconditions.checkNotNull (arguments);
		BasicComponentHarnessMain.prepareLogger (environment, arguments);
		final ComponentCallbacksProvider callbacksProvider = BasicComponentHarnessMain.prepareCallbacksProvider (environment, arguments);
		final BasicChannel channel = BasicComponentHarnessMain.prepareChannel (environment, arguments);
		final BasicComponent component = BasicComponentHarnessMain.prepareComponent (environment, arguments, channel, callbacksProvider);
		environment.transcript.traceInformation ("joining component...");
		component.await ();
		environment.transcript.traceInformation ("joined component.");
	}
	
	public static final void main (final String[] argumentsList)
			throws Throwable
	{
		Preconditions.checkNotNull (argumentsList);
		final Transcript transcript = Transcript.create (BasicComponentHarnessMain.class);
		final BaseExceptionTracer exceptions = TranscriptExceptionTracer.create (transcript, AbortingExceptionTracer.defaultInstance);
		final ArgumentsProvider arguments = BasicComponentHarnessMain.parseArguments (argumentsList, transcript, exceptions);
		BasicComponentHarnessMain.main (arguments, null, null, transcript, exceptions);
	}
	
	private static final ArgumentsProvider parseArguments (final String[] argumentsList, final Transcript transcript, final ExceptionTracer exceptions)
			throws Throwable
	{
		transcript.traceDebugging ("parsing arguments: `%{array}`...", (Object) argumentsList);
		final ArgumentsProvider arguments = CliFactory.parseArguments (ArgumentsProvider.class, argumentsList);
		return (arguments);
	}
	
	private static final ComponentCallbacksProvider prepareCallbacksProvider (final Environment environment, final ArgumentsProvider arguments)
			throws Throwable
	{
		environment.transcript.traceInformation ("preparing callbacks provider...");
		final String callbacksClassName = arguments.getCallbacks ();
		environment.transcript.traceDebugging ("resolving callbacks class `%s`...", callbacksClassName);
		Preconditions.checkNotNull (callbacksClassName);
		final Class<?> callbacksClass = environment.classLoader.loadClass (callbacksClassName);
		Preconditions.checkArgument (ComponentCallbacks.class.isAssignableFrom (callbacksClass), "invalid callbacks class `%s` (not an instance of `ComponentCallbacks`)", callbacksClass.getName ());
		final ComponentCallbacksProvider callbacksProvider = new Provider (callbacksClass);
		environment.transcript.traceInformation ("prepared callbacks provider.");
		return (callbacksProvider);
	}
	
	private static final BasicChannel prepareChannel (final Environment environment, final ArgumentsProvider arguments)
			throws Throwable
	{
		environment.transcript.traceInformation ("preparing channel...");
		final String endpoint = arguments.getChannelEndpoint ();
		final InputStream inputStream;
		final OutputStream outputStream;
		if ((endpoint == null) || (endpoint.equals ("stdio"))) {
			environment.transcript.traceDebugging ("creating stdio streams...");
			inputStream = BasicComponentHarnessPreMain.stdin;
			outputStream = BasicComponentHarnessPreMain.stdout;
		} else {
			environment.transcript.traceDebugging ("creating socket streams (forwarding to `%s`)...", endpoint);
			final String[] endpointParts = endpoint.split (":");
			Preconditions.checkArgument (endpointParts.length == 2);
			final InetSocketAddress channelAddress = new InetSocketAddress (endpointParts[0], Integer.parseInt (endpointParts[1]));
			final Socket channelConnection;
			final ServerSocket channelAcceptor = new ServerSocket ();
			environment.transcript.traceDebugging ("listening socket...");
			channelAcceptor.setReuseAddress (true);
			channelAcceptor.setSoTimeout (6 * 1000);
			channelAcceptor.bind (channelAddress, 1);
			channelConnection = channelAcceptor.accept ();
			channelAcceptor.close ();
			inputStream = channelConnection.getInputStream ();
			outputStream = channelConnection.getOutputStream ();
			environment.transcript.traceDebugging ("accepted socket.");
		}
		environment.transcript.traceDebugging ("creating pipes...");
		final Pipe inputPipe = Pipe.open ();
		final Pipe outputPipe = Pipe.open ();
		environment.transcript.traceDebugging ("creating pipers...");
		final Piper inputPiper = new Piper (Channels.newChannel (inputStream), inputPipe.sink (), environment.threading, environment.exceptions);
		final Piper outputPiper = new Piper (outputPipe.source (), Channels.newChannel (outputStream), environment.threading, environment.exceptions);
		environment.transcript.traceDebugging ("creating coder...");
		final DefaultChannelMessageCoder coder = DefaultChannelMessageCoder.create ();
		environment.transcript.traceDebugging ("creating channel...");
		final BasicChannel channel = BasicChannel.create (inputPipe.source (), outputPipe.sink (), coder, environment.reactor, environment.threading, environment.exceptions);
		environment.transcript.traceDebugging ("initializing channel...");
		channel.initialize ();
		environment.transcript.traceInformation ("prepared channel.");
		return (channel);
	}
	
	private static final ClassLoader prepareClassLoader (final ArgumentsProvider arguments, final Transcript transcript, final ExceptionTracer exceptions)
	{
		transcript.traceInformation ("preparing class loader...");
		final String classpath = arguments.getClasspath ();
		final ClassLoader classLoader;
		if (classpath != null) {
			transcript.traceDebugging ("creating class loader...");
			final LinkedList<URL> classLoaderUrls = new LinkedList<URL> ();
			for (final String classpathPart : classpath.split ("\\|"))
				if (classpathPart.length () > 0) {
					final URL classpathUrl;
					if (classpathPart.startsWith ("http:") || classpathPart.startsWith ("file:"))
						try {
							classpathUrl = new URL (classpathPart);
						} catch (final Exception exception) {
							throw (new IllegalArgumentException (String.format ("invalid class-path URL `%s`", classpathPart), exception));
						}
					else {
						throw (new IllegalArgumentException (String.format ("invalid class-path URL `%s`", classpathPart)));
					}
					transcript.traceDebugging ("initializing class loader with URL `%s`...", classpathUrl.toExternalForm ());
					classLoaderUrls.add (classpathUrl);
				}
			classLoader = new URLClassLoader (classLoaderUrls.toArray (new URL[0]), BasicComponentHarnessMain.class.getClassLoader ());
			transcript.traceInformation ("prepared class loader.");
		} else {
			transcript.traceInformation ("no customized class loader configured...");
			classLoader = ClassLoader.getSystemClassLoader ();
		}
		return (classLoader);
	}
	
	private static final BasicComponent prepareComponent (final Environment environment, final ArgumentsProvider arguments, final BasicChannel channel, final ComponentCallbacksProvider callbacksProvider)
			throws Throwable
	{
		environment.transcript.traceInformation ("preparing component...");
		environment.transcript.traceDebugging ("creating component...");
		final BasicComponent component = BasicComponent.create (environment.reactor, environment.exceptions);
		environment.transcript.traceDebugging ("initializing component...");
		component.initialize ();
		environment.transcript.traceDebugging ("creating callbacks...");
		final ComponentController componentController = component.getController ();
		final ComponentCallbacks componentCallbacks = callbacksProvider.provide (ComponentEnvironment.create (componentController, BasicComponentHarnessMain.class.getClassLoader (), environment.reactor, environment.threading, environment.exceptions));
		environment.transcript.traceDebugging ("binding callbacks...");
		componentController.bind (componentCallbacks, channel.getController ());
		environment.transcript.traceInformation ("prepared component.");
		return (component);
	}
	
	private static final Environment prepareEnvironment (final ArgumentsProvider arguments, final ClassLoader classLoader_, final ThreadingContext threading_, final Transcript transcript_, final ExceptionTracer exceptions_)
			throws Throwable
	{
		BasicThreadingSecurityManager.initialize ();
		final Transcript transcript;
		if (transcript_ == null)
			transcript = Transcript.create (BasicComponentHarnessMain.class);
		else
			transcript = transcript_;
		final ExceptionTracer exceptions;
		if (exceptions_ == null)
			exceptions = TranscriptExceptionTracer.create (transcript, AbortingExceptionTracer.defaultInstance);
		else
			exceptions = exceptions_;
		final ClassLoader classLoader;
		if (classLoader_ == null)
			classLoader = BasicComponentHarnessMain.prepareClassLoader (arguments, transcript, exceptions);
		else
			classLoader = classLoader_;
		final ThreadingContext threading;
		if (threading_ == null) {
			transcript.traceDebugging ("creating threading context...");
			final BasicThreadingContext threading1 = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions, UncaughtExceptionHandler.create (exceptions));
			transcript.traceDebugging ("initializing threading context...");
			threading1.initialize ();
			threading = threading1;
		} else
			threading = threading_;
		final BasicCallbackReactor reactor;
		{
			transcript.traceDebugging ("creating callbacks reactor...");
			reactor = BasicCallbackReactor.create (threading, exceptions);
			transcript.traceDebugging ("initializing callbacks reactor....");
			reactor.initialize ();
		}
		final Environment environment = new Environment (classLoader, reactor, threading, transcript, exceptions);
		return (environment);
	}
	
	private static final void prepareLogger (final Environment environment, final ArgumentsProvider arguments)
			throws Throwable
	{
		environment.transcript.traceInformation ("preparing logger...");
		final String endpoint = arguments.getLoggingEndpoint ();
		if (endpoint != null) {
			environment.transcript.traceDebugging ("creating logger (forwarding to `%s`)...", endpoint);
			final Logger logger = (Logger) LoggerFactory.getLogger (org.slf4j.Logger.ROOT_LOGGER_NAME);
			final String[] endpointParts = endpoint.split (":");
			Preconditions.checkArgument (endpointParts.length == 2);
			final InetSocketAddress address = new InetSocketAddress (endpointParts[0], Integer.parseInt (endpointParts[1]));
			final SocketAppender appender = new SocketAppender ();
			appender.setName ("remote");
			appender.setContext (logger.getLoggerContext ());
			appender.setRemoteHost (address.getHostString ());
			appender.setPort (address.getPort ());
			environment.transcript.traceDebugging ("starting logger...");
			appender.start ();
			appender.setReconnectionDelay (1000);
			environment.transcript.traceDebugging ("registering logger...");
			logger.addAppender (appender);
		} else
			environment.transcript.traceDebugging ("no customized logger configured...");
		environment.transcript.traceInformation ("prepared logging.");
	}
	
	public static interface ArgumentsProvider
	{
		@Option (longName = "callbacks-class")
		public abstract String getCallbacks ();
		
		@Option (longName = "channel-endpoint", defaultToNull = true)
		public abstract String getChannelEndpoint ();
		
		@Option (longName = "classpath", defaultToNull = true)
		public abstract String getClasspath ();
		
		@Option (longName = "identifier", defaultToNull = true)
		public abstract String getIdentifier ();
		
		@Option (longName = "logging-endpoint", defaultToNull = true)
		public abstract String getLoggingEndpoint ();
	}
	
	public static final class Environment
	{
		public Environment (final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final Transcript transcript, final ExceptionTracer exceptions)
		{
			super ();
			Preconditions.checkNotNull (classLoader);
			Preconditions.checkNotNull (reactor);
			Preconditions.checkNotNull (threading);
			Preconditions.checkNotNull (transcript);
			Preconditions.checkNotNull (exceptions);
			this.classLoader = classLoader;
			this.reactor = reactor;
			this.threading = threading;
			this.transcript = transcript;
			this.exceptions = exceptions;
		}
		
		public final ClassLoader classLoader;
		public final ExceptionTracer exceptions;
		public final CallbackReactor reactor;
		public final ThreadingContext threading;
		public final Transcript transcript;
	}
	
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
			this.thread = Threading.createAndStartDaemonThread (this.threading, this, "piper", this, exceptions, this);
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
	
	private static final class Provider
			extends Object
			implements
				ComponentCallbacksProvider
	{
		Provider (final Class<?> clasz)
		{
			super ();
			Preconditions.checkNotNull (clasz);
			this.clasz = clasz;
		}
		
		@Override
		public final ComponentCallbacks provide (final ComponentEnvironment context)
		{
			Preconditions.checkNotNull (context);
			Method provideMethod;
			try {
				provideMethod = this.clasz.getMethod ("provide", ComponentEnvironment.class);
			} catch (final NoSuchMethodException exception) {
				provideMethod = null;
			}
			Constructor<?> provideConstructor;
			try {
				provideConstructor = this.clasz.getConstructor (ComponentEnvironment.class);
			} catch (final NoSuchMethodException exception) {
				provideConstructor = null;
			}
			Preconditions.checkArgument ((provideMethod != null) || (provideConstructor != null));
			final ComponentCallbacks callbacks;
			if (provideMethod != null) {
				final CallbackProxy callbacksProxy;
				try {
					try {
						callbacksProxy = (CallbackProxy) provideMethod.invoke (null, context);
					} catch (final InvocationTargetException exception) {
						context.exceptions.trace (ExceptionResolution.Handled, exception);
						throw (exception.getCause ());
					}
				} catch (final Throwable exception) {
					context.exceptions.trace (ExceptionResolution.Deferred, exception);
					throw (new IllegalArgumentException (String.format ("invalid callbacks provider class `%s` (error encountered while invocking)", this.clasz.getName ()), exception));
				} finally {
					Threading.setDefaultContext (null);
				}
				Preconditions.checkArgument (callbacksProxy != null, "invalid callbacks (is null)");
				Preconditions.checkArgument (ComponentCallbacks.class.isInstance (callbacksProxy), "invalid callbacks proxy `%s` (not an instance of `ComponentCallbacks`)", callbacksProxy.getClass ().getName ());
				Preconditions.checkArgument (CallbackProxy.class.isInstance (callbacksProxy), "invalid callbacks proxy `%s` (not an instance of `CallbackProxy`)", callbacksProxy.getClass ().getName ());
				callbacks = (ComponentCallbacks) callbacksProxy;
			} else if (provideConstructor != null) {
				final CallbackHandler callbacksHandler;
				Threading.setDefaultContext (context.threading);
				try {
					callbacksHandler = (CallbackHandler) provideConstructor.newInstance (context);
				} catch (final Exception exception) {
					context.exceptions.trace (ExceptionResolution.Deferred, exception);
					throw (new IllegalArgumentException (String.format ("invalid callbacks handler class `%s` (error encountered while instantiating)", this.clasz.getName ()), exception));
				} finally {
					Threading.setDefaultContext (null);
				}
				Preconditions.checkArgument (ComponentCallbacks.class.isInstance (callbacksHandler), "invalid callbacks handler class `%s` (not an instance of `ComponentCallbacks`)", callbacksHandler.getClass ().getName ());
				Preconditions.checkArgument (CallbackHandler.class.isInstance (callbacksHandler), "invalid callbacks handler class `%s` (not an instance of `CallbackHandler`)", callbacksHandler.getClass ().getName ());
				final CallbackIsolate callbacksIsolate = context.reactor.createIsolate ();
				final ComponentCallbacks callbacksProxy = context.reactor.createProxy (ComponentCallbacks.class);
				Preconditions.checkState (context.reactor.assignHandler (callbacksProxy, callbacksHandler, callbacksIsolate).await ());
				callbacks = callbacksProxy;
			} else
				throw (new AssertionError ());
			Preconditions.checkNotNull (callbacks);
			return (callbacks);
		}
		
		private final Class<?> clasz;
	}
}
