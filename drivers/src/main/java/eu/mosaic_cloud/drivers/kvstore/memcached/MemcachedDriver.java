/*
 * #%L
 * mosaic-drivers
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package eu.mosaic_cloud.drivers.kvstore.memcached;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.drivers.kvstore.AbstractKeyValueDriver;
import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.GenericResult;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * Driver class for the memcached-compatible key-value database management
 * systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class MemcachedDriver extends AbstractKeyValueDriver { // NOPMD by
																	// georgiana
																	// on
																	// 10/12/11
																	// 10:07 AM

	private final static boolean USE_BUCKET = false;
	private final List<?> hosts;
	private final String username;
	private final String password;

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
	private MemcachedDriver(ThreadingContext threading, int noThreads,
			List<?> hosts, String user, String password) {
		super(threading, noThreads);
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
	public static MemcachedDriver create(IConfiguration config,
			ThreadingContext threading) throws IOException {
		List<URI> nodesURI = new ArrayList<URI>(10); // NOPMD by georgiana on
														// 10/12/11 12:51 PM
		int noNodes = 0; // NOPMD by georgiana on 10/12/11 10:07 AM
		int port, noThreads;
		List<String> hosts = new ArrayList<String>(10); // NOPMD by georgiana on
														// 10/12/11 12:51 PM
		List<Integer> ports = new ArrayList<Integer>(10); // NOPMD by georgiana
															// on 10/12/11 12:51
															// PM
		MemcachedDriver driver;

		while (true) {
			noNodes++;
			String host = ConfigUtils
					.resolveParameter(
							config,
							ConfigProperties.getString("MemcachedDriver.0") + noNodes, String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
			if ("".equals(host)) { //$NON-NLS-1$
				noNodes--; // NOPMD by georgiana on 10/12/11 10:08 AM
				break;
			}
			port = ConfigUtils.resolveParameter(config,
					ConfigProperties.getString("MemcachedDriver.1") //$NON-NLS-1$
							+ noNodes, Integer.class, 0);

			hosts.add(host);
			ports.add(port);
		}

		for (int index = 0; index < noNodes; index++) {
			try {
				URI address = new URI("http://" + hosts.get(index) + ":" // NOPMD
																			// by
																			// georgiana
																			// on
																			// 10/12/11
																			// 12:50
																			// PM
						+ ports.get(index) + "/pools");
				nodesURI.add(address);
			} catch (URISyntaxException e) {
				ExceptionTracer.traceIgnored(e);
			}
		}

		noThreads = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KVStoreDriver.2"), Integer.class, 1); //$NON-NLS-1$
		// String bucket = ConfigUtils
		// .resolveParameter(
		// config,
		//						ConfigProperties.getString("KVStoreDriver.3"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
		String user = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.5"), //$NON-NLS-1$
				String.class, ""); //$NON-NLS-1$
		String passwd = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KVStoreDriver.4"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$

		driver = new MemcachedDriver(threading, noThreads, nodesURI, user,
				passwd);
		return driver;
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	@Override
	public synchronized void destroy() {
		super.destroy();
		this.logger.trace("MemcachedDriver destroyed."); //$NON-NLS-1$
	}

	public IResult<Boolean> invokeSetOperation(String clientId, String key,
			int exp, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.SET, key, exp, data);
		return startOperation(operation, complHandler);
	}

	public IResult<Boolean> invokeAddOperation(String clientId, String key,
			int exp, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.ADD, key, exp, data);
		return startOperation(operation, complHandler);
	}

	public IResult<Boolean> invokeReplaceOperation(String clientId, String key,
			int exp, byte[] data,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.REPLACE, key, exp, data);
		return startOperation(operation, complHandler);
	}

	public IResult<Boolean> invokeAppendOperation(String clientId, String key,
			byte[] data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.APPEND, key, data);

		return startOperation(operation, complHandler);
	}

	public IResult<Boolean> invokePrependOperation(String clientId, String key,
			byte[] data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.PREPEND, key, data);
		return startOperation(operation, complHandler);
	}

	public IResult<Boolean> invokeCASOperation(String clientId, String key,
			byte[] data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.CAS, key, data);

		return startOperation(operation, complHandler);
	}

	public IResult<Map<String, byte[]>> invokeGetBulkOperation(String clientId,
			List<String> keys,
			IOperationCompletionHandler<Map<String, byte[]>> complHandler) {
		String[] aKeys = keys.toArray(new String[keys.size()]);

		@SuppressWarnings("unchecked")
		GenericOperation<Map<String, byte[]>> operation = (GenericOperation<Map<String, byte[]>>) super
				.getOperationFactory(clientId, MemcachedOperationFactory.class)
				.getOperation(KeyValueOperations.GET_BULK, (Object[]) aKeys);

		return startOperation(operation, complHandler);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T extends Object> IResult<T> startOperation(
			GenericOperation<T> operation,
			IOperationCompletionHandler complHandler) {
		IResult<T> iResult = new GenericResult<T>(operation);
		operation.setHandler(complHandler);
		super.addPendingOperation(iResult);

		super.submitOperation(operation.getOperation());
		return iResult;
	}

	@Override
	protected IOperationFactory createOperationFactory(Object... params) {
		String bucket = params[0].toString();
		IOperationFactory opFactory = MemcachedOperationFactory.getFactory(
				this.hosts, this.username, this.password, bucket,
				MemcachedDriver.USE_BUCKET);
		return opFactory;
	}

}
