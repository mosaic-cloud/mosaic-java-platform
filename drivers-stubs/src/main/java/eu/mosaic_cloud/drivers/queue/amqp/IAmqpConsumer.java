/*
 * #%L
 * mosaic-drivers
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

package eu.mosaic_cloud.drivers.queue.amqp;

import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;

/**
 * Interface for application callback objects to receive notifications and
 * messages from a queue by subscription. Methods of this interface are invoked
 * inside the driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IAmqpConsumer {

    /**
     * Handles the Cancel message. Called when the consumer is cancelled for
     * reasons other than by a basicCancel.
     * 
     * @param consumerTag
     *            the consumer identifier
     */
    void handleCancel(String consumerTag);

    /**
     * Handles the Cancel OK message.
     * 
     * @param consumerTag
     *            the consumer identifier
     */
    void handleCancelOk(String consumerTag);

    /**
     * Handles the Consume OK message.
     * 
     * @param consumerTag
     *            the consumer identifier
     */
    void handleConsumeOk(String consumerTag);

    /**
     * Handles a delivered message.
     * 
     * @param message
     *            the message and all its properties
     */
    void handleDelivery(AmqpInboundMessage message);

    /**
     * Handles the shutdown signals.
     * 
     * @param consumerTag
     *            the consumer identifier
     * @param signalMessage
     *            the signal message
     */
    void handleShutdown(String consumerTag, String signalMessage);
}
