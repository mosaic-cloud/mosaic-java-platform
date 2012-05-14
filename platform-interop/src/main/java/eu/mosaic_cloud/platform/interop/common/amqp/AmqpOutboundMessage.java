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
 * This class defines an outbound message and all information required to
 * publish it.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpOutboundMessage
		implements
			IAmqpMessage
{
	/**
	 * @param exchange
	 *            the exchange to publish the message to
	 * @param routingKey
	 *            the routing key
	 * @param data
	 *            the message body
	 * @param mandatory
	 *            <code>true</code> if we are requesting a mandatory publish
	 * @param immediate
	 *            <code>true</code> if we are requesting an immediate publish
	 * @param durable
	 *            <code>true</code> if delivery mode should be 2
	 * @param contentType
	 *            the RFC-2046 MIME type for the Message content (such as
	 *            "text/plain")
	 */
	public AmqpOutboundMessage (final String exchange, final String routingKey, final byte[] data, final boolean mandatory, final boolean immediate, final boolean durable, final String contentType)
	{
		this (exchange, routingKey, data, mandatory, immediate, durable, null, null, contentType, null, null);
	}
	
	/**
	 * Constructs a message.
	 * 
	 * @param exchange
	 *            the exchange to publish the message to
	 * @param routingKey
	 *            the routing key
	 * @param data
	 *            the message body
	 * @param mandatory
	 *            <code>true</code> if we are requesting a mandatory publish
	 * @param immediate
	 *            <code>true</code> if we are requesting an immediate publish
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
	public AmqpOutboundMessage (final String exchange, final String routingKey, final byte[] data, final boolean mandatory, final boolean immediate, final boolean durable, final String callback, final String contentEncoding, final String contentType, final String correlation, final String identifier)
	{
		super ();
		this.callback = callback;
		this.contentEncoding = contentEncoding;
		this.contentType = contentType;
		this.correlation = correlation;
		this.data = data;
		this.durable = durable;
		this.exchange = exchange;
		this.identifier = identifier;
		this.immediate = immediate;
		this.mandatory = mandatory;
		this.routingKey = routingKey;
	}
	
	/**
	 * @param exchange
	 *            the exchange to publish the message to
	 * @param routingKey
	 *            the routing key
	 * @param data
	 *            the message body
	 */
	public AmqpOutboundMessage (final String exchange, final String routingKey, final byte[] data, final String contentType)
	{
		this (exchange, routingKey, data, false, false, false, null, null, contentType, null, null);
	}
	
	public String getCallback ()
	{
		return this.callback;
	}
	
	public String getContentEncoding ()
	{
		return this.contentEncoding;
	}
	
	public String getContentType ()
	{
		return this.contentType;
	}
	
	public String getCorrelation ()
	{
		return this.correlation;
	}
	
	public byte[] getData ()
	{
		return this.data;
	}
	
	public String getExchange ()
	{
		return this.exchange;
	}
	
	public String getIdentifier ()
	{
		return this.identifier;
	}
	
	public String getRoutingKey ()
	{
		return this.routingKey;
	}
	
	public boolean isDurable ()
	{
		return this.durable;
	}
	
	public boolean isImmediate ()
	{
		return this.immediate;
	}
	
	public boolean isMandatory ()
	{
		return this.mandatory;
	}
	
	private final String callback;
	private final String contentEncoding;
	private final String contentType;
	private final String correlation;
	private final byte[] data;
	private final boolean durable;
	private final String exchange;
	private final String identifier;
	private final boolean immediate;
	private final boolean mandatory;
	private final String routingKey;
}
