/*
 * #%L
 * mosaic-components-container
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


import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessMain.ArgumentsProvider;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.json.tools.DefaultJsonCoder;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;


public final class BasicComponentLocalLauncher
{
	private BasicComponentLocalLauncher ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void launch (final String[] arguments, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
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
		BasicComponentLocalLauncher.launch (controllerBaseUrl, channelAddress, componentCallbacks, Arrays.asList (componentConfiguration), classLoader, threading, exceptions);
	}
	
	public static final void launch (final URL controllerBaseUrl, final InetSocketAddress channelAddress, final String componentCallbacks, final List<String> componentConfiguration)
			throws Throwable
	{
		Preconditions.checkNotNull (controllerBaseUrl);
		Preconditions.checkNotNull (channelAddress);
		Preconditions.checkNotNull (componentCallbacks);
		Preconditions.checkNotNull (componentConfiguration);
		BasicThreadingSecurityManager.initialize ();
		final BaseExceptionTracer exceptions = AbortingExceptionTracer.defaultInstance;
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader ();
		final BasicThreadingContext threading = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions, exceptions.catcher, classLoader);
		threading.initialize ();
		BasicComponentLocalLauncher.launch (controllerBaseUrl, channelAddress, componentCallbacks, componentConfiguration, classLoader, threading, exceptions);
		threading.destroy ();
	}
	
	public static final void launch (final URL controllerBaseUrl, final InetSocketAddress channelAddress, final String componentCallbacks, final List<String> componentConfiguration, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
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
		final String[] componentCallbacksClassNameParts = componentCallbacks.split ("\\.");
		final String[] controllerCreateUrlArguments;
		{
			// FIXME: At some point in time remove the old variant...
			final boolean useDeprecated = true;
			if (!useDeprecated) {
				final String controllerCreateTypeEncoded = URLEncoder.encode ("#mosaic-tests:socat", "UTF-8");
				final String controllerCreateConfigurationEncoded = URLEncoder.encode (DefaultJsonCoder.defaultInstance.encodeToString (channelAddressEncoded), "UTF-8");
				final String controllerCreateAnnotationEncoded = URLEncoder.encode (DefaultJsonCoder.defaultInstance.encodeToString (componentCallbacksClassNameParts[componentCallbacksClassNameParts.length - 1]), "UTF-8");
				controllerCreateUrlArguments = new String[] {String.format ("type=%s", controllerCreateTypeEncoded), String.format ("configuration=%s", controllerCreateConfigurationEncoded), String.format ("annotation=%s", controllerCreateAnnotationEncoded), "count=1"};
			} else {
				final String controllerCreateTypeEncoded = URLEncoder.encode ("#mosaic-tests:socat", "UTF-8");
				final List<String> controllerCreateConfiguration = Arrays.asList (componentCallbacksClassNameParts[componentCallbacksClassNameParts.length - 1], channelAddressEncoded);
				final String controllerCreateConfigurationEncoded = URLEncoder.encode (DefaultJsonCoder.defaultInstance.encodeToString (controllerCreateConfiguration), "UTF-8");
				controllerCreateUrlArguments = new String[] {String.format ("type=%s", controllerCreateTypeEncoded), String.format ("configuration=%s", controllerCreateConfigurationEncoded), "count=1"};
			}
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
		BasicComponentGenericLauncher.launch (run, controllerBaseUrl, controllerCreateUrlArguments, componentIdentifier, threading, exceptions);
	}
}
