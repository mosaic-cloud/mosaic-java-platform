/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.core;


import java.util.MissingResourceException;
import java.util.ResourceBundle;

import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;


public final class ConfigProperties
{
	private ConfigProperties ()
	{}
	
	public static String getString (final String key)
	{
		try {
			return ConfigProperties.RESOURCE_BUNDLE.getString (key);
		} catch (final MissingResourceException exception) {
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (exception, "failed resolving the config-property `%s`...", key);
			return '!' + key + '!';
		}
	}
	
	public static final boolean IN_DEBUGGING = java.lang.management.ManagementFactory.getRuntimeMXBean ().getInputArguments ().toString ().indexOf ("-agentlib:jdwp") > 0;
	private static final String BUNDLE_NAME = "eu.mosaic_cloud.connectors.config"; // $NON-NLS-1$
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle (ConfigProperties.BUNDLE_NAME);
}
