/*
 * #%L
 * mosaic-drivers
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
package eu.mosaic_cloud.driver.kvstore;

import java.io.IOException;

import eu.mosaic_cloud.core.configuration.ConfigUtils;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.ops.IOperationFactory;
import eu.mosaic_cloud.driver.ConfigProperties;


/**
 * Driver class for the Redis key-value database management systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class RedisDriver extends AbstractKeyValueDriver {

	private final String host;
	private final int port;
	private String password;

	/**
	 * Creates a new Redis driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param host
	 *            the hostname of the Redis server
	 * @param port
	 *            the port for the Redis server
	 */
	private RedisDriver(int noThreads, String host, int port) {
		super(noThreads);
		this.host = host;
		this.port = port;
	}

	/**
	 * Creates a new Redis driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param host
	 *            the hostname of the Redis server
	 * @param port
	 *            the port for the Redis server
	 * @param passwd
	 *            the password for connecting to the server
	 */
	private RedisDriver(int noThreads, String host, int port, String password) {
		super(noThreads);
		this.host = host;
		this.port = port;
		this.password = password;
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
	public static RedisDriver create(IConfiguration config) throws IOException {
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

		String passwd = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.4"), //$NON-NLS-1$
				String.class, ""); //$NON-NLS-1$

		return new RedisDriver(noThreads, host, port, passwd);
	}

	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	@Override
	public void destroy() {
		super.destroy();
		MosaicLogger.getLogger().trace("RedisDriver destroyed."); //$NON-NLS-1$
	}

	@Override
	protected IOperationFactory createOperationFactory(Object... params) {
		String bucket = params[0].toString();
		IOperationFactory opFactory = RedisOperationFactory.getFactory(
				this.host, this.port, this.password, bucket);
		return opFactory;
	}

}
