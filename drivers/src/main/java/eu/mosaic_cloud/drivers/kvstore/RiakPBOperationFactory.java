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

package eu.mosaic_cloud.drivers.kvstore;

import static com.google.protobuf.ByteString.copyFromUtf8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.IOperation;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IOperationType;

import com.basho.riak.pbc.KeySource;
import com.basho.riak.pbc.RiakClient;
import com.basho.riak.pbc.RiakObject;
import com.google.protobuf.ByteString;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Riak key-value store.
 * 
 * @author Carmine Di Biase, Georgiana Macariu
 * 
 */
public final class RiakPBOperationFactory implements IOperationFactory { // NOPMD

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
    public static RiakPBOperationFactory getFactory(String riakHost, int port, String bucket) {
        RiakPBOperationFactory factory = null; // NOPMD by georgiana on 10/12/11
                                               // 4:49 PM
        try {
            factory = new RiakPBOperationFactory(riakHost, port, bucket);
            final MosaicLogger sLogger = MosaicLogger.createLogger(RiakRestOperationFactory.class);
            sLogger.trace("Created Riak PB factory for " + riakHost + ":" + port + " bucket "
                    + bucket);
        } catch (final IOException e) {
            ExceptionTracer.traceIgnored(e);
        }
        return factory;
    }

    // by
    // georgiana
    // on
    // 10/12/11
    // 4:49
    // PM
    private final RiakClient riakcl;

    private final String bucket;

    private RiakPBOperationFactory(String riakHost, int port, String bucket) throws IOException {
        super();
        this.riakcl = new RiakClient(riakHost, port);
        this.bucket = bucket;
    }

    private IOperation<?> buildDeleteOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws IOException {
                final String key = (String) parameters[0];
                RiakPBOperationFactory.this.riakcl.delete(RiakPBOperationFactory.this.bucket, key);
                return true;
            }
        });
    }

    private IOperation<?> buildGetOperation(final Object... parameters) {
        return new GenericOperation<byte[]>(new Callable<byte[]>() {

            @Override
            public byte[] call() throws IOException {
                byte[] result = null;
                final String key = (String) parameters[0];
                final RiakObject[] riakobj = RiakPBOperationFactory.this.riakcl.fetch(
                        RiakPBOperationFactory.this.bucket, key);
                if (riakobj.length == 1) {
                    result = riakobj[0].getValue().toByteArray();
                }
                return result;
            }
        });
    }

    private IOperation<?> buildListOperation() {
        return new GenericOperation<List<String>>(new Callable<List<String>>() {

            @Override
            public List<String> call() throws IOException {
                KeySource keyStore;
                keyStore = RiakPBOperationFactory.this.riakcl
                        .listKeys(copyFromUtf8(RiakPBOperationFactory.this.bucket));
                final List<String> keys = new ArrayList<String>();
                while (keyStore.hasNext()) {
                    keys.add(keyStore.next().toStringUtf8());
                }
                return keys;
            }
        });
    }

    private IOperation<?> buildSetOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws IOException {
                final String key = (String) parameters[0];
                final byte[] dataBytes = (byte[]) parameters[1];
                final ByteString keyBS = ByteString.copyFromUtf8(key);
                final ByteString bucketBS = ByteString
                        .copyFromUtf8(RiakPBOperationFactory.this.bucket);
                final ByteString dataBS = ByteString.copyFrom(dataBytes);
                final RiakObject riakobj = new RiakObject(bucketBS, keyBS, dataBS);
                RiakPBOperationFactory.this.riakcl.store(riakobj);
                return true;
            }
        });
    }

    @Override
    public void destroy() {
        // NOTE: nothing to do here
    }

    @Override
    public IOperation<?> getOperation(final IOperationType type, // NOPMD by
                                                                 // georgiana
                                                                 // on
                                                                 // 10/12/11
                                                                 // 4:49 PM
            Object... parameters) {
        IOperation<?> operation;
        if (!(type instanceof KeyValueOperations)) {
            return new GenericOperation<Object>(new Callable<Object>() { // NOPMD

                        // by
                        // georgiana
                        // on
                        // 10/12/11
                        // 4:49
                        // PM
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
                operation = buildSetOperation(parameters); // NOPMD by georgiana
                                                           // on 10/12/11 4:49
                                                           // PM
                break;
            case GET:
                operation = buildGetOperation(parameters); // NOPMD by georgiana
                                                           // on 10/12/11 4:49
                                                           // PM
                break;
            case LIST:
                operation = buildListOperation(); // NOPMD by georgiana on
                                                  // 10/12/11 4:49 PM
                break;
            case DELETE:
                operation = buildDeleteOperation(parameters); // NOPMD by
                                                              // georgiana on
                                                              // 10/12/11 4:49
                                                              // PM
                break;
            default:
                operation = new GenericOperation<Object>( // NOPMD by georgiana
                                                          // on 10/12/11 4:48
                                                          // PM
                        new Callable<Object>() {

                            @Override
                            public Object call() throws UnsupportedOperationException {
                                throw new UnsupportedOperationException("Unsupported operation: "
                                        + mType.toString());
                            }
                        });
            }
        } catch (final Exception e) {
            ExceptionTracer.traceDeferred(e);
            operation = new GenericOperation<Object>(new Callable<Object>() { // NOPMD

                        // by
                        // georgiana
                        // on
                        // 10/12/11
                        // 4:48
                        // PM
                        @Override
                        public Object call() throws Exception { // NOPMD by
                                                                // georgiana on
                                                                // 10/12/11 4:49
                                                                // PM
                            throw e;
                        }
                    });
        }
        return operation;
    }
}
