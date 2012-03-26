/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;

import eu.mosaic_cloud.cloudlets.core.GenericCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerCallback;
import eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueDeliveryToken;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;

public class AmqpQueueConsumerConnector<Context, Message, Extra>
        extends
        BaseAmqpQueueConnector<eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<Message>, IAmqpQueueConsumerConnectorCallback<Context, Message, Extra>, Context>
        implements IAmqpQueueConsumerConnector<Context, Message, Extra> {

    public static final class Callback<Message> implements
            IAmqpQueueConsumerCallback<Message> {

        private AmqpQueueConsumerConnector<?, Message, ?> connector = null; // NOPMD

        @Override
        public CallbackCompletion<Void> consume(
                final IAmqpQueueDeliveryToken delivery, final Message message) {
            return this.connector.consume(delivery, message);
        }
    }

    @SuppressWarnings("synthetic-access")
    public AmqpQueueConsumerConnector(
            final ICloudletController<?> cloudlet,
            final eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConsumerConnector<Message> connector,
            final IConfiguration configuration,
            final IAmqpQueueConsumerConnectorCallback<Context, Message, Extra> callback,
            final Context context, final Callback<Message> backingCallback) {
        super(cloudlet, connector, configuration, callback, context);
        backingCallback.connector = this;
        // FIXME
        this.initialize();
    }

    @Override
    public CallbackCompletion<Void> acknowledge(
            final IAmqpQueueDeliveryToken delivery) {
        return this.acknowledge(delivery, null);
    }

    @Override
    public CallbackCompletion<Void> acknowledge(
            final IAmqpQueueDeliveryToken delivery, final Extra extra) {
        final CallbackCompletion<Void> completion = this.connector
                .acknowledge(delivery);
        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {

                @SuppressWarnings("synthetic-access")
                @Override
                public CallbackCompletion<Void> completed(
                        final CallbackCompletion<?> completion_) {
                    assert (completion_ == completion); // NOPMD
                    CallbackCompletion<Void> result;
                    if (completion.getException() == null) {
                        result = AmqpQueueConsumerConnector.this.callback
                                .acknowledgeSucceeded(
                                        AmqpQueueConsumerConnector.this.context,
                                        new GenericCallbackCompletionArguments<Context, Extra>(
                                                AmqpQueueConsumerConnector.this.cloudlet,
                                                extra));
                    } else {
                        result = AmqpQueueConsumerConnector.this.callback
                                .acknowledgeFailed(
                                        AmqpQueueConsumerConnector.this.context,
                                        new GenericCallbackCompletionArguments<Context, Extra>(
                                                AmqpQueueConsumerConnector.this.cloudlet,
                                                completion.getException()));
                    }
                    return result;
                }
            });
        }
        return completion;
    }

    protected CallbackCompletion<Void> consume(
            final IAmqpQueueDeliveryToken delivery, final Message message) {
        CallbackCompletion<Void> result;
        if (this.callback == null) {
            result = CallbackCompletion
                    .createFailure(new IllegalStateException());
        } else {
            result = this.callback
                    .consume(
                            this.context,
                            new AmqpQueueConsumeCallbackArguments<Context, Message, Extra>(
                                    this.cloudlet, delivery, message));
        }
        return result;
    }
}
