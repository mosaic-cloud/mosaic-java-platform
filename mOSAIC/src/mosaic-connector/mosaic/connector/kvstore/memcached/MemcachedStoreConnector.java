package mosaic.connector.kvstore.memcached;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mosaic.connector.ConfigProperties;
import mosaic.connector.interop.kvstore.memcached.MemcachedProxy;
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
public class MemcachedStoreConnector implements IMemcachedStore {

	private MemcachedProxy proxy;
	private ExecutorService executor;

	private MemcachedStoreConnector(MemcachedProxy proxy, int noThreads) {
		this.proxy = proxy;
		this.executor = Executors.newFixedThreadPool(noThreads);
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
	public static synchronized MemcachedStoreConnector create(
			IConfiguration config) throws Throwable {
		int noThreads = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("MemcachedStoreConnector.0"), Integer.class, 1); //$NON-NLS-1$
		MemcachedProxy proxy = MemcachedProxy.create(config);
		return new MemcachedStoreConnector(proxy, noThreads);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.IResourceConnector#destroy()
	 */
	public void destroy() throws Throwable {
		proxy.destroy();
		executor.shutdown();
		MosaicLogger.getLogger().trace("MemcachedConnector destroyed.");
	}

	@Override
	public IResult<Boolean> set(final String key, final int exp,
			final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.set(key, exp, data, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Boolean> add(final String key, final int exp,
			final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.add(key, exp, data, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Boolean> replace(final String key, final int exp,
			final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.replace(key, exp, data, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Boolean> append(final String key, final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.append(key, data, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Boolean> prepend(final String key, final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.prepend(key, data, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Boolean> cas(final String key, final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.cas(key, data, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Object> get(final String key,
			List<IOperationCompletionHandler<Object>> handlers,
			CompletionInvocationHandler<Object> iHandler) {
		IResult<Object> result = null;
		synchronized (this) {
			final EventDrivenOperation<Object> op = new EventDrivenOperation<Object>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.get(key, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Object>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Map<String, Object>> getBulk(final List<String> keys,
			List<IOperationCompletionHandler<Map<String, Object>>> handlers,
			CompletionInvocationHandler<Map<String, Object>> iHandler) {
		IResult<Map<String, Object>> result = null;
		synchronized (this) {
			final EventDrivenOperation<Map<String, Object>> op = new EventDrivenOperation<Map<String, Object>>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.getBulk(keys, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Map<String, Object>>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Boolean> delete(final String key,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.delete(key, op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<Boolean>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

	@Override
	public IResult<Boolean> set(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> iResult = this.set(key, 0, data, handlers, iHandler);
		return iResult;
	}

	@Override
	public IResult<List<String>> list(
			List<IOperationCompletionHandler<List<String>>> handlers,
			CompletionInvocationHandler<List<String>> iHandler) {
		IResult<List<String>> result = null;
		synchronized (this) {
			final EventDrivenOperation<List<String>> op = new EventDrivenOperation<List<String>>(
					handlers, iHandler);
			op.setOperation(new Runnable() {

				@Override
				public void run() {
					proxy.list(op.getCompletionHandlers());

				}
			});
			result = new EventDrivenResult<List<String>>(op);
			executor.submit(op.getOperation());
		}

		return result;
	}

}
