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


import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.json.tools.DefaultJsonCoder;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

import ch.qos.logback.classic.Logger;


public final class BasicComponentGenericLauncher
{
	private BasicComponentGenericLauncher () {
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void launch (final Runnable run, final URL controllerBaseUrl, final String[] controllerCreateUrlArguments, final AtomicReference<String> componentIdentifier, final ThreadingContext threading, final ExceptionTracer exceptions)
				throws Throwable {
		Preconditions.checkNotNull (run);
		Preconditions.checkNotNull (controllerBaseUrl);
		Preconditions.checkNotNull (controllerCreateUrlArguments);
		Preconditions.checkNotNull (componentIdentifier);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final Logger logger = (Logger) LoggerFactory.getLogger (BasicComponentGenericLauncher.class);
		Threading.registerExitCallback (threading, BasicComponentGenericLauncher.class, "exit-hook", new Runnable () {
			@Override
			public final void run () {
				logger.debug ("starting safety hook...");
				Threading.sleep (AbortingExceptionTracer.defaulExitTimeout);
				Threading.halt ();
			}
		});
		Threading.registerExitCallback (threading, BasicComponentGenericLauncher.class, "exit-hook-3", new Runnable () {
			@Override
			public final void run () {
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
		final Thread main = Threading.createDaemonThread (threading, BasicComponentGenericLauncher.class, "main", run);
		main.start ();
		Threading.sleep (BasicComponentGenericLauncher.defaultCreateDelay);
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
