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
import eu.mosaic_cloud.tools.exceptions.core.DeferredException;
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


public final class MosBasicComponentLauncher
{
	private MosBasicComponentLauncher ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String callbacksClass, final String[] arguments, final ClassLoader classLoader)
			throws Throwable
	{
		Preconditions.checkNotNull (callbacksClass);
		Preconditions.checkNotNull (arguments);
		Preconditions.checkNotNull (classLoader);
		final String[] finalArguments = new String[arguments.length + 1];
		finalArguments[0] = callbacksClass;
		System.arraycopy (arguments, 0, finalArguments, 1, arguments.length);
		MosBasicComponentLauncher.main (finalArguments, classLoader);
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		MosBasicComponentLauncher.main (arguments, ClassLoader.getSystemClassLoader ());
	}
	
	public static final void main (final String[] arguments, final ClassLoader classLoader)
			throws Throwable
	{
		Preconditions.checkNotNull (arguments);
		Preconditions.checkNotNull (classLoader);
		BasicThreadingSecurityManager.initialize ();
		final BaseExceptionTracer exceptions = AbortingExceptionTracer.defaultInstance;
		final BasicThreadingContext threading = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions, exceptions.catcher);
		threading.initialize ();
		MosBasicComponentLauncher.main (arguments, classLoader, threading, exceptions);
		threading.destroy ();
	}
	
	public static final void main (final String[] arguments, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkNotNull (arguments);
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final Logger logger = (Logger) LoggerFactory.getLogger (MosBasicComponentLauncher.class);
		Preconditions.checkArgument ((arguments != null) && (arguments.length >= 2) && (("local".equals (arguments[1]) && (arguments.length == 6)) || ("remote".equals (arguments[1]) && (arguments.length == 7))), "invalid arguments: expected `<component-class> local <local-ip> <local-port> <mos-url> <component-configuration>` or `<component-callbacks> remote <local-ip> <local-port-1> <local-port-2> <mos-url> <component-configuration>`");
		Threading.registerExitCallback (threading, MosBasicComponentLauncher.class, "exit-hook", new Runnable () {
			@Override
			public final void run ()
			{
				logger.debug ("starting safety hook...");
				Threading.sleep (AbortingExceptionTracer.defaulExitTimeout);
				Threading.halt ();
			}
		});
		final AtomicReference<String> componentIdentifier = Atomics.newReference (null);
		final String componentCallbacksClassName = arguments[0];
		final URL controllerBaseUrl;
		final String[] controllerCreateUrlArguments;
		final Runnable run;
		if ("local".equals (arguments[1])) {
			final String componentConfigurationEncoded = arguments[5];
			final InetSocketAddress channelAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[3]));
			final String channelAddressEncoded = String.format ("tcp:%s:%d", channelAddress.getAddress ().getHostAddress (), Integer.valueOf (channelAddress.getPort ()));
			{
				final String[] componentCallbacksClassNameParts = componentCallbacksClassName.split ("\\.");
				final List<String> controllerCreateConfiguration = Arrays.asList (componentCallbacksClassNameParts[componentCallbacksClassNameParts.length - 1], channelAddressEncoded);
				final String controllerCreateTypeEncoded = URLEncoder.encode ("#mosaic-tests:socat", "UTF-8");
				final String controllerCreateConfigurationEncoded = URLEncoder.encode (DefaultJsonCoder.defaultInstance.encodeToString (controllerCreateConfiguration), "UTF-8");
				controllerCreateUrlArguments = new String[] {String.format ("type=%s", controllerCreateTypeEncoded), String.format ("configuration=%s", controllerCreateConfigurationEncoded), "count=1"};
				controllerBaseUrl = new URL (arguments[4]);
			}
			run = new Runnable () {
				@Override
				public final void run ()
				{
					final ArgumentsProvider argumentsProvider = new ArgumentsProvider () {
						@Override
						public final String getCallbacksClass ()
						{
							return (componentCallbacksClassName);
						}
						
						@Override
						public final List<String> getCallbacksOptions ()
						{
							return (Arrays.asList (componentConfigurationEncoded));
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
					} catch (final Throwable exeption) {
						throw (new DeferredException (exeption).wrap ());
					}
				}
			};
		} else if ("remote".equals (arguments[1])) {
			final String componentConfigurationEncoded = arguments[6];
			final Object componentConfiguration = DefaultJsonCoder.defaultInstance.decodeFromString (componentConfigurationEncoded);
			final InetSocketAddress exporterAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[3]));
			final InetSocketAddress appenderAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[4]));
			final String exporterAddressEncoded = String.format ("http://%s:%d/", exporterAddress.getAddress ().getHostAddress (), Integer.valueOf (exporterAddress.getPort ()));
			final String appenderAddressEncoded = String.format ("%s:%d", appenderAddress.getAddress ().getHostAddress (), Integer.valueOf (appenderAddress.getPort ()));
			{
				final List<Object> controllerCreateConfiguration = Arrays.asList (componentCallbacksClassName, componentConfiguration, exporterAddressEncoded, appenderAddressEncoded);
				final String controllerCreateTypeEncoded = URLEncoder.encode ("#mosaic-components:java-component-container", "UTF-8");
				final String controllerCreateConfigurationEncoded = URLEncoder.encode (DefaultJsonCoder.defaultInstance.encodeToString (controllerCreateConfiguration), "UTF-8");
				controllerCreateUrlArguments = new String[] {String.format ("type=%s", controllerCreateTypeEncoded), String.format ("configuration=%s", controllerCreateConfigurationEncoded), "count=1"};
				controllerBaseUrl = new URL (arguments[5]);
			}
			final ClasspathExporter exporter = ClasspathExporter.create (exporterAddress, classLoader, exceptions);
			final SimpleSocketServer appender = new SimpleSocketServer ((LoggerContext) LoggerFactory.getILoggerFactory (), appenderAddress.getPort ());
			Threading.registerExitCallback (threading, MosBasicComponentLauncher.class, "exit-hook-2", new Runnable () {
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
				exceptions.trace (ExceptionResolution.Deferred, exception);
				logger.error ("failed starting exporter; ignoring!", exception);
				throw (new Error (exception));
			}
			logger.debug ("starting appender...");
			try {
				appender.start ();
			} catch (final Throwable exception) {
				exceptions.trace (ExceptionResolution.Deferred, exception);
				logger.error ("failed starting appender; ignoring!", exception);
				throw (new Error (exception));
			}
			run = new Runnable () {
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
		} else
			throw (new IllegalStateException ());
		Threading.registerExitCallback (threading, MosBasicComponentLauncher.class, "exit-hook-3", new Runnable () {
			@Override
			public final void run ()
			{
				logger.debug ("stopping component...");
				final String identifier = componentIdentifier.get ();
				if (identifier != null)
					try {
						final URL stopUrl = new URL (controllerBaseUrl, String.format ("/processes/stop?key=%s", identifier));
						stopUrl.openStream ().close ();
					} catch (final Throwable exception) {
						exceptions.trace (ExceptionResolution.Ignored, exception);
						logger.error ("failed stopping component; ignoring!", exception);
					}
			}
		});
		logger.debug ("starting main...");
		final Thread main = Threading.createDaemonThread (threading, MosBasicComponentLauncher.class, "main", run);
		main.start ();
		Threading.sleep (MosBasicComponentLauncher.defaultCreateDelay);
		logger.debug ("creating component...");
		try {
			final URL controllerCreateUrl = new URL (controllerBaseUrl, String.format ("/processes/create?%s", Joiner.on ("&").join (Arrays.asList (controllerCreateUrlArguments))));
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
					exceptions.trace (ExceptionResolution.Deferred, exception);
					logger.error (String.format ("failed creating component: %s; ignoring!", createResponse), exception);
					throw (new Error (exception));
				}
			} catch (final Throwable exception) {
				exceptions.trace (ExceptionResolution.Deferred, exception);
				logger.error ("failed creating component; ignoring!", exception);
				throw (new Error (exception));
			}
		} catch (final Throwable exception) {
			exceptions.trace (ExceptionResolution.Deferred, exception);
			logger.error ("failed creating component; ignoring!", exception);
			throw (new Error (exception));
		}
		logger.info ("started: {}", componentIdentifier.get ());
		main.join ();
	}
	
	public static final long defaultCreateDelay = 1000;
}
