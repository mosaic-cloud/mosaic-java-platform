/*
 * #%L
 * mosaic-drivers-stubs-riak
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

import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * Driver class for the Riak key-value database management systems.
 * 
 * Rest Interface
 * 
 * @author Carmine Di Biase
 * 
 */
public final class RiakRestDriver extends AbstractKeyValueDriver {

    private final String riakHost;

    private final int riakPort;

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
    private RiakRestDriver(ThreadingContext threading, int noThreads, String riakHost, int riakPort) {
        super(threading, noThreads);
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
    public static RiakRestDriver create(IConfiguration config, ThreadingContext threading) {
        int port, noThreads;
        final String host = ConfigUtils.resolveParameter(config,
                ConfigProperties.getString("KVStoreDriver.0"), //$NON-NLS-1$
                String.class, ""); //$NON-NLS-1$
        port = ConfigUtils.resolveParameter(config, ConfigProperties.getString("KVStoreDriver.1"), //$NON-NLS-1$
                Integer.class, 0);
        noThreads = ConfigUtils.resolveParameter(config,
                ConfigProperties.getString("KVStoreDriver.2"), Integer.class, 1); //$NON-NLS-1$
        final MosaicLogger sLogger = MosaicLogger.createLogger(RiakRestDriver.class);
        sLogger.trace("Created Riak REST driver for host " + host + ":" + port + " [threads="
                + noThreads + "]");
        return new RiakRestDriver(threading, noThreads, host, port);
    }

    @Override
    protected IOperationFactory createOperationFactory(Object... params) {
        final String bucket = (String) params[0];
        final IOperationFactory opFactory = RiakRestOperationFactory.getFactory(this.riakHost,
                this.riakPort, bucket);
        return opFactory;
    }

    /**
     * Destroys the driver. After this call no other method should be invoked on
     * the driver object.
     */
    @Override
    public synchronized void destroy() {
        super.destroy();
        this.logger.trace("RiakDriver destroyed."); //$NON-NLS-1$
    }
    /*
     * Here is eventually possible add more particular operation for your key
     * value store engine
     */
}
