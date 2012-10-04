/*
 * #%L
 * mosaic-drivers-stubs-memcache
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

package eu.mosaic_cloud.drivers.kvstore;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.GenericResult;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.utils.EncodingMetadata;
import eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


/**
 * Driver class for the memcached-compatible key-value database management
 * systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class MemcachedDriver
		extends AbstractKeyValueDriver
{
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
	private MemcachedDriver (final ThreadingContext threading, final int noThreads, final List<?> hosts, final String user, final String password)
	{
		super (threading, noThreads);
		this.hosts = hosts;
		this.username = user;
		this.password = password;
	}
	
	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	@Override
	public synchronized void destroy ()
	{
		super.destroy ();
		this.logger.trace ("MemcachedDriver destroyed."); // $NON-NLS-1$
	}
	
	public IResult<Boolean> invokeAddOperation (final String clientId, final KeyValueMessage kvMessage, final int exp, final IOperationCompletionHandler<Boolean> complHandler)
	{
		@SuppressWarnings ("unchecked") final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super.getOperationFactory (clientId, MemcachedOperationFactory.class).getOperation (KeyValueOperations.ADD, kvMessage, exp);
		return this.startOperation (operation, complHandler);
	}
	
	public IResult<Boolean> invokeAppendOperation (final String clientId, final KeyValueMessage kvMessage, final IOperationCompletionHandler<Boolean> complHandler)
	{
		@SuppressWarnings ("unchecked") final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super.getOperationFactory (clientId, MemcachedOperationFactory.class).getOperation (KeyValueOperations.APPEND, kvMessage);
		return this.startOperation (operation, complHandler);
	}
	
	public IResult<Boolean> invokeCASOperation (final String clientId, final KeyValueMessage kvMessage, final IOperationCompletionHandler<Boolean> complHandler)
	{
		@SuppressWarnings ("unchecked") final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super.getOperationFactory (clientId, MemcachedOperationFactory.class).getOperation (KeyValueOperations.CAS, kvMessage);
		return this.startOperation (operation, complHandler);
	}
	
	public IResult<Map<String, KeyValueMessage>> invokeGetBulkOperation (final String clientId, final List<String> keys, final EncodingMetadata expectedEncoding, final IOperationCompletionHandler<Map<String, KeyValueMessage>> complHandler)
	{
		final String[] aKeys = keys.toArray (new String[keys.size ()]);
		@SuppressWarnings ("unchecked") final GenericOperation<Map<String, KeyValueMessage>> operation = (GenericOperation<Map<String, KeyValueMessage>>) super.getOperationFactory (clientId, MemcachedOperationFactory.class).getOperation (KeyValueOperations.GET_BULK, (Object[]) aKeys, expectedEncoding);
		return this.startOperation (operation, complHandler);
	}
	
	public IResult<Boolean> invokePrependOperation (final String clientId, final KeyValueMessage kvMessage, final IOperationCompletionHandler<Boolean> complHandler)
	{
		@SuppressWarnings ("unchecked") final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super.getOperationFactory (clientId, MemcachedOperationFactory.class).getOperation (KeyValueOperations.PREPEND, kvMessage);
		return this.startOperation (operation, complHandler);
	}
	
	public IResult<Boolean> invokeReplaceOperation (final String clientId, final KeyValueMessage kvMessage, final int exp, final IOperationCompletionHandler<Boolean> complHandler)
	{
		@SuppressWarnings ("unchecked") final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super.getOperationFactory (clientId, MemcachedOperationFactory.class).getOperation (KeyValueOperations.REPLACE, kvMessage, exp);
		return this.startOperation (operation, complHandler);
	}
	
	public IResult<Boolean> invokeSetOperation (final String clientId, final KeyValueMessage kvMessage, final int exp, final IOperationCompletionHandler<Boolean> complHandler)
	{
		@SuppressWarnings ("unchecked") final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) super.getOperationFactory (clientId, MemcachedOperationFactory.class).getOperation (KeyValueOperations.SET, kvMessage, exp);
		return this.startOperation (operation, complHandler);
	}
	
	@Override
	protected IOperationFactory createOperationFactory (final Object ... params)
	{
		final String bucket = params[0].toString ();
		final IOperationFactory opFactory = MemcachedOperationFactory.getFactory (this.hosts, this.username, this.password, bucket, MemcachedDriver.USE_BUCKET);
		return opFactory;
	}
	
	@SuppressWarnings ({"rawtypes", "unchecked"})
	private <T extends Object> IResult<T> startOperation (final GenericOperation<T> operation, final IOperationCompletionHandler complHandler)
	{
		final IResult<T> iResult = new GenericResult<T> (operation);
		operation.setHandler (complHandler);
		super.addPendingOperation (iResult);
		super.submitOperation (operation.getOperation ());
		return iResult;
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
	public static MemcachedDriver create (final IConfiguration config, final ThreadingContext threading)
	{
		final List<URI> nodesURI = new ArrayList<URI> (10);
		int noNodes = 0;
		int port, noThreads;
		final List<String> hosts = new ArrayList<String> (10);
		final List<Integer> ports = new ArrayList<Integer> (10);
		MemcachedDriver driver;
		while (true) {
			noNodes++;
			final String host = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("MemcachedDriver.0") + noNodes, String.class, ""); // $NON-NLS-1$ $NON-NLS-2$
			if ("".equals (host)) { // $NON-NLS-1$
				noNodes--;
				break;
			}
			port = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("MemcachedDriver.1") + noNodes, Integer.class, 0); // $NON-NLS-1$
			hosts.add (host);
			ports.add (port);
		}
		for (int index = 0; index < noNodes; index++) {
			try {
				final URI address = new URI ("http://" + hosts.get (index) + ":" + ports.get (index) + "/pools");
				nodesURI.add (address);
			} catch (final URISyntaxException e) {
				FallbackExceptionTracer.defaultInstance.traceIgnoredException (e);
			}
		}
		noThreads = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KVStoreDriver.2"), Integer.class, 1); // $NON-NLS-1$
		final String user = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KVStoreDriver.5"), String.class, ""); // $NON-NLS-1$ $NON-NLS-2$
		final String passwd = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KVStoreDriver.4"), String.class, ""); // $NON-NLS-1$ $NON-NLS-2$
		driver = new MemcachedDriver (threading, noThreads, nodesURI, user, passwd);
		return driver;
	}
	
	private final List<?> hosts;
	private final String password;
	private final String username;
	public static final String DEFAULT_CONTENT_ENCODING = "default";
	public static final String DEFAULT_CONTENT_TYPE = "text/plain";
	private final static boolean USE_BUCKET = true;
}
