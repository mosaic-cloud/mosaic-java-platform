package mosaic.connector.kvstore;

import java.util.List;
import java.util.UUID;
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
import mosaic.core.utils.DataEncoder;
import mosaic.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Connector for key-value distributed storage systems .
 * 
 * @author Georgiana Macariu
 * @param <T>
 *            type of stored data
 */
public class KeyValueStoreConnector<T extends Object> implements IKeyValueStore<T> {

	private KeyValueProxy<T> proxy;
	private ExecutorService executor;
	protected DataEncoder<?> dataEncoder;

	protected KeyValueStoreConnector(KeyValueProxy<T> proxy, int noThreads,
			DataEncoder<T> encoder) {
		this.proxy = proxy;
		this.executor = Executors.newFixedThreadPool(noThreads);
		this.dataEncoder = encoder;
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
	public static <T extends Object>  KeyValueStoreConnector<T> create(IConfiguration config,
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
		channel.connect(driverChannel);
		KeyValueProxy<T> proxy = KeyValueProxy.create(config, connectorIdentifier,
				driverIdentifier, bucket, channel, encoder);
		MosaicLogger.getLogger().debug(
				"KeyValueConnector connecting to " + driverChannel + " bucket "
						+ bucket);
		return new KeyValueStoreConnector<T>(proxy, noThreads, encoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.connector.IResourceConnector#destroy()
	 */
	@Override
	public void destroy() throws Throwable {
		this.proxy.destroy();
		this.executor.shutdown();
		MosaicLogger.getLogger().trace("KeyValueStoreConnector destroyed.");
	}

	@Override
	public IResult<T> get(final String key,
			List<IOperationCompletionHandler<T>> handlers,
			CompletionInvocationHandler<T> iHandler) {
		IResult<T> result = null;
		final EventDrivenOperation<T> op = new EventDrivenOperation<T>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@Override
			public void run() {
				KeyValueStoreConnector.this.proxy.get(key,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<T>(op);
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
				KeyValueStoreConnector.this.proxy.delete(key,
						op.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(op);
		submitOperation(op.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> set(final String key, final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result = null;
		final EventDrivenOperation<Boolean> op = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		op.setOperation(new Runnable() {

			@Override
			public void run() {
				KeyValueStoreConnector.this.proxy.set(key, data,
						op.getCompletionHandlers());
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
				KeyValueStoreConnector.this.proxy.list(op
						.getCompletionHandlers());

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
	protected <P extends KeyValueProxy<T>> P getProxy(Class<P> proxyClass) {
		return proxyClass.cast(this.proxy);
	}

	/**
	 * Submits an operation for execution
	 * 
	 * @param op
	 *            the operation
	 */
	protected synchronized void submitOperation(Runnable op) {
		this.executor.submit(op);
	}

}
