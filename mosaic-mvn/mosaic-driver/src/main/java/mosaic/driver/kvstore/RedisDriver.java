package mosaic.driver.kvstore;

import java.io.IOException;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationFactory;
import mosaic.driver.ConfigProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

/**
 * Driver class for the Redis key-value database management systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class RedisDriver extends BaseKeyValueDriver {

	private Jedis jedisClient;

	/**
	 * Creates a new Redis driver.
	 * 
	 * @param client
	 *            the Redis client object
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param opFactory
	 *            factory for handling key-value stores operations
	 */
	private RedisDriver(Jedis client, int noThreads, IOperationFactory opFactory) {
		super(noThreads, opFactory);
		this.jedisClient = client;
		this.jedisClient.connect();
	}

	/**
	 * Returns a Redis driver.
	 * 
	 * @param config
	 *            the configuration parameters required by the driver:
	 *            <ol>
	 *            <il>there should be two parameters: <i>kvstore.host</i> and
	 *            <i>kvstore.port</i> indicating the hostname and the port where
	 *            the Redis server is listening </il>
	 *            <il><i>kvstore.driver_threads</i> specifies the maximum number
	 *            of threads that shall be created by the driver for serving
	 *            requests </il>
	 *            </ol>
	 * @return the driver
	 * @throws IOException
	 */
	public static synchronized RedisDriver create(IConfiguration config)
			throws IOException {
		int port, noThreads;

		String host = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.0"), //$NON-NLS-1$
				String.class, ""); //$NON-NLS-1$

		port = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.1"), //$NON-NLS-1$
				Integer.class, 0);

		noThreads = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KVStoreDriver.2"), Integer.class, 1); //$NON-NLS-1$
		int db = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.3"), //$NON-NLS-1$
				Integer.class, (-1));
		String passwd = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.4"), //$NON-NLS-1$
				String.class, ""); //$NON-NLS-1$
		Jedis jedis = new Jedis(host, port, 0);
		if (!passwd.equals("")) { //$NON-NLS-1$
			jedis.auth(passwd);
		}
		if (db > -1) {
			jedis.select(db);
//			jedis.flushDB();
		}

		IOperationFactory opFactory = RedisOperationFactory.getFactory(jedis);
		RedisDriver wrapper = new RedisDriver(jedis, noThreads, opFactory);
		return wrapper;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	public synchronized void destroy() {
		super.destroy();
		// this.jedisClient.bgsave(); //TODO
		this.jedisClient.disconnect();
		MosaicLogger.getLogger().trace("RedisDriver destroyed."); //$NON-NLS-1$
	}

}
