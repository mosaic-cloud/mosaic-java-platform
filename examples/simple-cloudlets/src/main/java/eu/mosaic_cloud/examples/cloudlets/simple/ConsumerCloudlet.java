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

import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.AmqpQueueConsumeCallbackArguments;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import org.junit.Ignore;

public class ConsumerCloudlet {

    public static final class AmqpConsumerCallback extends
            DefaultAmqpQueueConsumerConnectorCallback<ConsumerCloudletContext, String, Void> {

        @Override
        public CallbackCompletion<Void> acknowledgeSucceeded(ConsumerCloudletContext context,
                GenericCallbackCompletionArguments<ConsumerCloudletContext, Void> arguments) {
            context.consumer.destroy();
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> consume(ConsumerCloudletContext context,
                AmqpQueueConsumeCallbackArguments<ConsumerCloudletContext, String, Void> arguments) {
            final String data = arguments.getMessage();
            this.logger.info("ConsumerCloudlet received logging message for user " + data);
            context.consumer.acknowledge(arguments.getDelivery());
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> destroySucceeded(ConsumerCloudletContext context,
                CallbackArguments<ConsumerCloudletContext> arguments) {
            this.logger.info("ConsumerCloudlet consumer was destroyed successfully.");
            context.consumer = null;
            arguments.getCloudlet().destroy();
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(ConsumerCloudletContext context,
                CallbackArguments<ConsumerCloudletContext> arguments) {
            this.logger.info("ConsumerCloudlet consumer initialized successfully.");
            return ICallback.SUCCESS;
        }
    }

    public static final class ConsumerCloudletContext {

        IAmqpQueueConsumerConnector<ConsumerCloudletContext, String, Void> consumer;
    }

    public static final class LifeCycleHandler extends
            DefaultCloudletCallback<ConsumerCloudletContext> {

        @Override
        public CallbackCompletion<Void> destroy(ConsumerCloudletContext context,
                CloudletCallbackArguments<ConsumerCloudletContext> arguments) {
            this.logger.info("ConsumerCloudlet is being destroyed.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> destroySucceeded(ConsumerCloudletContext context,
                CloudletCallbackCompletionArguments<ConsumerCloudletContext> arguments) {
            this.logger.info("Consumer cloudlet was destroyed successfully.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initialize(ConsumerCloudletContext context,
                CloudletCallbackArguments<ConsumerCloudletContext> arguments) {
            this.logger.info("ConsumerCloudlet is being initialized.");
            final ICloudletController<ConsumerCloudletContext> cloudlet = arguments.getCloudlet();
            final IConfiguration configuration = cloudlet.getConfiguration();
            final IConfiguration queueConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("queue"));
            final DataEncoder<String> encoder = new PojoDataEncoder<String>(String.class);
            context.consumer = cloudlet.getConnectorFactory(
                    IAmqpQueueConsumerConnectorFactory.class).create(queueConfiguration,
                    String.class, encoder, new AmqpConsumerCallback(), context);
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(ConsumerCloudletContext context,
                CloudletCallbackCompletionArguments<ConsumerCloudletContext> arguments) {
            this.logger.info("ConsumerCloudlet initialized successfully.");
            return ICallback.SUCCESS;
        }
    }
}
