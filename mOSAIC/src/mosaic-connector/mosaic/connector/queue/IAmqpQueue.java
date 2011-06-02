package mosaic.connector.queue;

import java.util.List;

import mosaic.connector.IResourceConnector;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.queue.AmqpExchangeType;

/**
 * Interface for working with AMQP compatible key-value stores.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IAmqpQueue extends IResourceConnector {

	/**
	 * Opens a new AMQP connection. If one already exists then the current one
	 * is closed and a new one is created.
	 * 
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if the connection was established
	 */
	public abstract IResult<Boolean> openConnection(
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

	/**
	 * Closes the connection with the AMQP queue system. It also closes all
	 * channels.
	 * 
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if the connection was closed
	 */
	public abstract IResult<Boolean> closeConnection(
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

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
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if the exchange declaration succeeded
	 */
	public abstract IResult<Boolean> declareExchange(final String name,
			final AmqpExchangeType type, final boolean durable,
			final boolean autoDelete, final boolean passive,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

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
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if the queue declaration succeeded
	 */
	public abstract IResult<Boolean> declareQueue(final String queue,
			final boolean exclusive, final boolean durable,
			final boolean autoDelete, final boolean passive,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

	/**
	 * Bind a queue to an exchange, with no extra arguments.
	 * 
	 * @param exchange
	 *            the name of the queue
	 * @param queue
	 *            the name of the exchange
	 * @param routingKey
	 *            the routing key to use for the binding
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if the queue bind succeeded
	 */
	public abstract IResult<Boolean> bindQueue(final String exchange,
			final String queue, final String routingKey,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

	/**
	 * Publishes a message.
	 * 
	 * @param message
	 *            the message, message properties and destination data
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if message was published successfully
	 */
	public abstract IResult<Boolean> publish(final AmqpOutboundMessage message,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

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
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @param consumerCallback
	 *            the consumer callback (this will called when the queuing
	 *            system will send Consume messages)
	 * @return the client-generated consumer tag to establish context
	 */
	public abstract IResult<String> consume(final String queue,
			final String consumer, final boolean exclusive,
			final boolean autoAck, final Object extra,
			List<IOperationCompletionHandler<String>> handlers,
			CompletionInvocationHandler<String> iHandler,
			final IAmqpConsumerCallback consumerCallback);

	/**
	 * Cancels a consumer.
	 * 
	 * @param consumer
	 *            a client- or server-generated consumer tag to establish
	 *            context
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if consumer was canceled
	 */
	public abstract IResult<Boolean> cancel(final String consumer,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

	/**
	 * Retrieve a message from a queue.
	 * 
	 * @param queue
	 *            the name of the queue
	 * @param autoAck
	 *            <code>true</code> if the server should consider messages
	 *            acknowledged once delivered; <code>false</code> if the server
	 *            should expect explicit acknowledgments
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if message was retrieved successfully
	 */
	public abstract IResult<Boolean> get(final String queue,
			final boolean autoAck,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

	/**
	 * Acknowledge one or several received messages.
	 * 
	 * @param delivery
	 *            the tag received with the messages
	 * @param multiple
	 *            <code>true</code> to acknowledge all messages up to and
	 *            including the supplied delivery tag; <code>false</code> to
	 *            acknowledge just the supplied delivery tag.
	 * @param handlers
	 *            handlers to be called when the operation finishes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return <code>true</code> if messages were acknowledged successfully
	 */
	public abstract IResult<Boolean> ack(final long delivery,
			final boolean multiple,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

}