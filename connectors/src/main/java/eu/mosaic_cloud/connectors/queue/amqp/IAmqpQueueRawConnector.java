
package eu.mosaic_cloud.connectors.queue.amqp;


import eu.mosaic_cloud.connectors.queue.IQueueConnector;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface IAmqpQueueRawConnector
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
	CallbackCompletion<Void> ack (final long delivery, final boolean multiple);
	
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
	CallbackCompletion<Void> bindQueue (final String exchange, final String queue, final String routingKey);
	
	/**
	 * Cancels a consumer.
	 * 
	 * @param consumer
	 *            a client- or server-generated consumer tag to establish
	 *            context
	 * @return <code>true</code> if consumer was canceled
	 */
	CallbackCompletion<Void> cancel (final String consumer);
	
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
	CallbackCompletion<Void> consume (final String queue, final String consumer, final boolean exclusive, final boolean autoAck, final IAmqpQueueRawConsumerCallback consumerCallback);
	
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
	CallbackCompletion<Void> declareExchange (final String name, final AmqpExchangeType type, final boolean durable, final boolean autoDelete, final boolean passive);
	
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
	CallbackCompletion<Void> declareQueue (final String queue, final boolean exclusive, final boolean durable, final boolean autoDelete, final boolean passive);
	
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
	CallbackCompletion<Void> get (final String queue, final boolean autoAck);
	
	/**
	 * Publishes a message.
	 * 
	 * @param message
	 *            the message, message properties and destination data
	 * @return <code>true</code> if message was published successfully
	 */
	CallbackCompletion<Void> publish (final AmqpOutboundMessage message);
}
