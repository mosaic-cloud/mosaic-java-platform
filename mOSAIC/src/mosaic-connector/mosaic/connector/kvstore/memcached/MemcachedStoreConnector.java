package mosaic.connector.kvstore.memcached;

import java.util.List;
import java.util.Map;

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

/**
 * Connector for key-value distributed storage systems implementing the
 * memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedStoreConnector extends KeyValueStoreConnector implements
		IMemcachedStore {

	private MemcachedStoreConnector(MemcachedProxy proxy, int noThreads) {
		super(proxy, noThreads);
	}

	/**
	 * Creates the connector.
	 * 
	 * @param config
	 *            the configuration parameters required by the connector. This
	 *            should also include configuration settings for the
	 *            corresponding driver.
	 * @return the connector
	 * @throws Throwable
	 */
	public static MemcachedStoreConnector create(IConfiguration config)
			throws Throwable {
		int noThreads = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KeyValueStoreConnector.0"), Integer.class, 1); //$NON-NLS-1$
		MemcachedProxy proxy = MemcachedProxy.create(config);
		return new MemcachedStoreConnector(proxy, noThreads);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.IResourceConnector#destroy()
	 */
	public void destroy() throws Throwable {
		super.destroy();
		MosaicLogger.getLogger().trace("MemcachedConnector destroyed.");
	}

	@Override
	public IResult<Boolean> set(final String key, final int exp,
			final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@Override
			public void run() {
				getProxy(MemcachedProxy.class).set(key, exp, data,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> add(final String key, final int exp,
			final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

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
			final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

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
	public IResult<Boolean> append(final String key, final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

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
	public IResult<Boolean> prepend(final String key, final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

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
	public IResult<Boolean> cas(final String key, final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

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
	public IResult<Map<String, Object>> getBulk(final List<String> keys,
			List<IOperationCompletionHandler<Map<String, Object>>> handlers,
			CompletionInvocationHandler<Map<String, Object>> iHandler) {
		IResult<Map<String, Object>> result = null;
		final EventDrivenOperation<Map<String, Object>> op = new EventDrivenOperation<Map<String, Object>>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@Override
			public void run() {
				getProxy(MemcachedProxy.class).getBulk(keys,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Map<String, Object>>(op);
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
