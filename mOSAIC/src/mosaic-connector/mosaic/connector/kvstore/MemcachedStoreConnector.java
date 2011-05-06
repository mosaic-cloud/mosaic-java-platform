package mosaic.connector.kvstore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mosaic.connector.IResourceConnector;
import mosaic.connector.interop.MemcachedProxy;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
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
public class MemcachedStoreConnector implements IMemcachedStore,
		IResourceConnector {

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
	 * @throws IOException
	 */
	public static synchronized MemcachedStoreConnector create(
			IConfiguration config) throws IOException {
		int noThreads = ConfigUtils.resolveParameter(config,
				"memcached.connector_threads", Integer.class, 1);
		MemcachedProxy proxy = MemcachedProxy.create(config);
		return new MemcachedStoreConnector(proxy, noThreads);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.IResourceConnector#destroy()
	 */
	public void destroy() {
		// FIXME wait for running operations to complete
		proxy.destroy();
		executor.shutdown();
	}

	@Override
	public IResult<Boolean> set(final String key, final int exp,
			final Object data, List<IOperationCompletionHandler<Boolean>> handlers) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers);
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
			final Object data, List<IOperationCompletionHandler<Boolean>> handlers) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers);
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
			final Object data, List<IOperationCompletionHandler<Boolean>> handlers) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers);
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
			List<IOperationCompletionHandler<Boolean>> handlers) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers);
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
			List<IOperationCompletionHandler<Boolean>> handlers) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers);
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
			List<IOperationCompletionHandler<Boolean>> handlers) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers);
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
			List<IOperationCompletionHandler<Object>> handlers) {
		IResult<Object> result = null;
		synchronized (this) {
			final EventDrivenOperation<Object> op = new EventDrivenOperation<Object>(
					handlers);
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
			List<IOperationCompletionHandler<Map<String, Object>>> handlers) {
		IResult<Map<String, Object>> result = null;
		synchronized (this) {
			final EventDrivenOperation<Map<String, Object>> op = new EventDrivenOperation<Map<String, Object>>(
					handlers);
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
			List<IOperationCompletionHandler<Boolean>> handlers) {
		IResult<Boolean> result = null;
		synchronized (this) {
			final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
					handlers);
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

}
