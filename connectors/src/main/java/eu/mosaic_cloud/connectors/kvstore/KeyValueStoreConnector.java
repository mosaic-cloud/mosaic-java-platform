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
package eu.mosaic_cloud.connectors.kvstore;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import eu.mosaic_cloud.connectors.ConfigProperties;
import eu.mosaic_cloud.connectors.interop.kvstore.KeyValueProxy;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.CompletionInvocationHandler;
import eu.mosaic_cloud.platform.core.ops.EventDrivenOperation;
import eu.mosaic_cloud.platform.core.ops.EventDrivenResult;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ThreadConfiguration;

/**
 * Connector for key-value distributed storage systems .
 * 
 * @author Georgiana Macariu
 * @param <T>
 *            type of stored data
 */
public class KeyValueStoreConnector<T extends Object> implements
		IKeyValueStore<T> {

	private final KeyValueProxy<T> proxy;
	private final ThreadingContext threading;
	private final ExecutorService executor;
	protected DataEncoder<?> dataEncoder;

	protected KeyValueStoreConnector(KeyValueProxy<T> proxy,
			ThreadingContext threading, int noThreads, DataEncoder<T> encoder) {
		this.proxy = proxy;
		this.threading = threading;
		this.executor = this.threading.newFixedThreadPool(
				new ThreadConfiguration(this, "operations"), noThreads);
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
	public static <T extends Object> KeyValueStoreConnector<T> create(
			IConfiguration config, DataEncoder<T> encoder,
			ThreadingContext threading) throws Throwable {
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
				threading, AbortingExceptionTracer.defaultInstance);
		channel.register(KeyValueSession.CONNECTOR);
		channel.connect(driverChannel);
		KeyValueProxy<T> proxy = KeyValueProxy
				.create(config, connectorIdentifier, driverIdentifier, bucket,
						channel, encoder);
		MosaicLogger.getLogger().debug(
				"KeyValueConnector connecting to " + driverChannel + " bucket "
						+ bucket);
		return new KeyValueStoreConnector<T>(proxy, threading, noThreads,
				encoder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.mosaic_cloud.connectors.IResourceConnector#destroy()
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
		IResult<T> result;
		final EventDrivenOperation<T> operation = new EventDrivenOperation<T>(
				handlers, iHandler);
		operation.setOperation(new Runnable() {

			@Override
			public void run() {
				KeyValueStoreConnector.this.proxy.get(key,
						operation.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<T>(operation);
		submitOperation(operation.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> delete(final String key,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result;
		final EventDrivenOperation<Boolean> operation = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		operation.setOperation(new Runnable() {

			@Override
			public void run() {
				KeyValueStoreConnector.this.proxy.delete(key,
						operation.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<Boolean>(operation);
		submitOperation(operation.getOperation());

		return result;
	}

	@Override
	public IResult<Boolean> set(final String key, final T data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler) {
		IResult<Boolean> result;
		final EventDrivenOperation<Boolean> operation = new EventDrivenOperation<Boolean>(
				handlers, iHandler);
		operation.setOperation(new Runnable() {

			@Override
			public void run() {
				KeyValueStoreConnector.this.proxy.set(key, data,
						operation.getCompletionHandlers());
			}
		});
		result = new EventDrivenResult<Boolean>(operation);
		submitOperation(operation.getOperation());

		return result;
	}

	@Override
	public IResult<List<String>> list(
			List<IOperationCompletionHandler<List<String>>> handlers,
			CompletionInvocationHandler<List<String>> iHandler) {
		IResult<List<String>> result;
		final EventDrivenOperation<List<String>> operation = new EventDrivenOperation<List<String>>(
				handlers, iHandler);
		operation.setOperation(new Runnable() {

			@Override
			public void run() {
				KeyValueStoreConnector.this.proxy.list(operation
						.getCompletionHandlers());

			}
		});
		result = new EventDrivenResult<List<String>>(operation);
		submitOperation(operation.getOperation());

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
	 * @param operation
	 *            the operation
	 */
	protected void submitOperation(Runnable operation) {
		this.executor.submit(operation);
	}

}
