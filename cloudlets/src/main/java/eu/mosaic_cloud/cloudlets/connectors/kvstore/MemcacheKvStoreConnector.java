/*
 * #%L
 * mosaic-cloudlets
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
package eu.mosaic_cloud.cloudlets.connectors.kvstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

/**
 * Cloudlet-level accessor for memcached-based key value storages. Cloudlets
 * will use an object of this type to get access to a memcached storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using the accessor
 */
public class MemcacheKvStoreConnector<C> extends KvStoreConnector<C> implements
		IMemcacheKvStoreConnector<C> {

	/**
	 * Creates a new accessor.
	 * 
	 * @param config
	 *            configuration data required by the accessor
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 * @param encoder
	 *            encoder used for serializing data
	 */
	public MemcacheKvStoreConnector(IConfiguration config,
			ICloudletController<C> cloudlet, DataEncoder<?> encoder) {
		super(config, cloudlet, encoder);
	}

	@Override
	public CallbackCompletion<Void> set(final String key, final Object value, int exp,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, value, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).setSucceeded(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, error, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).setFailed(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector.class).set(key, exp, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public CallbackCompletion<Void> add(final String key, final Object value, int exp,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, value, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).addSucceeded(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, error, extra);
				(getCallback(IMemcacheKvStoreConnectorCallback.class)).addFailed(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector.class).add(key, exp, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public CallbackCompletion<Void> append(final String key, final Object value,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, value, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).appendSucceeded(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, error, extra);
				(getCallback(IMemcacheKvStoreConnectorCallback.class)).appendFailed(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector.class).append(key, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public CallbackCompletion<Void> prepend(final String key, final Object value,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, value, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).prependSucceeded(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, error, extra);
				(getCallback(IMemcacheKvStoreConnectorCallback.class)).prependFailed(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector.class).prepend(key, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public CallbackCompletion<Void> cas(final String key, final Object value,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, value, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).casSucceeded(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, error, extra);
				(getCallback(IMemcacheKvStoreConnectorCallback.class)).casFailed(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector.class).cas(key, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public CallbackCompletion<Void> replace(final String key, final Object value,
			int exp, final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, value, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).replaceSucceeded(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, key, error, extra);
				(getCallback(IMemcacheKvStoreConnectorCallback.class)).replaceFailed(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector.class).replace(key, exp,
				value, handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public CallbackCompletion<Void> getBulk(final List<String> keys,
			final Object extra) {
		IOperationCompletionHandler<Map<String, Object>> cHandler = new IOperationCompletionHandler<Map<String, Object>>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Map<String, Object> result) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, keys, result, extra);
				getCallback(IMemcacheKvStoreConnectorCallback.class).getBulkSucceeded(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KvStoreCallbackCompletionArguments<C> arguments = new KvStoreCallbackCompletionArguments<C>(
						MemcacheKvStoreConnector.this.cloudlet, keys, error, extra);
				(getCallback(IMemcacheKvStoreConnectorCallback.class)).getBulkFailed(
						MemcacheKvStoreConnector.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Map<String, Object>>> handlers = new ArrayList<IOperationCompletionHandler<Map<String, Object>>>();
		handlers.add(cHandler);
		return super.getConnector(eu.mosaic_cloud.connectors.kvstore.memcache.IMemcacheKvStoreConnector.class).getBulk(keys,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

}
