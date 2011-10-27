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

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.MosBasicComponentLauncher;

/**
 * Launches a driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class DriverLauncher {

	private DriverLauncher() {
		super();
		throw new UnsupportedOperationException();
	}

	public static void main(final String[] arguments) throws Throwable {
		Preconditions
				.checkArgument(
						(arguments != null) && (arguments.length == 4),
						"invalid arguments: expected <ip> <mos-url> <resource type: amqp | kv | memcached> <port>");
		String clasz = DriverCallbackType.valueOf(arguments[2].toUpperCase())
				.getCallbackClass();
		String port = Integer.toString(Integer.parseInt(arguments[3]) + 1);
		MosBasicComponentLauncher.main(new String[] { clasz, arguments[0],
				arguments[3], port, arguments[1] }, Thread.currentThread()
				.getContextClassLoader());
	}
}
