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

import eu.mosaic_cloud.components.core.ComponentCallbacks;
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
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

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
	
	public static final void main (final Class<? extends ComponentCallbacks> callbacksClass, final String[] arguments)
			throws Throwable
	{
		Preconditions.checkNotNull (callbacksClass);
		MosBasicComponentLauncher.main (callbacksClass.getName (), arguments, callbacksClass.getClassLoader ());
	}
	
	public static final void main (final String callbacksClass, final String[] arguments)
			throws Throwable
	{
		MosBasicComponentLauncher.main (callbacksClass, arguments, null);
	}
	
	public static final void main (final String callbacksClass, final String[] arguments, final ClassLoader loader)
			throws Throwable
	{
		Preconditions.checkNotNull (callbacksClass);
		Preconditions.checkArgument (arguments != null);
		final String[] finalArguments = new String[arguments.length + 1];
		finalArguments[0] = callbacksClass;
		System.arraycopy (arguments, 0, finalArguments, 1, arguments.length);
		MosBasicComponentLauncher.main (finalArguments, loader);
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		MosBasicComponentLauncher.main (arguments, null);
	}
	
	public static final void main (final String[] arguments, final ClassLoader loader)
			throws Throwable
	{
		BasicThreadingSecurityManager.initialize ();
		final BaseExceptionTracer exceptions = AbortingExceptionTracer.defaultInstance;
		final BasicThreadingContext threading = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions, exceptions.catcher);
		threading.initialize ();
		MosBasicComponentLauncher.main (arguments, loader, threading, exceptions);
		threading.destroy ();
	}
	
	public static final void main (final String[] arguments, final ClassLoader loader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final Logger logger = (Logger) LoggerFactory.getLogger (MosBasicComponentLauncher.class);
		Preconditions.checkArgument ((arguments != null) && (arguments.length >= 2) && (("local".equals (arguments[1]) && (arguments.length == 5)) || ("remote".equals (arguments[1]) && (arguments.length == 6))), "invalid arguments: expected `<class> local <ip> <port> <mos-url>` or `<class> remote <ip> <port-1> <port-2> <mos-url>`");
		final boolean[] shouldStop = new boolean[] {false};
		{
			Threading.registerExitCallback (threading, MosBasicComponentLauncher.class, "exit-hook-1", new Runnable () {
				@Override
				public final void run ()
				{
					shouldStop[0] = true;
					logger.debug ("starting safety hook...");
					Threading.createAndStartDaemonThread (threading, MosBasicComponentLauncher.class, "exiter", new Runnable () {
						@Override
						public final void run ()
						{
							Threading.sleep (AbortingExceptionTracer.defaulExitTimeout);
							Threading.halt ();
						}
					});
				}
			});
		}
		final String clasz = arguments[0];
		final URL controller;
		final String[] createParameters;
		final Runnable run;
		if ("local".equals (arguments[1])) {
			final InetSocketAddress channelAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[3]));
			controller = new URL (arguments[4]);
			final String[] claszParts = clasz.split ("\\.");
			final List<String> configuration = Arrays.asList (claszParts[claszParts.length - 1], String.format (String.format ("tcp:%s:%d", channelAddress.getAddress ().getHostAddress (), Integer.valueOf (channelAddress.getPort ()))));
			createParameters = new String[] {String.format ("type=%s", URLEncoder.encode ("#mosaic-tests:socat", "UTF-8")), String.format ("configuration=%s", DefaultJsonCoder.defaultInstance.encodeToString (configuration)), "count=1"};
			run = new Runnable () {
				@Override
				public final void run ()
				{
					final ArgumentsProvider argumentsProvider = new ArgumentsProvider () {
						@Override
						public final String getCallbacksClass ()
						{
							return (clasz);
						}
						
						@Override
						public final String getCallbacksOptions ()
						{
							return (null);
						}
						
						@Override
						public final String getChannelEndpoint ()
						{
							return (String.format ("%s:%d", channelAddress.getAddress ().getHostAddress (), Integer.valueOf (channelAddress.getPort ())));
						}
						
						@Override
						public final String getClasspath ()
						{
							return (null);
						}
						
						@Override
						public final String getIdentifier ()
						{
							return (null);
						}
						
						@Override
						public final String getLoggingEndpoint ()
						{
							return (null);
						}
					};
					try {
						BasicComponentHarnessMain.main (argumentsProvider, loader, threading, null, exceptions);
					} catch (final Throwable exeption) {
						throw (CaughtException.create (ExceptionResolution.Deferred, exeption).wrap ());
					}
				}
			};
		} else if ("remote".equals (arguments[1])) {
			final InetSocketAddress httpAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[3]));
			final InetSocketAddress logbackAddress = new InetSocketAddress (arguments[2], Integer.parseInt (arguments[4]));
			controller = new URL (arguments[5]);
			final List<String> configuration = Arrays.asList (clasz, String.format ("http://%s:%d/", httpAddress.getAddress ().getHostAddress (), Integer.valueOf (httpAddress.getPort ())), String.format ("%s:%d", logbackAddress.getAddress ().getHostAddress (), Integer.valueOf (logbackAddress.getPort ())));
			createParameters = new String[] {String.format ("type=%s", URLEncoder.encode ("#mosaic-components:java-container", "UTF-8")), String.format ("configuration=%s", DefaultJsonCoder.defaultInstance.encodeToString (configuration)), "count=1"};
			final ClasspathExporter exporter = ClasspathExporter.create (httpAddress, Objects.firstNonNull (loader, ClassLoader.getSystemClassLoader ()), exceptions);
			final SimpleSocketServer appender = new SimpleSocketServer ((LoggerContext) LoggerFactory.getILoggerFactory (), logbackAddress.getPort ());
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
		final Thread main = Threading.createDaemonThread (threading, MosBasicComponentLauncher.class, "main", run);
		final String[] identifier = new String[] {null};
		{
			Threading.registerExitCallback (threading, MosBasicComponentLauncher.class, "exit-hook-3", new Runnable () {
				@Override
				public final void run ()
				{
					logger.debug ("stopping component...");
					if (identifier[0] != null)
						try {
							final URL stopUrl = new URL (controller, String.format ("/processes/stop?key=%s", identifier[0]));
							stopUrl.openStream ().close ();
						} catch (final Throwable exception) {
							exceptions.trace (ExceptionResolution.Ignored, exception);
							logger.error ("failed stopping component; ignoring!", exception);
						}
				}
			});
		}
		logger.debug ("starting main...");
		main.start ();
		Threading.sleep (MosBasicComponentLauncher.defaultCreateDelay);
		logger.debug ("creating component...");
		try {
			final URL createUrl = new URL (controller, String.format ("/processes/create?%s", Joiner.on ("&").join (Arrays.asList (createParameters))));
			logger.debug ("{}", createUrl.toString ());
			try {
				final InputStream createStream = createUrl.openStream ();
				final String createResponse = new String (ByteStreams.toByteArray (createStream), Charset.forName ("UTF-8"));
				try {
					final Map<?, ?> createResponseMap = (Map<?, ?>) DefaultJsonCoder.defaultInstance.decodeFromString (createResponse);
					Preconditions.checkState (Boolean.TRUE.equals (createResponseMap.get ("ok")));
					Preconditions.checkState (((List<?>) createResponseMap.get ("keys")).size () > 0);
					identifier[0] = (String) ((List<?>) createResponseMap.get ("keys")).get (0);
					Preconditions.checkNotNull (identifier);
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
		logger.info ("started: {}", identifier);
		main.join ();
	}
	
	public static final long defaultCreateDelay = 1000;
}
