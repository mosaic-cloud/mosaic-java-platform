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
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.connectors.queue.amqp.IAmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpPublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultAmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;
import eu.mosaic_cloud.platform.core.utils.SerializedDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

public class UserCloudlet {

    public static final class AmqpConsumerCallback
            extends
            DefaultAmqpQueueConsumerConnectorCallback<UserCloudletContext, AuthenticationToken, Void> {

        @Override
        public CallbackCompletion<Void> acknowledgeSucceeded(final UserCloudletContext context,
                final GenericCallbackCompletionArguments<Void> arguments) {
            context.cloudlet.destroy();
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> consume(final UserCloudletContext context,
                final AmqpQueueConsumeCallbackArguments<AuthenticationToken> arguments) {
            final AuthenticationToken data = arguments.getMessage();
            final String token = data.getToken();
            if (token != null) {
                this.logger.info("UserCloudlet received authentication token `{}`.", token);
            } else {
                this.logger.error("UserCloudlet did not receive authentication token.");
            }
            context.consumer.acknowledge(arguments.getToken());
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> destroySucceeded(final UserCloudletContext context,
                final CallbackArguments arguments) {
            this.logger.info("UserCloudlet consumer destroyed successfully.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(final UserCloudletContext context,
                final CallbackArguments arguments) {
            this.logger.info("UserCloudlet consumer initialized successfully.");
            return ICallback.SUCCESS;
        }
    }

    public static final class AmqpPublisherCallback
            extends
            DefaultAmqpPublisherConnectorCallback<UserCloudletContext, LoggingData, MessageEnvelope> {

        @Override
        public CallbackCompletion<Void> destroySucceeded(final UserCloudletContext context,
                final CallbackArguments arguments) {
            this.logger.info("UserCloudlet publisher destroyed successfully.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(final UserCloudletContext context,
                final CallbackArguments arguments) {
            this.logger.info("UserCloudlet publisher initialized successfully.");
            return ICallback.SUCCESS;
        }
    }

    public static final class LifeCycleHandler extends DefaultCloudletCallback<UserCloudletContext> {

        @Override
        public CallbackCompletion<Void> destroy(final UserCloudletContext context,
                final CloudletCallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet destroying...");
            return CallbackCompletion.createAndChained(context.consumer.destroy(),
                    context.publisher.destroy());
        }

        @Override
        public CallbackCompletion<Void> destroySucceeded(final UserCloudletContext context,
                final CloudletCallbackCompletionArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet destroyed successfully.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initialize(final UserCloudletContext context,
                final CloudletCallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet initializing...");
            context.cloudlet = arguments.getCloudlet();
            final IConfiguration configuration = context.cloudlet.getConfiguration();
            final IConfiguration queueConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("queue"));
            context.consumer = context.cloudlet.getConnectorFactory(
                    IAmqpQueueConsumerConnectorFactory.class).create(queueConfiguration,
                    AuthenticationToken.class,
                    SerializedDataEncoder.create(AuthenticationToken.class),
                    new AmqpConsumerCallback(), context);
            context.publisher = context.cloudlet.getConnectorFactory(
                    IAmqpQueuePublisherConnectorFactory.class).create(queueConfiguration,
                    LoggingData.class, SerializedDataEncoder.create(LoggingData.class),
                    new AmqpPublisherCallback(), context);
            return CallbackCompletion.createAndChained(context.consumer.initialize(),
                    context.publisher.initialize());
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(final UserCloudletContext context,
                final CloudletCallbackCompletionArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet initialized successfully.");
            final String user = ConfigUtils.resolveParameter(arguments.getCloudlet()
                    .getConfiguration(), "test.user", String.class, "error");
            final String pass = ConfigUtils.resolveParameter(arguments.getCloudlet()
                    .getConfiguration(), "test.password", String.class, "");
            final LoggingData data = new LoggingData(user, pass);
            context.publisher.publish(data, null);
            return ICallback.SUCCESS;
        }
    }

    public static final class UserCloudletContext {

        ICloudletController<UserCloudletContext> cloudlet;
        IAmqpQueueConsumerConnector<AuthenticationToken, Void> consumer;
        IAmqpQueuePublisherConnector<LoggingData, MessageEnvelope> publisher;
    }
}
