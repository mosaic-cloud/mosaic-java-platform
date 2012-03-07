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
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

public class UserCloudlet {

    public static final class AmqpConsumerCallback
            extends
            DefaultAmqpQueueConsumerConnectorCallback<UserCloudletContext, AuthenticationToken, Void> {

        @Override
        public CallbackCompletion<Void> acknowledgeSucceeded(UserCloudletContext context,
                GenericCallbackCompletionArguments<UserCloudletContext, Void> arguments) {
            context.consumer.destroy();
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> consume(
                UserCloudletContext context,
                AmqpQueueConsumeCallbackArguments<UserCloudletContext, AuthenticationToken, Void> arguments) {
            final AuthenticationToken data = arguments.getMessage();
            final String token = data.getToken();
            if (token != null) {
                this.logger.info("UserCloudlet received authentication token: " + token);
            } else {
                this.logger.error("UserCloudlet did not receive authentication token.");
            }
            context.consumer.acknowledge(arguments.getDelivery());
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> destroySucceeded(UserCloudletContext context,
                CallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet consumer was destroyed successfully.");
            context.consumerRunning = false;
            context.consumer = null;
            if (context.publisher == null) {
                arguments.getCloudlet().destroy();
            }
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(UserCloudletContext context,
                CallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet consumer initialized successfully.");
            context.consumerRunning = true;
            return ICallback.SUCCESS;
        }
    }

    public static final class AmqpPublisherCallback extends
            DefaultAmqpPublisherConnectorCallback<UserCloudletContext, LoggingData, Void> {

        @Override
        public CallbackCompletion<Void> destroySucceeded(UserCloudletContext context,
                CallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet publisher was destroyed successfully.");
            context.publisherRunning = false;
            context.publisher = null;
            if (context.consumer == null) {
                arguments.getCloudlet().destroy();
            }
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(UserCloudletContext context,
                CallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet publisher initialized successfully.");
            context.publisherRunning = true;
            final String user = ConfigUtils.resolveParameter(arguments.getCloudlet()
                    .getConfiguration(), "test.user", String.class, "error");
            final String pass = ConfigUtils.resolveParameter(arguments.getCloudlet()
                    .getConfiguration(), "test.password", String.class, "");
            final LoggingData data = new LoggingData(user, pass);
            context.publisher.publish(data, null);
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> publishSucceeded(UserCloudletContext context,
                GenericCallbackCompletionArguments<UserCloudletContext, Void> arguments) {
            context.publisher.destroy();
            return ICallback.SUCCESS;
        }
    }

    public static final class LifeCycleHandler extends DefaultCloudletCallback<UserCloudletContext> {

        @Override
        public CallbackCompletion<Void> destroy(UserCloudletContext context,
                CloudletCallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet is being destroyed.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> destroySucceeded(UserCloudletContext context,
                CloudletCallbackCompletionArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet was destroyed successfully.");
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initialize(UserCloudletContext context,
                CloudletCallbackArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet is being initialized.");
            final ICloudletController<UserCloudletContext> cloudlet = arguments.getCloudlet();
            final IConfiguration configuration = cloudlet.getConfiguration();
            final IConfiguration queueConfiguration = configuration
                    .spliceConfiguration(ConfigurationIdentifier.resolveAbsolute("queue"));
            context.consumer = cloudlet.getConnectorFactory(
                    IAmqpQueueConsumerConnectorFactory.class).create(queueConfiguration,
                    AuthenticationToken.class,
                    new PojoDataEncoder<AuthenticationToken>(AuthenticationToken.class),
                    new AmqpConsumerCallback(), context);
            context.publisher = cloudlet.getConnectorFactory(
                    IAmqpQueuePublisherConnectorFactory.class).create(queueConfiguration,
                    LoggingData.class, new PojoDataEncoder<LoggingData>(LoggingData.class),
                    new AmqpPublisherCallback(), context);
            return ICallback.SUCCESS;
        }

        @Override
        public CallbackCompletion<Void> initializeSucceeded(UserCloudletContext context,
                CloudletCallbackCompletionArguments<UserCloudletContext> arguments) {
            this.logger.info("UserCloudlet initialized successfully.");
            return ICallback.SUCCESS;
        }
    }

    public static final class UserCloudletContext {

        IAmqpQueueConsumerConnector<UserCloudletContext, AuthenticationToken, Void> consumer;
        IAmqpQueuePublisherConnector<UserCloudletContext, LoggingData, Void> publisher;
        boolean publisherRunning = false;
        boolean consumerRunning = false;
    }
}
