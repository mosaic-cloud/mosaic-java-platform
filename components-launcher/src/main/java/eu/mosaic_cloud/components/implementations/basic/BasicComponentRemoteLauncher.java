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
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.classpath_exporter.ClasspathExporter;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.json.tools.DefaultJsonCoder;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SimpleSocketServer;


public final class BasicComponentRemoteLauncher
{
	private BasicComponentRemoteLauncher ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void launch (final String[] arguments, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
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
		BasicComponentRemoteLauncher.launch (controllerBaseUrl, exporterAddress, appenderAddress, componentCallbacks, componentConfiguration, classLoader, threading, exceptions);
	}
	
	public static final void launch (final URL controllerBaseUrl, final InetSocketAddress exporterAddress, final InetSocketAddress appenderAddress, final String componentCallbacks, final String componentConfiguration, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
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
		final Logger logger = (Logger) LoggerFactory.getLogger (BasicComponentRemoteLauncher.class);
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
		Threading.registerExitCallback (threading, BasicComponentRemoteLauncher.class, "exit-hook-2", new Runnable () {
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
		BasicComponentGenericLauncher.launch (run, controllerBaseUrl, controllerCreateUrlArguments, componentIdentifier, threading, exceptions);
	}
}
