package mosaic.driver.kvstore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.GenericResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.AbstractResourceDriver;
import mosaic.driver.ConfigProperties;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

/**
 * Driver class for the memcached-compatible key-value database management
 * systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedDriver extends AbstractResourceDriver {

	private MemcachedClient mcClient;
	private MemcachedOperationFactory opFactory;

	/**
	 * Creates a new memcached driver.
	 * 
	 * @param client
	 *            the memcached client object
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 */
	private MemcachedDriver(MemcachedClient client, int noThreads) {
		super(noThreads);
		this.mcClient = client;
		this.opFactory = MemcachedOperationFactory.getFactory(client);
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
		List<InetSocketAddress> nodes = new ArrayList<InetSocketAddress>();
		int noNodes = 0;
		InetSocketAddress address;
		int port, noThreads;

		while (true) {
			noNodes++;
			String host = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("MemcachedDriver.0") + noNodes, String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (host.equals("")) { //$NON-NLS-1$
				noNodes--;
				break;
			}
			port = ConfigUtils.resolveParameter(config, ConfigProperties.getString("MemcachedDriver.3") //$NON-NLS-1$
					+ noNodes, Integer.class, 0);
			address = new InetSocketAddress(host, port);
			nodes.add(address);
		}
		noThreads = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("MemcachedDriver.4"), Integer.class, 1); //$NON-NLS-1$

		MemcachedDriver wrapper = new MemcachedDriver(new MemcachedClient(
				new BinaryConnectionFactory(), nodes), noThreads);

		return wrapper;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	public void destroy() {
		super.destroy();
		this.mcClient.shutdown(30, TimeUnit.SECONDS);
	}

	public synchronized IResult<Boolean> invokeSetOperation(String key,
			int exp, Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.SET, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAddOperation(String key,
			int exp, Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.ADD, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeReplaceOperation(String key,
			int exp, Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.REPLACE, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAppendOperation(String key,
			Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.APPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokePrependOperation(String key,
			Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.PREPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeCASOperation(String key,
			Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.CAS, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Object> invokeGetOperation(String key,
			IOperationCompletionHandler<Object> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Object> op = (GenericOperation<Object>) this.opFactory
				.getOperation(MemcachedOperations.GET, key);

		IResult<Object> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Map<String, Object>> invokeGetBulkOperation(
			List<String> keys, IOperationCompletionHandler<Map<String, Object>> complHandler) {
		String[] aKeys = keys.toArray(new String[0]);

		@SuppressWarnings("unchecked")
		GenericOperation<Map<String, Object>> op = (GenericOperation<Map<String, Object>>) this.opFactory
				.getOperation(MemcachedOperations.GET_BULK, (Object[]) aKeys);

		IResult<Map<String, Object>> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeDeleteOperation(String key,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.DELETE, key);

		IResult<Boolean> iResult = startOperation(op, complHandler);
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
