/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.kvstore.generic;

import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnectorProxy;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;

/**
 * Proxy for the driver for key-value distributed storage systems. This is used
 * by the {@link GenericKvStoreConnector} to communicate with a key-value store
 * driver.
 * 
 * @author Georgiana Macariu
 * @param <T>
 *            type of stored data
 * 
 */
public final class GenericKvStoreConnectorProxy<T extends Object> extends
        BaseKvStoreConnectorProxy<T> { // NOPMD

    // by
    // georgiana
    // on
    // 2/20/12
    // 5:06
    // PM
    protected GenericKvStoreConnectorProxy(final IConfiguration configuration,
            final Channel channel, final DataEncoder<T> encoder) {
        super(configuration, channel, encoder);
    }

    /**
     * Returns a proxy for key-value distributed storage systems.
     * 
     * @param bucket
     *            the name of the bucket where the connector will operate
     * @param configuration
     *            the configurations required to initialize the proxy
     * @param driverIdentity
     *            the identifier of the driver to which request will be sent
     * @param channel
     *            the channel on which to communicate with the driver
     * @param encoder
     *            encoder used for serializing and deserializing data stored in
     *            the key-value store
     * @return the proxy
     */
    public static <T extends Object> GenericKvStoreConnectorProxy<T> create(final String bucket,
            final IConfiguration configuration, final String driverIdentity, final Channel channel,
            final DataEncoder<T> encoder) {
        final GenericKvStoreConnectorProxy<T> proxy = new GenericKvStoreConnectorProxy<T>(
                configuration, channel, encoder);
        final InitRequest.Builder requestBuilder = InitRequest.newBuilder();
        requestBuilder.setToken(proxy.generateToken());
        requestBuilder.setBucket(bucket);
        proxy.connect(driverIdentity, KeyValueSession.CONNECTOR, new Message(
                KeyValueMessage.ACCESS, requestBuilder.build()));
        return proxy;
    }
}
