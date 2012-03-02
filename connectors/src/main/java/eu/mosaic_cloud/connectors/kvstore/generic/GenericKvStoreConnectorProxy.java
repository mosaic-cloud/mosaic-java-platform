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

import eu.mosaic_cloud.connectors.core.ConfigProperties;
import eu.mosaic_cloud.connectors.kvstore.BaseKvStoreConnectorProxy;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Resolver;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

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
            final Channel channel, final Resolver resolver,
            final ThreadingContext threading, final ExceptionTracer exceptions,
            final DataEncoder<? super T> encoder) {
        super(configuration, channel, resolver, threading, exceptions, encoder);
        final String bucket = ConfigUtils.resolveParameter(configuration,
                ConfigProperties.getString("GenericKvStoreConnector.1"), String.class, "");
        // FIXME
        final String driverIdentity = null;
        channel.register(KeyValueSession.CONNECTOR);
        final InitRequest.Builder requestBuilder = InitRequest.newBuilder();
        requestBuilder.setToken(this.generateToken());
        requestBuilder.setBucket(bucket);
        this.connect(driverIdentity, KeyValueSession.CONNECTOR, new Message(
                KeyValueMessage.ACCESS, requestBuilder.build()));
    }

    @Override
    public CallbackCompletion<Void> initialize() {
        return CallbackCompletion.createOutcome();
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
    public static <T extends Object> GenericKvStoreConnectorProxy<T> create(
            final IConfiguration configuration,
            final Channel channel, final Resolver resolver,
            final ThreadingContext threading, final ExceptionTracer exceptions,
            final DataEncoder<? super T> encoder) {
        final GenericKvStoreConnectorProxy<T> proxy = new GenericKvStoreConnectorProxy<T>(
                configuration, channel, resolver, threading, exceptions, encoder);
        return proxy;
    }
}
