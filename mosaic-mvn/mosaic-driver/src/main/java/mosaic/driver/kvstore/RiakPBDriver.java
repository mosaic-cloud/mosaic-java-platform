package mosaic.driver.kvstore;

import java.io.IOException;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationFactory;
import mosaic.driver.ConfigProperties;

import com.basho.riak.pbc.RiakClient;

/**
 * Driver class for the Riak key-value database management systems.
 * 
 * Protocol Buffer Interface
 * 
 * @author Carmine Di Biase
 * 
 */

public class RiakPBDriver extends BaseKeyValueDriver {

	private RiakClient riakClient;

	/**
	 * Creates a new Riak driver.
	 * 
	 * @param client
	 *            the Riak client object
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param opFactory
	 *            factory for handling key-value stores operations
	 */
	private RiakPBDriver(RiakClient client, int noThreads,
			IOperationFactory opFactory) {
		super(noThreads, opFactory);
		this.riakClient = client;
	}

	/**
	 * Returns a Riak driver.
	 * 
	 * @param config
	 *            the configuration parameters required by the driver:
	 *            <ol>
	 *            <il>for each server to which the driver should connect there
	 *            should be two parameters: <i>host_&lt;server_number&gt;</i>
	 *            and <i>port_&lt;server_number&gt;</i> indicating the hostnames
	 *            and the ports where the servers are installed </il>
	 *            <il><i>memcached.driver_threads</i> specifies the maximum
	 *            number of threads that shall be created by the driver for
	 *            serving requests </il>
	 *            </ol>
	 * @return the driver
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public static synchronized RiakPBDriver create(IConfiguration config)
			throws IOException, ConnectionException {
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
		String bucket = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.3"), //$NON-NLS-1$
				String.class, "");
		// String address="http://"+host+":"+port+"/riak";
		RiakClient riakClient = new RiakClient(host, port);
//		if (riakClient == null)
//			throw new ConnectionException("Cannot connect to driver: " + host
//					+ ":" + port);
		IOperationFactory opFactory = RiakPBOperationFactory.getFactory(
				riakClient, bucket);
		RiakPBDriver wrapper = new RiakPBDriver(riakClient, noThreads,
				opFactory);
		return wrapper;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	@Override
	public synchronized void destroy() {
		super.destroy();
		MosaicLogger.getLogger().trace("RiakDriver destroyed."); //$NON-NLS-1$
	}

	/*
	 * Here is eventually possible add more particular operation for your key
	 * value store engine
	 */

}
