package mosaic.connector.queue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mosaic.connector.ConfigProperties;
import mosaic.connector.interop.AmqpProxy;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.EventDrivenOperation;
import mosaic.core.ops.EventDrivenResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.queue.AmqpExchangeType;

/**
 * Connector for queuing systems implementing the AMQP protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpConnector implements IAmqpQueue {

	private AmqpProxy proxy;
	private ExecutorService executor;

	/**
	 * Creates a new AMQP connector.
	 * 
	 * @param proxy
	 *            the proxy for the connector
	 * @param noThreads
	 *            the number of threads to be used for processing requests
	 */
	private AmqpConnector(AmqpProxy proxy, int noThreads) {
		this.proxy = proxy;
		this.executor = Executors.newFixedThreadPool(noThreads);
	}

	/**
	 * Returns an AMQP connector. For AMQP it should always return a new
	 * connector.
	 * 
	 * @param config
	 *            the configuration parameters required by the connector. This
	 *            should also include configuration settings for the
	 *            corresponding driver.
	 * @return the connector
	 * @throws IOException
	 */
	public static synchronized AmqpConnector create(IConfiguration config)
			throws IOException {
		int noThreads = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AmqpConnector.0"), Integer.class, 1); //$NON-NLS-1$
		AmqpProxy proxy = AmqpProxy.create(config);
		return new AmqpConnector(proxy, noThreads);
	}

	@Override
	public void destroy() {
		// FIXME wait for running operations to complete
		proxy.destroy();
		executor.shutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#openConnection(java.util.List,
	 * mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> openConnection(
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.openConnection(op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#closeConnection(java.util.List,
	 * mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> closeConnection(
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.closeConnection(op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#declareExchange(java.lang.String,
	 * mosaic.driver.queue.AmqpExchangeType, boolean, boolean, boolean,
	 * java.util.List, mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> declareExchange(final String name,
			final AmqpExchangeType type, final boolean durable,
			final boolean autoDelete, final boolean passive,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.declareExchange(name, type, durable, autoDelete,
							passive, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#declareQueue(java.lang.String,
	 * boolean, boolean, boolean, boolean, java.util.List,
	 * mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> declareQueue(final String queue,
			final boolean exclusive, final boolean durable,
			final boolean autoDelete, final boolean passive,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.declareQueue(queue, exclusive, durable, autoDelete,
							passive, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#bindQueue(java.lang.String,
	 * java.lang.String, java.lang.String, java.util.List,
	 * mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> bindQueue(final String exchange,
			final String queue, final String routingKey,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.bindQueue(exchange, queue, routingKey,
							op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#publish(mosaic.connector.queue.
	 * AmqpOutboundMessage, java.util.List,
	 * mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> publish(final AmqpOutboundMessage message,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.publish(message, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#consume(java.lang.String,
	 * java.lang.String, boolean, boolean, java.lang.Object, java.util.List,
	 * mosaic.core.ops.CompletionInvocationHandler,
	 * mosaic.connector.queue.IAmqpConsumerCallback)
	 */
	@Override
	public IResult<String> consume(final String queue, final String consumer,
			final boolean exclusive, final boolean autoAck, final Object extra,
			List<IOperationCompletionHandler<String>> handlers,
			CompletionInvocationHandler<String> iHandler,
			final IAmqpConsumerCallback consumerCallback) {
		IResult<String> result = null;
		synchronized (this) {
			final EventDrivenOperation<String> op = new EventDrivenOperation<String>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.consume(queue, consumer, exclusive, autoAck, extra,
							op.getCompletionHandlers(), consumerCallback);

				}
			});
			result = new EventDrivenResult<String>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#cancel(java.lang.String,
	 * java.util.List, mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> cancel(final String consumer,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.cancel(consumer, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#get(java.lang.String, boolean,
	 * java.util.List, mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> get(final String queue, final boolean autoAck,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.get(queue, autoAck, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.queue.IAmqpQueue#ack(long, boolean, java.util.List,
	 * mosaic.core.ops.CompletionInvocationHandler)
	 */
	@Override
	public IResult<Boolean> ack(final long delivery, final boolean multiple,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.ack(delivery, multiple, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

}
