package mosaic.driver.kvstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;
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

	private RedisOperationFactory(String host, int port, String user,
			String passwd, String bucket) {
		super();
		this.redisClient = new Jedis(host, port, 0);
		if (!passwd.equals("")) { //$NON-NLS-1$
			this.redisClient.auth(passwd);
		}
		int db = Integer.parseInt(bucket);
		if (db > -1) {
			this.redisClient.select(db);
			// jedis.flushDB();
		}
		this.redisClient.connect();
	}

	/**
	 * Creates a new factory.
	 * 
	 * @param host
	 *            the hostname of the Redis server
	 * @param port
	 *            the port for the Redis server
	 * @param user
	 *            the username for connecting to the server
	 * @param passwd
	 *            the password for connecting to the serve
	 * @param bucket
	 *            the bucket where all operations are applied
	 * @return the factory
	 */
	public final static RedisOperationFactory getFactory(String host, int port,
			String user, String passwd, String bucket) {
		return new RedisOperationFactory(host, port, user, passwd, bucket);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.core.IOperationFactory#getOperation(mosaic.core.IOperationType,
	 * java.lang.Object[])
	 */
	@Override
	public IOperation<?> getOperation(final IOperationType type,
			Object... parameters) {
		IOperation<?> operation = null;
		if (!(type instanceof KeyValueOperations)) {
			operation = new GenericOperation<Object>(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					throw new UnsupportedOperationException(
							"Unsupported operation: " + type.toString());
				}

			});
			return operation;
		}

		final KeyValueOperations mType = (KeyValueOperations) type;
		final String key;
		final byte[] keyBytes;
		final byte[] dataBytes;
		switch (mType) {
		case SET:
			key = (String) parameters[0];
			// data = (String) parameters[1];
			keyBytes = SafeEncoder.encode(key);
			dataBytes = (byte[]) parameters[1];// SafeEncoder.encode(data);
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					String opResult = RedisOperationFactory.this.redisClient
							.set(keyBytes, dataBytes);
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
			operation = new GenericOperation<byte[]>(new Callable<byte[]>() {

				@Override
				public byte[] call() throws Exception {
					byte[] result = RedisOperationFactory.this.redisClient
							.get(keyBytes);
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
							Set<String> opResult = RedisOperationFactory.this.redisClient
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
			operation = new GenericOperation<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					long opResult = RedisOperationFactory.this.redisClient
							.del(keyBytes);
					if (opResult == 0)
						return false;
					return true;
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

	@Override
	public void destroy() {
		// this.redisClient.bgsave(); //TODO
		this.redisClient.disconnect();
	}

}
