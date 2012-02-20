/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.queue.amqp;


import eu.mosaic_cloud.connectors.queue.IQueueConnector;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpExchangeType;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


/**
 * Interface for working with AMQP compatible queueing systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IAmqpQueueConnector
		extends
			IQueueConnector
{
	/**
	 * Acknowledge one or several received messages.
	 * 
	 * @param delivery
	 *            the tag received with the messages
	 * @param multiple
	 *            <code>true</code> to acknowledge all messages up to and
	 *            including the supplied delivery tag; <code>false</code> to
	 *            acknowledge just the supplied delivery tag.
	 * @return <code>true</code> if messages were acknowledged successfully
	 */
	CallbackCompletion<Boolean> ack (final long delivery, final boolean multiple);
	
	/**
	 * Bind a queue to an exchange, with no extra arguments.
	 * 
	 * @param exchange
	 *            the name of the queue
	 * @param queue
	 *            the name of the exchange
	 * @param routingKey
	 *            the routing key to use for the binding
	 * @return <code>true</code> if the queue bind succeeded
	 */
	CallbackCompletion<Boolean> bindQueue (final String exchange, final String queue, final String routingKey);
	
	/**
	 * Cancels a consumer.
	 * 
	 * @param consumer
	 *            a client- or server-generated consumer tag to establish
	 *            context
	 * @return <code>true</code> if consumer was canceled
	 */
	CallbackCompletion<Boolean> cancel (final String consumer);
	
	/**
	 * Start a message consumer.
	 * 
	 * @param queue
	 *            the name of the queue
	 * @param consumer
	 *            a client-generated consumer tag to establish context
	 * @param exclusive
	 *            <code>true</code> if this is an exclusive consumer
	 * @param autoAck
	 *            <code>true</code> if the server should consider messages
	 *            acknowledged once delivered; false if the server should expect
	 *            explicit acknowledgments
	 * @param extra
	 * @param consumerCallback
	 *            the consumer callback (this will called when the queuing
	 *            system will send Consume messages)
	 * @return the client-generated consumer tag to establish context
	 */
	CallbackCompletion<Boolean> consume (final String queue, final String consumer, final boolean exclusive, final boolean autoAck, final Object extra, final IAmqpQueueConsumerCallback consumerCallback);
	
	/**
	 * Declares an exchange and creates a channel for it.
	 * 
	 * @param name
	 *            the name of the exchange
	 * @param type
	 *            the exchange type
	 * @param durable
	 *            <code>true</code> if we are declaring a durable exchange (the
	 *            exchange will survive a server restart)
	 * @param autoDelete
	 *            <code>true</code> if the server should delete the exchange
	 *            when it is no longer in use
	 * @param passive
	 *            <code>true</code> if we declare an exchange passively; that
	 *            is, check if the named exchange exists
	 * @return <code>true</code> if the exchange declaration succeeded
	 */
	CallbackCompletion<Boolean> declareExchange (final String name, final AmqpExchangeType type, final boolean durable, final boolean autoDelete, final boolean passive);
	
	/**
	 * Declare a queue.
	 * 
	 * @param queue
	 *            the name of the queue
	 * @param exclusive
	 *            <code>true</code> if we are declaring an exclusive queue
	 *            (restricted to this connection)
	 * @param durable
	 *            <code>true</code> if we are declaring a durable queue (the
	 *            queue will survive a server restart)
	 * @param autoDelete
	 *            <code>true</code> if we are declaring an autodelete queue
	 *            (server will delete it when no longer in use)
	 * @param passive
	 *            <code>true</code> if we declare a queue passively; i.e., check
	 *            if it exists
	 * @return <code>true</code> if the queue declaration succeeded
	 */
	CallbackCompletion<Boolean> declareQueue (final String queue, final boolean exclusive, final boolean durable, final boolean autoDelete, final boolean passive);
	
	/**
	 * Retrieve a message from a queue.
	 * 
	 * @param queue
	 *            the name of the queue
	 * @param autoAck
	 *            <code>true</code> if the server should consider messages
	 *            acknowledged once delivered; <code>false</code> if the server
	 *            should expect explicit acknowledgments
	 * @return <code>true</code> if message was retrieved successfully
	 */
	CallbackCompletion<Boolean> get (final String queue, final boolean autoAck);
	
	/**
	 * Publishes a message.
	 * 
	 * @param message
	 *            the message, message properties and destination data
	 * @return <code>true</code> if message was published successfully
	 */
	CallbackCompletion<Boolean> publish (final AmqpOutboundMessage message);
}
