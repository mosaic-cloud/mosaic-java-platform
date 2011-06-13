package mosaic.driver.kvstore.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.GenericResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IResult;
import mosaic.driver.ConfigProperties;
import mosaic.driver.kvstore.BaseKeyValueDriver;
import mosaic.driver.kvstore.KeyValueOperations;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

/**
 * Driver class for the memcached-compatible key-value database management
 * systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedDriver extends BaseKeyValueDriver {

	private MemcachedClient mcClient;

	/**
	 * Creates a new memcached driver.
	 * 
	 * @param client
	 *            the memcached client object
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param opFactory
	 *            factory for handling key-value stores operations
	 */
	private MemcachedDriver(MemcachedClient client, int noThreads,
			IOperationFactory opFactory) {
		super(noThreads, opFactory);
		this.mcClient = client;
	}

	/**
	 * Returns a Memcached driver.
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
	public static synchronized MemcachedDriver create(IConfiguration config)
			throws IOException {
		List<URI> nodes = new ArrayList<URI>();
		List<InetSocketAddress> nodes2 = new ArrayList<InetSocketAddress>();
		int noNodes = 0;
		URI address;
		InetSocketAddress address2;
		int port, noThreads;

		while (true) {
			noNodes++;
			String host = ConfigUtils
					.resolveParameter(
							config,
							ConfigProperties.getString("MemcachedDriver.0") + noNodes, String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (host.equals("")) { //$NON-NLS-1$
				noNodes--;
				break;
			}
			port = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("MemcachedDriver.1") //$NON-NLS-1$
							+ noNodes, Integer.class, 0);
			// try {
			// address = new URI("http://" + host + ":" + port + "/pools");
			// nodes.add(address);
			// } catch (URISyntaxException e) {
			// ExceptionTracer.traceDeferred(e);
			// }

			address2 = new InetSocketAddress(host, port);
			nodes2.add(address2);

		}
		noThreads = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KVStoreDriver.2"), Integer.class, 1); //$NON-NLS-1$
		// String bucket = ConfigUtils.resolveParameter(config,
		//				ConfigProperties.getString("KVStoreDriver.3"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
		//		String user = ConfigUtils.resolveParameter(config, ConfigProperties.getString("KVStoreDriver.5"), //$NON-NLS-1$
		//				String.class, ""); //$NON-NLS-1$
		// String passwd = ConfigUtils.resolveParameter(config,
		//				ConfigProperties.getString("KVStoreDriver.4"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$

		// MosaicLogger.getLogger().trace(
		// "MemcachedDriver using user: " + user + " passwd: " + passwd);
		// System.out.println("MemcachedDriver using user: " + user +
		// " passwd: "
		// + passwd);

		// MemcachedClient client=new MemcachedClient(
		// nodes, bucket, user, passwd);
		MemcachedClient client = new MemcachedClient(
				new BinaryConnectionFactory(), nodes2);
		IOperationFactory opFactory = MemcachedOperationFactory
				.getFactory(client);
		MemcachedDriver wrapper = new MemcachedDriver(client, noThreads,
				opFactory);

		return wrapper;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	public synchronized void destroy() {
		super.destroy();
		this.mcClient.shutdown(30, TimeUnit.SECONDS);
		MosaicLogger.getLogger().trace("MemcachedDriver destroyed."); //$NON-NLS-1$
	}

	public synchronized IResult<Boolean> invokeSetOperation(String key,
			int exp, Object data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.SET, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAddOperation(String key,
			int exp, Object data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.ADD, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeReplaceOperation(String key,
			int exp, Object data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.REPLACE, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAppendOperation(String key,
			Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.APPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokePrependOperation(String key,
			Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.PREPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeCASOperation(String key,
			Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.CAS, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Map<String, Object>> invokeGetBulkOperation(
			List<String> keys,
			IOperationCompletionHandler<Map<String, Object>> complHandler) {
		String[] aKeys = keys.toArray(new String[0]);

		@SuppressWarnings("unchecked")
		GenericOperation<Map<String, Object>> op = (GenericOperation<Map<String, Object>>) super
				.getOperationFactory(MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.GET_BULK, (Object[]) aKeys);

		IResult<Map<String, Object>> iResult = startOperation(op, complHandler);
		return iResult;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T extends Object> IResult<T> startOperation(
			GenericOperation<T> op, IOperationCompletionHandler complHandler) {
		IResult<T> iResult = new GenericResult<T>(op);
		op.setHandler(complHandler);
		super.addPendingOperation(iResult);

		super.submitOperation(op.getOperation());
		return iResult;
	}

}
