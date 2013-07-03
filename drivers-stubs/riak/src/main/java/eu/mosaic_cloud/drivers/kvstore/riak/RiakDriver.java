/*
 * #%L
 * mosaic-drivers-stubs-riak
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.drivers.kvstore.riak;


import java.io.IOException;

import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.drivers.exceptions.ConnectionException;
import eu.mosaic_cloud.drivers.ops.IOperationFactory;
import eu.mosaic_cloud.platform.implementations.v1.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.v1.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;


/**
 * Driver class for the Riak key-value database management systems.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class RiakDriver
		extends AbstractKeyValueDriver
{
	/**
	 * Creates a new Riak driver.
	 * 
	 * @param noThreads
	 *            number of threads to be used for serving requests
	 * @param riakHost
	 *            the hostname of the Riak server
	 * @param riakPort
	 *            the port for the Riak server
	 * @param pb
	 *            whethet the driver uses protocol buffers
	 */
	private RiakDriver (final ThreadingContext threading, final int noThreads, final String riakHost, final int riakPort, final boolean pb)
	{
		super (threading, noThreads);
		this.riakHost = riakHost;
		this.riakPort = riakPort;
		this.usePB = pb;
	}
	
	/**
	 * Destroys the driver. After this call no other method should be invoked on
	 * the driver object.
	 */
	@Override
	public synchronized void destroy ()
	{
		super.destroy ();
		RiakDriver.logger.trace ("RiakDriver destroyed."); // $NON-NLS-1$
	}
	
	/*
	 * Here is eventually possible add more particular operation for your key
	 * value store engine
	 */
	@Override
	protected IOperationFactory createOperationFactory (final Object ... params)
	{
		final String bucket = (String) params[0];
		final String clientId = (String) params[1];
		final IOperationFactory opFactory = RiakOperationFactory.getFactory (this.riakHost, this.riakPort, bucket, clientId, this.usePB);
		return opFactory;
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
	public static RiakDriver create (final IConfiguration config, final ThreadingContext threading)
			throws IOException,
				ConnectionException
	{
		int port, noThreads;
		final String host = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KVStoreDriver.0"), String.class, ""); // $NON-NLS-1$ $NON-NLS-2$
		port = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KVStoreDriver.1"), Integer.class, 0);// $NON-NLS-1$
		noThreads = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KVStoreDriver.2"), Integer.class, 1); // $NON-NLS-1$
		final String driverName = ConfigUtils.resolveParameter (config, ConfigProperties.getString ("KVStoreDriver.6"), String.class, KeyValueDriverFactory.DriverType.RIAKREST.toString ());
		boolean usePb = false;
		if (driverName.equalsIgnoreCase (KeyValueDriverFactory.DriverType.RIAKPB.toString ())) {
			usePb = true;
		}
		RiakDriver.logger.trace ("Created Riak PB driver for host " + host + ":" + port);
		return new RiakDriver (threading, noThreads, host, port, usePb);
	}
	
	private final String riakHost;
	private final int riakPort;
	private final boolean usePB;
	private static final Logger logger = Transcript.create (RiakDriver.class).adaptAs (Logger.class);
}
