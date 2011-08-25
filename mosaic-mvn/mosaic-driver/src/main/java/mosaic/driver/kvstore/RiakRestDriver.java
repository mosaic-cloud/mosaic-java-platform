package mosaic.driver.kvstore;

import java.io.IOException;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationFactory;
import mosaic.driver.ConfigProperties;

/**
 * Driver class for the Riak key-value database management systems.
 * 
 * Rest Interface
 * 
 * @author Carmine Di Biase
 * 
 */
public class RiakRestDriver extends BaseKeyValueDriver {
	private String riakHost;
	private int riakPort;

	/**
	 * Creates a new Riak driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param riakHost
	 *            the hostname of the Riak server
	 * @param riakPort
	 *            the port for the Riak server
	 */
	private RiakRestDriver(int noThreads, String riakHost, int riakPort) {
		super(noThreads);
		this.riakHost = riakHost;
		this.riakPort = riakPort;
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
	 */
	public static synchronized RiakRestDriver create(IConfiguration config)
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

		// String bucket = ConfigUtils.resolveParameter(config,
		//				ConfigProperties.getString("KVStoreDriver.3"), //$NON-NLS-1$
		// String.class, "");

		RiakRestDriver wrapper = new RiakRestDriver(noThreads, host, port);
		MosaicLogger.getLogger().trace(
				"Created Riak REST driver for host " + host + ":" + port);
		return wrapper;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	public synchronized void destroy() {
		super.destroy();
		MosaicLogger.getLogger().trace("RiakDriver destroyed."); //$NON-NLS-1$
	}

	@Override
	protected IOperationFactory createOperationFactory(Object... params) {
		String bucket = (String) params[0];
		IOperationFactory opFactory = RiakRestOperationFactory.getFactory(
				riakHost, riakPort, bucket);
		return opFactory;
	}

	/*
	 * Here is eventually possible add more particular operation for your key
	 * value store engine
	 */
}
