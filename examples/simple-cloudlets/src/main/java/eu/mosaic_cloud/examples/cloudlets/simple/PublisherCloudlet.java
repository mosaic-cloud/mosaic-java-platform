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

import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueuePublishCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

public class PublisherCloudlet {

	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<PublisherCloudletContext> {

		@Override
		public CallbackCompletion<Void> initialize(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet is being initialized.");
			ICloudletController<PublisherCloudletContext> cloudlet = arguments
					.getCloudlet();
			IConfiguration configuration = cloudlet.getConfiguration();
			IConfiguration queueConfiguration = configuration
					.spliceConfiguration(ConfigurationIdentifier
							.resolveAbsolute("queue"));
			context.publisher = cloudlet.getConnectorFactory(IAmqpQueuePublisherConnectorFactory.class)
					.create(queueConfiguration, String.class,
							new PojoDataEncoder<String>(String.class),
							new AmqpPublisherCallback(), context);
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> initializeSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet initialized successfully.");
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroy(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet is being destroyed.");
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroySucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"Publisher cloudlet was destroyed successfully.");
			return ICallback.SUCCESS;
		}
	}

	public static final class AmqpPublisherCallback
			extends
			DefaultAmqpPublisherConnectorCallback<PublisherCloudletContext, String> {

		@Override
		public CallbackCompletion<Void> registerSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet publisher registered successfully.");
			context.publisher.publish("TEST MESSAGE!");
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> unregisterSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet publisher unregistered successfully.");
			// if unregistered as publisher is successful then destroy resource
			ICloudletController<PublisherCloudletContext> cloudlet = arguments
					.getCloudlet();
			context.publisher.destroy();
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> initializeSucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			// if resource initialized successfully then just register as a
			// publisher
			context.publisher.register();
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> destroySucceeded(PublisherCloudletContext context,
				CallbackArguments<PublisherCloudletContext> arguments) {
			this.logger.info(
					"PublisherCloudlet publisher was destroyed successfully.");
			context.publisher = null;
			arguments.getCloudlet().destroy();
			return ICallback.SUCCESS;
		}

		@Override
		public CallbackCompletion<Void> publishSucceeded(
				PublisherCloudletContext context,
				AmqpQueuePublishCallbackCompletionArguments<PublisherCloudletContext, String> arguments) {
			context.publisher.unregister();
			return ICallback.SUCCESS;
		}

	}

	public static final class PublisherCloudletContext {

		IAmqpQueuePublisherConnector<PublisherCloudletContext, String> publisher;
	}
}
