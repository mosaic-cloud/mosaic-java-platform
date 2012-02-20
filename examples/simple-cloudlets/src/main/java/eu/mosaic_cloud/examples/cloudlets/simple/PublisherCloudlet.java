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
package eu.mosaic_cloud.examples.cloudlets.simple;

import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherCallback;

import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;

import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.resources.amqp.AmqpQueuePublishCallbackArguments;
import eu.mosaic_cloud.cloudlets.resources.amqp.AmqpQueuePublisher;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;

public class PublisherCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<PublisherCloudletContext> {

		@Override
		public void initialize(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet is being initialized.");
			ICloudletController<PublisherCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.publisher = new AmqpQueuePublisher<PublisherCloudlet.PublisherCloudletContext, String>(
					queueConfiguration, cloudlet, String.class,
					new PojoDataEncoder<String>(String.class));

		}

		@Override
		public void initializeSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet initialized successfully.");
			ICloudletController<PublisherCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.initializeResource(context.publisher,
					new AmqpPublisherCallback(), context);

		}

		@Override
		public void destroy(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet is being destroyed.");
		}

		@Override
		public void destroySucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"Publisher cloudlet was destroyed successfully.");
		}
	}

	public static final class AmqpPublisherCallback
			extends
			DefaultAmqpPublisherCallback<PublisherCloudletContext, AuthenticationToken> {

		@Override
		public void registerSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet publisher registered successfully.");
			context.publisher.publish("TEST MESSAGE!", null, "text/plain");
		}

		@Override
		public void unregisterSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<PublisherCloudletContext> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroyResource(context.publisher, this);
		}

		@Override
		public void initializeSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			context.publisher.register();
		}

		@Override
		public void destroySucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet publisher was destroyed successfully.");
			context.publisher = null;
			arguments.getCloudlet().destroy();
		}

		@Override
		public void publishSucceeded(
				PublisherCloudletContext context,
				AmqpQueuePublishCallbackArguments<PublisherCloudletContext, AuthenticationToken> arguments) {
			context.publisher.unregister();
		}

	}

	public static final class PublisherCloudletContext {

		AmqpQueuePublisher<PublisherCloudletContext, String> publisher;
	}
}
