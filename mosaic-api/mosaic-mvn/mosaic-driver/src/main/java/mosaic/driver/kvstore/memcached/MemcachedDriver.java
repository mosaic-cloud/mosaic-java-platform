package mosaic.driver.kvstore.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

/**
 * Driver class for the memcached-compatible key-value database management
 * systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedDriver extends BaseKeyValueDriver {
	private List<InetSocketAddress> hosts;
	private String username;
	private String password;

	/**
	 * Creates a new memcached driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param hosts
	 *            the hostname and port of the Memcached servers
	 * @param user
	 *            the username for connecting to the server
	 * @param passwd
	 *            the password for connecting to the server
	 */
	private MemcachedDriver(int noThreads, List<InetSocketAddress> hosts,
			String user, String password) {
		super(noThreads);
		this.hosts = hosts;
		this.username = user;
		this.password = password;
	}

	/**
	 * Returns a Memcached driver.
	 * 
	 * @param config
	 *            the configuration parameters required by the driver:
	 *            <ol>
	 *            <il>for each server to which the driver should connect there
	 *            should be two parameters:
	 *            <i>memcached.host_&lt;server_number&gt;</i> and
	 *            <i>memcached.port_&lt;server_number&gt;</i> indicating the
	 *            hostnames and the ports where the servers are installed </il>
	 *            <il><i>kvstore.driver_threads</i> specifies the maximum number
	 *            of threads that shall be created by the driver for serving
	 *            requests </il>
	 *            </ol>
	 * @return the driver
	 * @throws IOException
	 */
	public static synchronized MemcachedDriver create(IConfiguration config)
			throws IOException {
//		List<URI> nodes = new ArrayList<URI>();
		List<InetSocketAddress> nodes2 = new ArrayList<InetSocketAddress>();
		int noNodes = 0;
//		URI address;
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
		String user = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.5"), //$NON-NLS-1$
				String.class, ""); //$NON-NLS-1$
		String passwd = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KVStoreDriver.4"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$

		MemcachedDriver wrapper = new MemcachedDriver(noThreads, nodes2, user,
				passwd);

		return wrapper;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	@Override
	public synchronized void destroy() {
		super.destroy();
		MosaicLogger.getLogger().trace("MemcachedDriver destroyed."); //$NON-NLS-1$
	}

	public synchronized IResult<Boolean> invokeSetOperation(String clientId,
			String key, int exp, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.SET, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAddOperation(String clientId,
			String key, int exp, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.ADD, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeReplaceOperation(
			String clientId, String key, int exp, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.REPLACE, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAppendOperation(String clientId,
			String key, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.APPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokePrependOperation(
			String clientId, String key, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.PREPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeCASOperation(String clientId,
			String key, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.CAS, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Map<String, byte[]>> invokeGetBulkOperation(
			String clientId, List<String> keys,
			IOperationCompletionHandler<Map<String, byte[]>> complHandler) {
		String[] aKeys = keys.toArray(new String[0]);

		@SuppressWarnings("unchecked")
		GenericOperation<Map<String, byte[]>> op = (GenericOperation<Map<String, byte[]>>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.GET_BULK, (Object[]) aKeys);

		IResult<Map<String, byte[]>> iResult = startOperation(op, complHandler);
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

	@Override
	protected IOperationFactory createOperationFactory(Object... params) {
		String bucket = params[0].toString();
		IOperationFactory opFactory = MemcachedOperationFactory.getFactory(
				this.hosts, this.username, this.password, bucket);
		return opFactory;
	}

}
