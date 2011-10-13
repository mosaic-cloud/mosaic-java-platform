package mosaic.connector.kvstore.memcached;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import mosaic.connector.ConfigProperties;
import mosaic.connector.interop.kvstore.memcached.MemcachedProxy;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.EventDrivenOperation;
import mosaic.core.ops.EventDrivenResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.DataEncoder;
import mosaic.interop.kvstore.KeyValueSession;
import mosaic.interop.kvstore.MemcachedSession;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Connector for key-value distributed storage systems implementing the
 * memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedStoreConnector<T extends Object> extends KeyValueStoreConnector<T> implements
		IMemcachedStore<T> {

	private MemcachedStoreConnector(MemcachedProxy<T> proxy, int noThreads,
			DataEncoder<T> encoder) {
		super(proxy, noThreads, encoder);
	}

	/**
	 * Creates the connector.
	 * 
	 * @param config
	 *            the configuration parameters required by the connector. This
	 *            should also include configuration settings for the
	 *            corresponding driver.
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @return the connector
	 * @throws Throwable
	 */
	public static <T extends Object> MemcachedStoreConnector<T> create(IConfiguration config,
			DataEncoder<T> encoder) throws Throwable {
		String connectorIdentifier = UUID.randomUUID().toString();
		int noThreads = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KeyValueStoreConnector.0"), Integer.class, 1); //$NON-NLS-1$
		String bucket = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KeyValueStoreConnector.1"), String.class, ""); //$NON-NLS-1$
		String driverChannel = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AllConnector.0"), String.class, "");
		String driverIdentifier = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("AllConnector.1"), String.class, "");
		ZeroMqChannel channel = new ZeroMqChannel(connectorIdentifier,
				AbortingExceptionTracer.defaultInstance);
		channel.register(KeyValueSession.CONNECTOR);
		channel.register(MemcachedSession.CONNECTOR);
		channel.connect(driverChannel);
		MemcachedProxy<T> proxy = MemcachedProxy
				.create(config, connectorIdentifier, driverIdentifier, bucket,
						channel, encoder);
		return new MemcachedStoreConnector<T>(proxy, noThreads, encoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.IResourceConnector#destroy()
	 */
	@Override
	public void destroy() throws Throwable {
		super.destroy();
		MosaicLogger.getLogger().trace("MemcachedConnector destroyed.");
	}

	@Override
	public IResult<Boolean> set(final String key, final int exp,
			final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				((MemcachedProxy<T>)getProxy(MemcachedProxy.class)).set(key, exp, data,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> add(final String key, final int exp,
			final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				getProxy(MemcachedProxy.class).add(key, exp, data,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> replace(final String key, final int exp,
			final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				getProxy(MemcachedProxy.class).replace(key, exp, data,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> append(final String key, final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				getProxy(MemcachedProxy.class).append(key, data,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> prepend(final String key, final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				getProxy(MemcachedProxy.class).prepend(key, data,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> cas(final String key, final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				getProxy(MemcachedProxy.class).cas(key, data,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Map<String, T>> getBulk(final List<String> keys,
			List<IOperationCompletionHandler<Map<String, T>>> handlers,
			CompletionInvocationHandler<Map<String, T>> iHandler) {
		IResult<Map<String, T>> result = null;
		final EventDrivenOperation<Map<String, T>> op = new EventDrivenOperation<Map<String, T>>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				getProxy(MemcachedProxy.class).getBulk(keys,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Map<String, T>>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<List<String>> list(
			List<IOperationCompletionHandler<List<String>>> handlers,
			CompletionInvocationHandler<List<String>> iHandler) {
		IResult<List<String>> result = null;
		final EventDrivenOperation<List<String>> op = new EventDrivenOperation<List<String>>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				getProxy(MemcachedProxy.class).list(op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<List<String>>(op);
		submitOperation(op.getOperation());

		return result;
	}

}
