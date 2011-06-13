package mosaic.driver.kvstore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;
import mosaic.core.utils.SerDesUtils;
import redis.clients.jedis.Jedis;
import redis.clients.util.SafeEncoder;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Redis key-value store.
 * 
 * @author Georgiana Macariu
 * 
 */
public class RedisOperationFactory implements IOperationFactory {
	private Jedis redisClient = null;

	private RedisOperationFactory(Jedis mcClient) {
		super();
		this.redisClient = mcClient;
	}

	/**
	 * Creates a new factory.
	 * 
	 * @param client
	 *            the Memcached client used for communicating with the key-value
	 *            system
	 * @return the factory
	 */
	public final static RedisOperationFactory getFactory(Jedis client) {
		return new RedisOperationFactory(client);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.core.IOperationFactory#getOperation(mosaic.core.IOperationType,
	 * java.lang.Object[])
	 */
	@Override
	public IOperation<?> getOperation(IOperationType type, Object... parameters) {
		IOperation<?> operation = null;
		if (!(type instanceof KeyValueOperations)) {
			throw new IllegalArgumentException("Unsupported operation: "
					+ type.toString());
		}
		KeyValueOperations mType = (KeyValueOperations) type;
		String key;
		Object data;
		final byte[] keyBytes;
		final byte[] dataBytes;
		try {
			switch (mType) {
			case SET:
				key = (String) parameters[0];
				data = parameters[1];
				keyBytes = SafeEncoder.encode(key);
				dataBytes = SerDesUtils.toBytes(data);
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								String opResult = redisClient.set(keyBytes,
										dataBytes);
								opResult = opResult.trim();
								if (opResult.equalsIgnoreCase("OK"))
									return true;
								return false;
							}

						});
				break;
			case GET:
				key = (String) parameters[0];
				keyBytes = SafeEncoder.encode(key);
				operation = new GenericOperation<Object>(
						new Callable<Object>() {

							@Override
							public Object call() throws Exception {
								byte[] opResult = redisClient.get(keyBytes);
								Object result = null;
								if (opResult != null)
									result = SerDesUtils.toObject(opResult);
								return result;
							}

						});
				break;
			case LIST:
				final String pattern = "*";
				operation = new GenericOperation<List<String>>(
						new Callable<List<String>>() {

							@Override
							public List<String> call() throws Exception {
								Set<String> opResult = redisClient
										.keys(pattern);
								List<String> result = new ArrayList<String>();
								for (String key : opResult) {
									result.add(key);
								}
								return result;
							}

						});
				break;
			case DELETE:
				key = (String) parameters[0];
				keyBytes = SafeEncoder.encode(key);
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								long opResult = redisClient.del(keyBytes);
								if (opResult == 0)
									return false;
								return true;
							}

						});
				break;
			default:
				throw new UnsupportedOperationException(
						"Unsupported operation: " + mType.toString());
			}
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
			// TODO send error
		}
		return operation;
	}

}
