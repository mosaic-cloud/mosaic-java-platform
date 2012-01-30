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
package eu.mosaic_cloud.cloudlets.resources.kvstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.connectors.kvstore.memcached.IMemcachedStore;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;

/**
 * Cloudlet-level accessor for memcached-based key value storages. Cloudlets
 * will use an object of this type to get access to a memcached storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using the accessor
 */
public class MemcachedAccessor<C> extends KeyValueAccessor<C> implements
		IMemcachedAccessor<C> {

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
	public MemcachedAccessor(IConfiguration config,
			ICloudletController<C> cloudlet, DataEncoder<?> encoder) {
		super(config, cloudlet, encoder);
	}

	@Override
	public IResult<Boolean> set(final String key, final Object value, int exp,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, value, extra);
				getCallback(IMemcachedAccessorCallback.class).setSucceeded(
						MemcachedAccessor.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, error, extra);
				getCallback(IMemcachedAccessorCallback.class).setFailed(
						MemcachedAccessor.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(IMemcachedStore.class).set(key, exp, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Boolean> add(final String key, final Object value, int exp,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, value, extra);
				getCallback(IMemcachedAccessorCallback.class).addSucceeded(
						MemcachedAccessor.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, error, extra);
				(getCallback(IMemcachedAccessorCallback.class)).addFailed(
						MemcachedAccessor.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(IMemcachedStore.class).add(key, exp, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Boolean> append(final String key, final Object value,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, value, extra);
				getCallback(IMemcachedAccessorCallback.class).appendSucceeded(
						MemcachedAccessor.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, error, extra);
				(getCallback(IMemcachedAccessorCallback.class)).appendFailed(
						MemcachedAccessor.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(IMemcachedStore.class).append(key, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Boolean> prepend(final String key, final Object value,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, value, extra);
				getCallback(IMemcachedAccessorCallback.class).prependSucceeded(
						MemcachedAccessor.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, error, extra);
				(getCallback(IMemcachedAccessorCallback.class)).prependFailed(
						MemcachedAccessor.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(IMemcachedStore.class).prepend(key, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Boolean> cas(final String key, final Object value,
			final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, value, extra);
				getCallback(IMemcachedAccessorCallback.class).casSucceeded(
						MemcachedAccessor.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, error, extra);
				(getCallback(IMemcachedAccessorCallback.class)).casFailed(
						MemcachedAccessor.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(IMemcachedStore.class).cas(key, value,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Boolean> replace(final String key, final Object value,
			int exp, final Object extra) {
		IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Boolean result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, value, extra);
				getCallback(IMemcachedAccessorCallback.class).replaceSucceeded(
						MemcachedAccessor.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, key, error, extra);
				(getCallback(IMemcachedAccessorCallback.class)).replaceFailed(
						MemcachedAccessor.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlers.add(cHandler);
		return super.getConnector(IMemcachedStore.class).replace(key, exp,
				value, handlers,
				this.cloudlet.getResponseInvocationHandler(cHandler));
	}

	@Override
	public IResult<Map<String, Object>> getBulk(final List<String> keys,
			final Object extra) {
		IOperationCompletionHandler<Map<String, Object>> cHandler = new IOperationCompletionHandler<Map<String, Object>>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Map<String, Object> result) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, keys, result, extra);
				getCallback(IMemcachedAccessorCallback.class).getBulkSucceeded(
						MemcachedAccessor.this.cloudletContext, arguments);

			}

			@SuppressWarnings("unchecked")
			@Override
			public <E extends Throwable> void onFailure(E error) {
				KeyValueCallbackArguments<C> arguments = new KeyValueCallbackArguments<C>(
						MemcachedAccessor.this.cloudlet, keys, error, extra);
				(getCallback(IMemcachedAccessorCallback.class)).getBulkFailed(
						MemcachedAccessor.this.cloudletContext, arguments);
			}
		};
		List<IOperationCompletionHandler<Map<String, Object>>> handlers = new ArrayList<IOperationCompletionHandler<Map<String, Object>>>();
		handlers.add(cHandler);
		return super.getConnector(IMemcachedStore.class).getBulk(keys,
				handlers, this.cloudlet.getResponseInvocationHandler(cHandler));
	}

}
