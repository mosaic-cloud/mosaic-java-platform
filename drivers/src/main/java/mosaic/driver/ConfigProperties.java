/*
 * #%L
 * mosaic-driver
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.driver;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import mosaic.core.exceptions.ExceptionTracer;

public final class ConfigProperties {
	private static final String BUNDLE_NAME = "mosaic.driver.config"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(ConfigProperties.BUNDLE_NAME);

	private ConfigProperties() {
	}

	public static String getString(String key) {
		String retString;
		try {
			retString = ConfigProperties.RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			ExceptionTracer.traceIgnored(e);
			retString = '!' + key + '!';
		}
		return retString;
	}
}
