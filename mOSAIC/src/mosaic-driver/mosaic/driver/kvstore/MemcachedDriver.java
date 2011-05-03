package mosaic.driver.kvstore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.GenericResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.IResourceDriver;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

/**
 * Driver class for the memcached-compatible key-value database management
 * systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedDriver implements IResourceDriver {

	private MemcachedClient mcClient;
	private List<IResult<?>> pendingResults;
	private ExecutorService executor;
	private MemcachedOperationFactory opFactory;

	private MemcachedDriver(MemcachedClient client, int noThreads) {
		this.mcClient = client;
		this.pendingResults = new ArrayList<IResult<?>>();
		this.executor = Executors.newFixedThreadPool(noThreads);
		this.opFactory = MemcachedOperationFactory.getFactory(client);
	}

	/**
	 * Creates a new driver.
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
	 * @return the new driver
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
					"memcached.host_" + noNodes, String.class, "");
			if (host.equals("")) {
				noNodes--;
				break;
			}
			port = ConfigUtils.resolveParameter(config, "memcached.port_"
					+ noNodes, Integer.class, 0);
			address = new InetSocketAddress(host, port);
			nodes.add(address);
		}
		noThreads = ConfigUtils.resolveParameter(config,
				"memcached.driver_threads", Integer.class, 1);

		MemcachedDriver wrapper = new MemcachedDriver(new MemcachedClient(
				new BinaryConnectionFactory(), nodes), noThreads);

		return wrapper;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	public void destroy() {
		IResult<?> pResult;

		// cancel all pending operations
		Iterator<IResult<?>> it = this.pendingResults.iterator();
		while (it.hasNext()) {
			pResult = it.next();
			pResult.cancel();
			it.remove();
		}
		this.mcClient.shutdown(30, TimeUnit.SECONDS);
		this.executor.shutdown();
	}

	public synchronized IResult<Boolean> invokeSetOperation(String key,
			int exp, Object data, IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.SET, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAddOperation(String key,
			int exp, Object data, IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.ADD, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeReplaceOperation(String key,
			int exp, Object data, IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.REPLACE, key, exp, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeAppendOperation(String key,
			Object data, IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.APPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokePrependOperation(String key,
			Object data, IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.PREPEND, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeCASOperation(String key,
			Object data, IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.CAS, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Object> invokeGetOperation(String key,
			IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Object> op = (GenericOperation<Object>) this.opFactory
				.getOperation(MemcachedOperations.GET, key);

		IResult<Object> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Map<String, Object>> invokeGetBulkOperation(
			List<String> keys, IOperationCompletionHandler complHandler) {
		String[] aKeys = keys.toArray(new String[0]);

		@SuppressWarnings("unchecked")
		GenericOperation<Map<String, Object>> op = (GenericOperation<Map<String, Object>>) this.opFactory
				.getOperation(MemcachedOperations.GET_BULK, (Object[]) aKeys);

		IResult<Map<String, Object>> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeDeleteOperation(String key,
			IOperationCompletionHandler complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(MemcachedOperations.DELETE, key);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	private <T extends Object> IResult<T> startOperation(
			GenericOperation<T> op, IOperationCompletionHandler complHandler) {
		IResult<T> iResult = new GenericResult<T>(op);
		op.setHandler(complHandler);
		synchronized (pendingResults) {
			pendingResults.add(iResult);
		}

		executor.submit(op.getOperation());
		return iResult;
	}

	public synchronized int countPendingOperations() {
		return this.pendingResults.size();
	}

	public synchronized void removePendingOperation(IResult<?> pendingOp) {
		this.pendingResults.remove(pendingOp);
	}

}
