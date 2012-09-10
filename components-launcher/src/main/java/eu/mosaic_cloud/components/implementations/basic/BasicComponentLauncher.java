/*
 * #%L
 * mosaic-components-launcher
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


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessMain.ArgumentsProvider;
import eu.mosaic_cloud.tools.classpath_exporter.ClasspathExporter;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.json.tools.DefaultJsonCoder;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.Atomics;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;


public final class BasicComponentLauncher
{
	private BasicComponentLauncher ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String componentCallbacks, final String componentConfiguration, final String[] arguments)
			throws Throwable
	{
		BasicComponentLauncher.main (componentCallbacks, componentConfiguration, arguments, 0);
	}
	
	public static final void main (final String componentCallbacks, final String componentConfiguration, final String[] arguments, final int argumentsOffset)
			throws Throwable
	{
		Preconditions.checkNotNull (componentCallbacks);
		Preconditions.checkNotNull (componentConfiguration);
		Preconditions.checkNotNull (arguments);
		Preconditions.checkArgument ((argumentsOffset >= 0) && (argumentsOffset <= arguments.length));
		final String[] finalArguments = new String[(arguments.length + 2) - argumentsOffset];
		finalArguments[0] = componentCallbacks;
		finalArguments[1] = componentConfiguration;
		System.arraycopy (arguments, argumentsOffset, finalArguments, 2, arguments.length - argumentsOffset);
		BasicComponentLauncher.main (finalArguments);
	}
	
	public static final void main (final String componentCallbacks, final String[] arguments)
			throws Throwable
	{
		BasicComponentLauncher.main (componentCallbacks, arguments, 0);
	}
	
	public static final void main (final String componentCallbacks, final String[] arguments, final int argumentsOffset)
			throws Throwable
	{
		BasicComponentLauncher.main (componentCallbacks, "null", arguments, argumentsOffset);
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		Preconditions.checkNotNull (arguments);
		BasicThreadingSecurityManager.initialize ();
		final BaseExceptionTracer exceptions = AbortingExceptionTracer.defaultInstance;
		final BasicThreadingContext threading = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions, exceptions.catcher);
		threading.initialize ();
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader ();
		BasicComponentLauncher.main (arguments, classLoader, threading, exceptions);
		threading.destroy ();
	}
	
	public static final void main (final String[] arguments, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length >= 3), "invalid arguments; expected `<component-callbacks> <component-configuration> <mode> ...`");
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final String mode = arguments[2];
		final String[] finalArguments = new String[arguments.length - 1];
		System.arraycopy (arguments, 0, finalArguments, 0, 2);
		System.arraycopy (arguments, 3, finalArguments, 2, arguments.length - 3);
		if ("local".equals (mode))
			BasicComponentLauncher.runLocal (finalArguments, classLoader, threading, exceptions);
		else if ("remote".equals (mode))
			BasicComponentLauncher.runRemote (finalArguments, classLoader, threading, exceptions);
		else
			throw (new IllegalArgumentException (String.format ("invalid mode `%s`", mode)));
	}
	
	public static final void runLocal (final String[] arguments, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length == 5), "invalid arguments; expected `<component-callbacks> <component-configuration> <local-ip> <local-port> <controller-url>`");
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final String componentCallbacks = arguments[0];
		final String componentConfiguration = arguments[1];
		final InetSocketAddress channelAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[3]));
		final URL controllerBaseUrl = new URL (arguments[4]);
		BasicComponentLauncher.runLocal (controllerBaseUrl, channelAddress, componentCallbacks, Arrays.asList (componentConfiguration), classLoader, threading, exceptions);
	}
	
	public static final void runLocal (final URL controllerBaseUrl, final InetSocketAddress channelAddress, final String componentCallbacks, final List<String> componentConfiguration)
			throws Throwable
	{
		Preconditions.checkNotNull (controllerBaseUrl);
		Preconditions.checkNotNull (channelAddress);
		Preconditions.checkNotNull (componentCallbacks);
		Preconditions.checkNotNull (componentConfiguration);
		BasicThreadingSecurityManager.initialize ();
		final BaseExceptionTracer exceptions = AbortingExceptionTracer.defaultInstance;
		final BasicThreadingContext threading = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions, exceptions.catcher);
		threading.initialize ();
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader ();
		BasicComponentLauncher.runLocal (controllerBaseUrl, channelAddress, componentCallbacks, componentConfiguration, classLoader, threading, exceptions);
		threading.destroy ();
	}
	
	public static final void runLocal (final URL controllerBaseUrl, final InetSocketAddress channelAddress, final String componentCallbacks, final List<String> componentConfiguration, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkNotNull (controllerBaseUrl);
		Preconditions.checkNotNull (channelAddress);
		Preconditions.checkNotNull (componentCallbacks);
		Preconditions.checkNotNull (componentConfiguration);
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final String channelAddressEncoded = String.format ("tcp:%s:%d", channelAddress.getAddress ().getHostAddress (), Integer.valueOf (channelAddress.getPort ()));
		final String[] controllerCreateUrlArguments;
		{
			final String[] componentCallbacksClassNameParts = componentCallbacks.split ("\\.");
			final List<String> controllerCreateConfiguration = Arrays.asList (componentCallbacksClassNameParts[componentCallbacksClassNameParts.length - 1], channelAddressEncoded);
			final String controllerCreateTypeEncoded = URLEncoder.encode ("#mosaic-tests:socat", "UTF-8");
			final String controllerCreateConfigurationEncoded = URLEncoder.encode (DefaultJsonCoder.defaultInstance.encodeToString (controllerCreateConfiguration), "UTF-8");
			controllerCreateUrlArguments = new String[] {String.format ("type=%s", controllerCreateTypeEncoded), String.format ("configuration=%s", controllerCreateConfigurationEncoded), "count=1"};
		}
		final AtomicReference<String> componentIdentifier = Atomics.newReference (null);
		final Runnable run = new Runnable () {
			@Override
			public final void run ()
			{
				final ArgumentsProvider argumentsProvider = new ArgumentsProvider () {
					@Override
					public final String getCallbacksClass ()
					{
						return (componentCallbacks);
					}
					
					@Override
					public final List<String> getCallbacksOptions ()
					{
						return (componentConfiguration);
					}
					
					@Override
					public final String getChannelEndpoint ()
					{
						return (channelAddressEncoded);
					}
					
					@Override
					public final String getClasspath ()
					{
						return (null);
					}
					
					@Override
					public final String getIdentifier ()
					{
						return (componentIdentifier.get ());
					}
					
					@Override
					public final String getLoggingEndpoint ()
					{
						return (null);
					}
				};
				try {
					BasicComponentHarnessMain.main (argumentsProvider, classLoader, threading, null, exceptions);
				} catch (final CaughtException.Wrapper wrapper) {
					wrapper.trace (exceptions);
					exceptions.trace (ExceptionResolution.Ignored, wrapper.exception.caught);
				} catch (final Throwable exception) {
					exceptions.trace (ExceptionResolution.Ignored, exception);
				}
			}
		};
		BasicComponentLauncher.run (run, controllerBaseUrl, controllerCreateUrlArguments, componentIdentifier, threading, exceptions);
	}
	
	public static final void runRemote (final String[] arguments, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length == 6), "invalid arguments: expected ` <component-callbacks> <component-configuration> <local-ip> <local-port-1> <local-port-2> <controller-url>`");
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final String componentCallbacks = arguments[0];
		final String componentConfiguration = arguments[1];
		final InetSocketAddress exporterAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[3]));
		final InetSocketAddress appenderAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[4]));
		final URL controllerBaseUrl = new URL (arguments[5]);
		BasicComponentLauncher.runRemote (controllerBaseUrl, exporterAddress, appenderAddress, componentCallbacks, componentConfiguration, classLoader, threading, exceptions);
	}
	
	public static final void runRemote (final URL controllerBaseUrl, final InetSocketAddress exporterAddress, final InetSocketAddress appenderAddress, final String componentCallbacks, final String componentConfiguration, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkNotNull (controllerBaseUrl);
		Preconditions.checkNotNull (exporterAddress);
		Preconditions.checkNotNull (appenderAddress);
		Preconditions.checkNotNull (componentCallbacks);
		Preconditions.checkNotNull (componentConfiguration);
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final Logger logger = (Logger) LoggerFactory.getLogger (BasicComponentLauncher.class);
		final Object componentConfigurationDecoded = DefaultJsonCoder.defaultInstance.decodeFromString (componentConfiguration);
		final String exporterAddressEncoded = String.format ("http://%s:%d/", exporterAddress.getAddress ().getHostAddress (), Integer.valueOf (exporterAddress.getPort ()));
		final String appenderAddressEncoded = String.format ("%s:%d", appenderAddress.getAddress ().getHostAddress (), Integer.valueOf (appenderAddress.getPort ()));
		final String[] controllerCreateUrlArguments;
		{
			final List<Object> controllerCreateConfiguration = Arrays.asList (componentCallbacks, componentConfigurationDecoded, Arrays.asList (exporterAddressEncoded), appenderAddressEncoded);
			final String controllerCreateTypeEncoded = URLEncoder.encode ("#mosaic-components:java-component-container", "UTF-8");
			final String controllerCreateConfigurationEncoded = URLEncoder.encode (DefaultJsonCoder.defaultInstance.encodeToString (controllerCreateConfiguration), "UTF-8");
			controllerCreateUrlArguments = new String[] {String.format ("type=%s", controllerCreateTypeEncoded), String.format ("configuration=%s", controllerCreateConfigurationEncoded), "count=1"};
		}
		final ClasspathExporter exporter = ClasspathExporter.create (exporterAddress, classLoader, exceptions);
		final SimpleSocketServer appender = new SimpleSocketServer ((LoggerContext) LoggerFactory.getILoggerFactory (), appenderAddress.getPort ());
		Threading.registerExitCallback (threading, BasicComponentLauncher.class, "exit-hook-2", new Runnable () {
			@Override
			public final void run ()
			{
				logger.debug ("stopping exporter...");
				try {
					exporter.stopServer ();
				} catch (final Throwable exception) {
					exceptions.trace (ExceptionResolution.Ignored, exception);
					logger.error ("failed stopping exporter; ignoring!", exception);
				}
				logger.debug ("sopping appender...");
				try {
					appender.close ();
				} catch (final Throwable exception) {
					exceptions.trace (ExceptionResolution.Ignored, exception);
					logger.error ("failed stopping appender; ignoring!", exception);
				}
			}
		});
		logger.debug ("starting exporter...");
		try {
			exporter.startServer ();
		} catch (final Throwable exception) {
			exceptions.trace (ExceptionResolution.Ignored, exception);
			logger.error ("failed starting exporter; ignoring!", exception);
			throw (new Error (exception));
		}
		logger.debug ("starting appender...");
		try {
			appender.start ();
		} catch (final Throwable exception) {
			exceptions.trace (ExceptionResolution.Ignored, exception);
			logger.error ("failed starting appender; ignoring!", exception);
			throw (new Error (exception));
		}
		final AtomicReference<String> componentIdentifier = Atomics.newReference (null);
		final Runnable run = new Runnable () {
			@Override
			public final void run ()
			{
				try {
					new BufferedReader (new InputStreamReader (System.in)).readLine ();
				} catch (final Throwable exception) {
					exceptions.trace (ExceptionResolution.Ignored, exception);
				}
			}
		};
		BasicComponentLauncher.run (run, controllerBaseUrl, controllerCreateUrlArguments, componentIdentifier, threading, exceptions);
	}
	
	private static final void run (final Runnable run, final URL controllerBaseUrl, final String[] controllerCreateUrlArguments, final AtomicReference<String> componentIdentifier, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (BasicComponentLauncher.class);
		Threading.registerExitCallback (threading, BasicComponentLauncher.class, "exit-hook", new Runnable () {
			@Override
			public final void run ()
			{
				logger.debug ("starting safety hook...");
				Threading.sleep (AbortingExceptionTracer.defaulExitTimeout);
				Threading.halt ();
			}
		});
		Threading.registerExitCallback (threading, BasicComponentLauncher.class, "exit-hook-3", new Runnable () {
			@Override
			public final void run ()
			{
				logger.debug ("stopping component...");
				final String identifier = componentIdentifier.get ();
				if (identifier != null)
					try {
						final URL stopUrl = new URL (controllerBaseUrl, String.format ("/v1/processes/stop?key=%s", identifier));
						stopUrl.openStream ().close ();
					} catch (final Throwable exception) {
						exceptions.trace (ExceptionResolution.Ignored, exception);
						logger.error ("failed stopping component; ignoring!", exception);
					}
			}
		});
		logger.debug ("starting main...");
		final Thread main = Threading.createDaemonThread (threading, BasicComponentLauncher.class, "main", run);
		main.start ();
		Threading.sleep (BasicComponentLauncher.defaultCreateDelay);
		logger.debug ("creating component...");
		try {
			final URL controllerCreateUrl = new URL (controllerBaseUrl, String.format ("/v1/processes/create?%s", Joiner.on ("&").join (Arrays.asList (controllerCreateUrlArguments))));
			logger.debug ("{}", controllerCreateUrl.toString ());
			try {
				final InputStream createStream = controllerCreateUrl.openStream ();
				final String createResponse = new String (ByteStreams.toByteArray (createStream), Charset.forName ("UTF-8"));
				try {
					final Map<?, ?> createResponseMap = (Map<?, ?>) DefaultJsonCoder.defaultInstance.decodeFromString (createResponse);
					Preconditions.checkState (Boolean.TRUE.equals (createResponseMap.get ("ok")));
					Preconditions.checkState (((List<?>) createResponseMap.get ("keys")).size () > 0);
					final String identifier = (String) ((List<?>) createResponseMap.get ("keys")).get (0);
					Preconditions.checkNotNull (identifier);
					componentIdentifier.set (identifier);
				} catch (final Throwable exception) {
					exceptions.trace (ExceptionResolution.Ignored, exception);
					logger.error (String.format ("failed creating component: %s; ignoring!", createResponse), exception);
					throw (new Error (exception));
				}
			} catch (final Throwable exception) {
				exceptions.trace (ExceptionResolution.Ignored, exception);
				logger.error ("failed creating component; ignoring!", exception);
				throw (new Error (exception));
			}
		} catch (final Throwable exception) {
			exceptions.trace (ExceptionResolution.Ignored, exception);
			logger.error ("failed creating component; ignoring!", exception);
			throw (new Error (exception));
		}
		logger.info ("started: {}", componentIdentifier.get ());
		main.join ();
	}
	
	public static final long defaultCreateDelay = 1000;
}
