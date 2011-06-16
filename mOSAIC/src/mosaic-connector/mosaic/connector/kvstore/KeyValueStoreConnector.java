package mosaic.connector.kvstore;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mosaic.connector.ConfigProperties;
import mosaic.connector.interop.kvstore.KeyValueProxy;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.EventDrivenOperation;
import mosaic.core.ops.EventDrivenResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;

/**
 * Connector for key-value distributed storage systems .
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueStoreConnector implements IKeyValueStore {

	private KeyValueProxy proxy;
	private ExecutorService executor;

	protected KeyValueStoreConnector(KeyValueProxy proxy, int noThreads) {
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
	public static KeyValueStoreConnector create(IConfiguration config)
			throws Throwable {
		int noThreads = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KeyValueStoreConnector.0"), Integer.class, 1); //$NON-NLS-1$
		KeyValueProxy proxy = KeyValueProxy.create(config);
		return new KeyValueStoreConnector(proxy, noThreads);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.IResourceConnector#destroy()
	 */
	public void destroy() throws Throwable {
		proxy.destroy();
		executor.shutdown();
		MosaicLogger.getLogger().trace("KeyValueStoreConnector destroyed.");
	}

	@Override
	public IResult<Object> get(final String key,
			List<IOperationCompletionHandler<Object>> handlers,
			CompletionInvocationHandler<Object> iHandler) {
		IResult<Object> result = null;
		final EventDrivenOperation<Object> op = new EventDrivenOperation<Object>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@Override
			public void run() {
				proxy.get(key, op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Object>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> delete(final String key,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@Override
			public void run() {
				proxy.delete(key, op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> set(final String key, final Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@Override
			public void run() {
				proxy.set(key, data, op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
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
				proxy.list(op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<List<String>>(op);
		submitOperation(op.getOperation());

		return result;
	}

	/**
	 * Returns the proxy used by the connector
	 * 
	 * @param <T>
	 *            the type of the proxy
	 * @param proxyClass
	 *            the class for the type of the proxy
	 * @return the proxy
	 */
	protected <T extends KeyValueProxy> T getProxy(Class<T> proxyClass) {
		return proxyClass.cast(this.proxy);
	}

	/**
	 * Submits an operation for execution
	 * 
	 * @param op
	 *            the operation
	 */
	protected synchronized void submitOperation(Runnable op) {
		executor.submit(op);
	}

}
