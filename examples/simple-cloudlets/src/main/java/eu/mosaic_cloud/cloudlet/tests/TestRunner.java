/*
 * #%L
 * mosaic-examples-simple-cloudlets
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
package eu.mosaic_cloud.cloudlet.tests;

import eu.mosaic_cloud.cloudlet.core.CloudletException;
import eu.mosaic_cloud.cloudlet.runtime.CloudletManager;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;

public class TestRunner {

	public static CloudletManager runHelloWorld() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "hello-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runSimpleQueueConsumer() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "consumer-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runSimpleQueuePublisher() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "publisher-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runLoggingCloudlet() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "logging-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runUserCloudlet() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "user-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	private static CloudletManager startCloudlet(IConfiguration configuration) {
		final CloudletManager container = new CloudletManager(
				TestRunner.class.getClassLoader(), configuration);

		try {
			container.start();
		} catch (CloudletException e) {
			ExceptionTracer.traceIgnored(e);
		}
		return container;
	}

}
