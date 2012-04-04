/*
 * #%L
 * mosaic-platform-interop
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

package eu.mosaic_cloud.platform.interop.common.amqp;

/**
 * This class defines an inbound message and all information required to consume
 * it.
 * 
 * @author Georgiana Macariu
 * 
 */
@SuppressWarnings("PMD")
public class AmqpInboundMessage implements IAmqpMessage {

    private final String callback;
    private final String consumer;
    private final String contentEncoding;
    private final String contentType;
    private final String correlation;
    private final byte[] data;
    private final long delivery;
    private final boolean durable;
    private final String exchange;
    private final String identifier;
    private final String routingKey;

    /**
     * Constructs a inbound message.
     * 
     * @param consumer
     *            a client-generated consumer tag to establish context
     * @param delivery
     *            delivery mode
     * @param exchange
     *            the exchange to publish the message to
     * @param routingKey
     *            the routing key
     * @param data
     *            the message body
     * @param durable
     *            <code>true</code> if delivery mode should be 2
     * @param contentType
     *            the type of the content
     */
    public AmqpInboundMessage(String consumer, long delivery, String exchange, String routingKey,
            byte[] data, boolean durable, String contentType) {
        super();
        this.consumer = consumer;
        this.delivery = delivery;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.identifier = null;
        this.correlation = null;
        this.callback = null;
        this.contentType = contentType;
        this.contentEncoding = null;
        this.data = data;
        this.durable = durable;
    }

    /**
     * Constructs a inbound message.
     * 
     * @param consumer
     *            a client-generated consumer tag to establish context
     * @param delivery
     *            delivery mode
     * @param exchange
     *            the exchange to publish the message to
     * @param routingKey
     *            the routing key
     * @param data
     *            the message body
     * @param durable
     *            <code>true</code> if delivery mode should be 2
     * @param callback
     *            the address of the Node to send replies to
     * @param contentEncoding
     * @param contentType
     *            the RFC-2046 MIME type for the Message content (such as
     *            "text/plain")
     * @param correlation
     *            this is a client-specific id that may be used to mark or
     *            identify Messages between clients. The server ignores this
     *            field.
     * @param identifier
     *            message-id is an optional property which uniquely identifies a
     *            Message within the Message system. The Message publisher is
     *            usually responsible for setting the message-id in such a way
     *            that it is assured to be globally unique. The server MAY
     *            discard a Message as a duplicate if the value of the
     *            message-id matches that of a previously received Message sent
     *            to the same Node.
     * 
     */
    public AmqpInboundMessage(String consumer, long delivery, String exchange, String routingKey,
            byte[] data, boolean durable, String callback, String contentEncoding,
            String contentType, String correlation, String identifier) {
        super();
        this.consumer = consumer;
        this.delivery = delivery;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.identifier = identifier;
        this.correlation = correlation;
        this.callback = callback;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.data = data;
        this.durable = durable;
    }

    public String getCallback() {
        return this.callback;
    }

    public String getConsumer() {
        return this.consumer;
    }

    public String getContentEncoding() {
        return this.contentEncoding;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getCorrelation() {
        return this.correlation;
    }

    public byte[] getData() {
        return this.data;
    }

    public long getDelivery() {
        return this.delivery;
    }

    public String getExchange() {
        return this.exchange;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getRoutingKey() {
        return this.routingKey;
    }

    public boolean isDurable() {
        return this.durable;
    }
}
