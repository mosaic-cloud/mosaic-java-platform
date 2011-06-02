package mosaic.cloudlet.resources.kvstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mosaic.cloudlet.core.ICloudletController;
import mosaic.connector.kvstore.IMemcachedStore;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;

/**
 * Ccloudlet-level accessor for memcached-based key value storages. Cloudlets
 * will use an object of this type to get access to a memcached storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using the accessor
 */
public class MemcachedAccessor<S> extends KeyValueAccessor<S> implements
		IMemcachedAccessor<S> {

	/**
	 * Creates a new accessor.
	 * 
	 * @param config
	 *            configuration data required by the accessor
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 */
	public MemcachedAccessor(IConfiguration config,
			ICloudletController<S> cloudlet) {
		super(config, cloudlet);
	}

	@Override
	public IResult<Boolean> set(final String key, final Object value, int exp) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@SuppressWarnings("unchecked")
				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, value);
					getCallback(IMemcachedAccessorCallback.class).setSucceeded(
							MemcachedAccessor.this.cloudletState, arguments);

				}

				@SuppressWarnings("unchecked")
				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, error);
					getCallback(IMemcachedAccessorCallback.class).setFailed(
							MemcachedAccessor.this.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return super.getConnector(IMemcachedStore.class).set(key, exp,
					value, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Boolean> add(final String key, final Object value, int exp) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@SuppressWarnings("unchecked")
				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, value);
					getCallback(IMemcachedAccessorCallback.class).addSucceeded(
							MemcachedAccessor.this.cloudletState, arguments);

				}

				@SuppressWarnings("unchecked")
				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, error);
					(getCallback(IMemcachedAccessorCallback.class)).addFailed(
							MemcachedAccessor.this.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return super.getConnector(IMemcachedStore.class).add(key, exp,
					value, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Boolean> append(final String key, final Object value) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@SuppressWarnings("unchecked")
				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, value);
					getCallback(IMemcachedAccessorCallback.class)
							.appendSucceeded(
									MemcachedAccessor.this.cloudletState,
									arguments);

				}

				@SuppressWarnings("unchecked")
				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, error);
					(getCallback(IMemcachedAccessorCallback.class))
							.appendFailed(MemcachedAccessor.this.cloudletState,
									arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return super.getConnector(IMemcachedStore.class).append(key, value,
					handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Boolean> prepend(final String key, final Object value) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@SuppressWarnings("unchecked")
				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, value);
					getCallback(IMemcachedAccessorCallback.class)
							.prependSucceeded(
									MemcachedAccessor.this.cloudletState,
									arguments);

				}

				@SuppressWarnings("unchecked")
				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, error);
					(getCallback(IMemcachedAccessorCallback.class))
							.prependFailed(
									MemcachedAccessor.this.cloudletState,
									arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return super.getConnector(IMemcachedStore.class).prepend(key,
					value, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Boolean> cas(final String key, final Object value) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@SuppressWarnings("unchecked")
				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, value);
					getCallback(IMemcachedAccessorCallback.class).casSucceeded(
							MemcachedAccessor.this.cloudletState, arguments);

				}

				@SuppressWarnings("unchecked")
				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, error);
					(getCallback(IMemcachedAccessorCallback.class)).casFailed(
							MemcachedAccessor.this.cloudletState, arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return super.getConnector(IMemcachedStore.class).cas(key, value,
					handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Boolean> replace(final String key, final Object value,
			int exp) {
		synchronized (this) {
			IOperationCompletionHandler<Boolean> cHandler = new IOperationCompletionHandler<Boolean>() {

				@SuppressWarnings("unchecked")
				@Override
				public void onSuccess(Boolean result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, value);
					getCallback(IMemcachedAccessorCallback.class)
							.replaceSucceeded(
									MemcachedAccessor.this.cloudletState,
									arguments);

				}

				@SuppressWarnings("unchecked")
				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, key, error);
					(getCallback(IMemcachedAccessorCallback.class))
							.replaceFailed(
									MemcachedAccessor.this.cloudletState,
									arguments);
				}
			};
			List<IOperationCompletionHandler<Boolean>> handlers = new ArrayList<IOperationCompletionHandler<Boolean>>();
			handlers.add(cHandler);
			return super.getConnector(IMemcachedStore.class).replace(key, exp,
					value, handlers,
					this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	@Override
	public IResult<Map<String, Object>> getBulk(final List<String> keys) {
		synchronized (this) {
			IOperationCompletionHandler<Map<String, Object>> cHandler = new IOperationCompletionHandler<Map<String, Object>>() {

				@SuppressWarnings("unchecked")
				@Override
				public void onSuccess(Map<String, Object> result) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, keys, result);
					getCallback(IMemcachedAccessorCallback.class)
							.getBulkSucceeded(
									MemcachedAccessor.this.cloudletState,
									arguments);

				}

				@SuppressWarnings("unchecked")
				@Override
				public <E extends Throwable> void onFailure(E error) {
					KeyValueCallbackArguments<S> arguments = new KeyValueCallbackArguments<S>(
							MemcachedAccessor.this.cloudlet, keys, error);
					(getCallback(IMemcachedAccessorCallback.class))
							.getBulkFailed(
									MemcachedAccessor.this.cloudletState,
									arguments);
				}
			};
			List<IOperationCompletionHandler<Map<String, Object>>> handlers = new ArrayList<IOperationCompletionHandler<Map<String, Object>>>();
			handlers.add(cHandler);
			return super.getConnector(IMemcachedStore.class)
					.getBulk(
							keys,
							handlers,
							this.cloudlet.getResponseInvocationHandler(cHandler));
		}
	}

	
}
