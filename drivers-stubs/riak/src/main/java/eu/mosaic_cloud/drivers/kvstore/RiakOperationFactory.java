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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.IOperation;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IOperationType;
import eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.bucket.RiakBucket;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.UnresolvedConflictException;
import com.basho.riak.client.convert.ConversionException;
import com.basho.riak.client.raw.config.Configuration;
import com.basho.riak.client.raw.http.HTTPClientConfig;
import com.basho.riak.client.raw.pbc.PBClientConfig;
import com.google.common.base.Charsets;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Riak key-value store.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class RiakOperationFactory implements IOperationFactory {

    private final Bucket bucket;

    @SuppressWarnings("unused")
    private final String clientId;

    private final BaseExceptionTracer exceptions;

    private final IRiakClient riakcl;

    private static final Logger logger = Transcript.create(RiakOperationFactory.class).adaptAs(
            Logger.class);

    private RiakOperationFactory(final Configuration config, final String bucket,
            final String clientId) throws RiakException {
        super();
        this.riakcl = RiakFactory.newClient(config);
        this.bucket = this.riakcl.fetchBucket(bucket).execute();
        ;
        this.clientId = clientId;
        this.exceptions = FallbackExceptionTracer.defaultInstance;
    }

    /**
     * Creates a new factory.
     * 
     * @param riakHost
     *            the hostname of the Riak server
     * @param port
     *            the port for the Riak server
     * @param bucket
     *            the bucket associated with the connection
     * @return the factory
     */
    public static IOperationFactory getFactory(final String riakHost, final int port,
            final String bucket, final String clientId, final boolean pb) {
        IOperationFactory factory = null;
        try {
            Configuration config = null;
            if (pb) {
                config = new PBClientConfig.Builder().withHost(riakHost).withPort(port).build();
            } else {
                config = new HTTPClientConfig.Builder().withHost(riakHost).withPort(port).build();
            }
            factory = new RiakOperationFactory(config, bucket, clientId);
            RiakOperationFactory.logger.trace("Created Riak PB factory for " + riakHost + ":"
                    + port + " bucket " + bucket);
        } catch (RiakException e) {
            FallbackExceptionTracer.defaultInstance.traceIgnoredException(e);
        }
        return factory;
    }

    private IOperation<?> buildDeleteOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws IOException {
                final String key = (String) parameters[0];
                // FIXME: use the vector clock...
                try {
                    RiakOperationFactory.this.bucket.delete(key).execute();
                } catch (RiakException e) {
                    // TODO: shutdown all connectors for this bucket?
                    FallbackExceptionTracer.defaultInstance.traceIgnoredException(e);
                    return false;
                }
                return true;
            }
        });
    }

    private IOperation<?> buildGetOperation(final Object... parameters) {
        return new GenericOperation<KeyValueMessage>(new Callable<KeyValueMessage>() {

            @Override
            public KeyValueMessage call() throws IOException {
                KeyValueMessage result = null;
                final String key = (String) parameters[0];
                // FIXME: use the vector clock...
                IRiakObject riakObj = null;
                try {
                    riakObj = RiakOperationFactory.this.bucket.fetch(key).execute();
                } catch (UnresolvedConflictException e) {
                    FallbackExceptionTracer.defaultInstance.traceIgnoredException(e);
                } catch (RiakRetryFailedException e) {
                    // TODO: shutdown all connectors for this bucket?
                    FallbackExceptionTracer.defaultInstance.traceIgnoredException(e);
                } catch (ConversionException e) {
                    FallbackExceptionTracer.defaultInstance.traceIgnoredException(e);
                }
                if (null != riakObj) {
                    result = new KeyValueMessage(key, riakObj.getValue(),
                            riakObj.getUsermeta(CONTENT_ENCODING), riakObj.getContentType());
                }

                return result;
            }
        });
    }

    private IOperation<?> buildListOperation() {
        return new GenericOperation<List<String>>(new Callable<List<String>>() {

            @Override
            public List<String> call() throws IOException {
                // FIXME: use the vector clock...
                final List<String> keys = new ArrayList<String>();
                try {
                    Iterator<String> keyStore = RiakOperationFactory.this.bucket.keys().iterator();
                    while (keyStore.hasNext()) {
                        keys.add(keyStore.next());
                    }
                } catch (RiakException e) {
                    // TODO: shutdown all connectors for this bucket?
                    FallbackExceptionTracer.defaultInstance.traceIgnoredException(e);
                }

                return keys;
            }
        });
    }

    private IOperation<?> buildSetOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws IOException {
                final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
                final String key = kvMessage.getKey();

                // FIXME: use the vector clock...
                IRiakObject riakObject = RiakObjectBuilder
                        .newBuilder(RiakOperationFactory.this.bucket.getName(), key)
                        .addUsermeta(CONTENT_ENCODING, kvMessage.getContentEncoding())
                        .withContentType(kvMessage.getContentType()).withValue(kvMessage.getData())
                        .build();
                RiakBucket riakBucket = RiakBucket.newRiakBucket(RiakOperationFactory.this.bucket);
                try {
                    riakBucket.store(riakObject);
                } catch (RiakException e) {
                    // TODO: shutdown all connectors for this bucket?
                    FallbackExceptionTracer.defaultInstance.traceIgnoredException(e);
                    return false;
                }
                return true;
            }
        });
    }

    @Override
    public void destroy() {
        // NOTE: nothing to do here
    }

    @Override
    public IOperation<?> getOperation(final IOperationType type, final Object... parameters) {
        IOperation<?> operation;
        if (!(type instanceof KeyValueOperations)) {
            return new GenericOperation<Object>(new Callable<Object>() {

                @Override
                public Object call() throws UnsupportedOperationException {
                    throw new UnsupportedOperationException("Unsupported operation: "
                            + type.toString());
                }
            });
        }
        final KeyValueOperations mType = (KeyValueOperations) type;
        try {
            switch (mType) {
            case SET:
                operation = this.buildSetOperation(parameters);
                break;
            case GET:
                operation = this.buildGetOperation(parameters);
                break;
            case LIST:
                operation = this.buildListOperation();
                break;
            case DELETE:
                operation = this.buildDeleteOperation(parameters);
                break;
            default:
                operation = new GenericOperation<Object>(new Callable<Object>() {

                    @Override
                    public Object call() throws UnsupportedOperationException {
                        throw new UnsupportedOperationException("Unsupported operation: "
                                + mType.toString());
                    }
                });
            }
        } catch (final Exception e) {
            this.exceptions.traceDeferredException(e);
            operation = new GenericOperation<Object>(new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    throw e;
                }
            });
        }
        return operation;
    }
}
