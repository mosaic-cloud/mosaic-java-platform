package mosaic.driver.kvstore.memcached;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;
import mosaic.driver.kvstore.KeyValueOperations;
import net.spy.memcached.CASResponse;
import net.spy.memcached.MemcachedClient;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * in the memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedOperationFactory implements IOperationFactory {
	private MemcachedClient mcClient = null;

	private MemcachedOperationFactory(MemcachedClient mcClient) {
		super();
		this.mcClient = mcClient;
	}

	/**
	 * Creates a new factory.
	 * 
	 * @param client
	 *            the Memcached client used for communicating with the key-value
	 *            system
	 * @return the factory
	 */
	public final static MemcachedOperationFactory getFactory(
			MemcachedClient client) {
		return new MemcachedOperationFactory(client);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.core.IOperationFactory#getOperation(mosaic.core.IOperationType,
	 * java.lang.Object[])
	 */
	@Override
	public IOperation<?> getOperation(final IOperationType type, Object... parameters) {
		IOperation<?> operation = null;
		if (!(type instanceof KeyValueOperations)) {
			operation = new GenericOperation<Object>(
					new Callable<Object>() {

						@Override
						public Object call() throws Exception {
							throw new UnsupportedOperationException(
									"Unsupported operation: "
											+ type.toString());
						}

					});
			return operation;
		}
		
		final KeyValueOperations mType = (KeyValueOperations) type;
		final String key;
		final int exp;
		final Object data;

		switch (mType) {
		case SET:
			key = (String) parameters[0];
			if (parameters.length == 3) {
				exp = (Integer) parameters[1];
				data = parameters[2];
			} else {
				exp = 0;
				data = parameters[1];
			}
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					Future<Boolean> opResult = mcClient.set(key, exp, data);
					Boolean result = opResult.get();
					return result;
				}

			});
			break;
		case ADD:
			key = (String) parameters[0];
			exp = (Integer) parameters[1];
			data = parameters[2];
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					Future<Boolean> opResult = mcClient.add(key, exp, data);
					Boolean result = opResult.get();
					return result;
				}

			});
			break;
		case REPLACE:
			key = (String) parameters[0];
			exp = (Integer) parameters[1];
			data = parameters[2];
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					Future<Boolean> opResult = mcClient.replace(key, exp, data);
					Boolean result = opResult.get();
					return result;
				}

			});
			break;
		case APPEND:
			key = (String) parameters[0];
			data = parameters[1];
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					long cas = mcClient.gets(key).getCas();
					Future<Boolean> opResult = mcClient.append(cas, key, data);
					Boolean result = opResult.get();
					return result;
				}

			});
			break;
		case PREPEND:
			key = (String) parameters[0];
			data = parameters[1];
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					long cas = mcClient.gets(key).getCas();
					Future<Boolean> opResult = mcClient.prepend(cas, key, data);
					Boolean result = opResult.get();
					return result;
				}

			});
			break;
		case CAS:
			key = (String) parameters[0];
			data = parameters[1];
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					long cas = mcClient.gets(key).getCas();
					Future<CASResponse> opResult = mcClient.asyncCAS(key, cas,
							data);
					Boolean result = (opResult.get() == CASResponse.OK);
					return result;
				}

			});
			break;
		case GET:
			key = (String) parameters[0];
			operation = new GenericOperation<Object>(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					Future<Object> opResult = mcClient.asyncGet(key);
					Object result = opResult.get();
					return result;
				}

			});
			break;
		case GET_BULK:
			final String[] keys = (String[]) parameters;
			operation = new GenericOperation<Map<String, Object>>(
					new Callable<Map<String, Object>>() {

						@Override
						public Map<String, Object> call() throws Exception {
							Future<Map<String, Object>> opResult = mcClient
									.asyncGetBulk(keys);
							Map<String, Object> result = opResult.get();
							return result;
						}

					});
			break;
		case DELETE:
			key = (String) parameters[0];
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					Future<Boolean> opResult = mcClient.delete(key);
					Boolean result = opResult.get();
					return result;
				}

			});
			break;
		default:
			operation = new GenericOperation<Object>(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					throw new UnsupportedOperationException(
							"Unsupported operation: " + mType.toString());
				}

			});

		}

		return operation;
	}

}
