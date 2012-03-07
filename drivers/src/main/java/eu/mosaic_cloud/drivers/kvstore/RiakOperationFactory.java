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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.IOperation;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IOperationType;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.bucket.Bucket;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Riak key-value store.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class RiakOperationFactory implements IOperationFactory { // NOPMD

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
     * @throws RiakException
     */
    public static RiakOperationFactory getFactory(String riakHost, int port, String bucket,
            boolean restCl) throws RiakException {
        return new RiakOperationFactory(riakHost, port, bucket, restCl);
    }

    // by
    // georgiana
    // on
    // 10/12/11
    // 4:46
    // PM
    private final IRiakClient riakcl;
    private final Bucket bucket;

    private RiakOperationFactory(String riakHost, int riakPort, String bucket, boolean restCl)
            throws RiakException {
        super();
        if (restCl == true) {
            final String address = "http://" + riakHost + ":" + riakPort + "/riak";
            this.riakcl = RiakFactory.httpClient(address);
        } else {
            this.riakcl = RiakFactory.pbcClient(riakHost, riakPort);
        }
        this.bucket = this.riakcl.fetchBucket(bucket).execute();
    }

    private IOperation<?> buildDeleteOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() {
                final String key = (String) parameters[0];
                try {
                    RiakOperationFactory.this.bucket.delete(key).execute();
                } catch (final RiakException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });
    }

    private IOperation<?> buildGetOperation(final Object... parameters) {
        return new GenericOperation<byte[]>(new Callable<byte[]>() {

            @Override
            public byte[] call() {
                final String key = (String) parameters[0];
                try {
                    final IRiakObject res = RiakOperationFactory.this.bucket.fetch(key).execute();
                    return res.getValue();
                } catch (final RiakException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    private IOperation<?> buildListOperation() {
        return new GenericOperation<List<String>>(new Callable<List<String>>() {

            @Override
            public List<String> call() {
                Iterable<String> keys;
                try {
                    keys = RiakOperationFactory.this.bucket.keys();
                    final List<String> lkeys = new ArrayList<String>();
                    for (final String key : keys) {
                        lkeys.add(key);
                    }
                    return lkeys;
                } catch (final RiakException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    private IOperation<?> buildSetOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() {
                final String key = (String) parameters[0];
                final byte[] dataBytes = (byte[]) parameters[1];
                try {
                    RiakOperationFactory.this.bucket.store(key, dataBytes).execute();
                    return true;
                } catch (final RiakException e) {
                    e.printStackTrace();
                    return false;
                }
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
                                                                 // 4:46 PM
            Object... parameters) {
        IOperation<?> operation;
        if (!(type instanceof KeyValueOperations)) {
            return new GenericOperation<Object>(new Callable<Object>() { // NOPMD

                        // by
                        // georgiana
                        // on
                        // 10/12/11
                        // 4:46
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
                                                           // on 10/12/11 4:46
                                                           // PM
                break;
            case GET:
                operation = buildGetOperation(parameters); // NOPMD by georgiana
                                                           // on 10/12/11 4:45
                                                           // PM
                break;
            case LIST:
                operation = buildListOperation(); // NOPMD by georgiana on
                                                  // 10/12/11 4:45 PM
                break;
            case DELETE:
                operation = buildDeleteOperation(parameters); // NOPMD by
                                                              // georgiana on
                                                              // 10/12/11 4:45
                                                              // PM
                break;
            default:
                operation = new GenericOperation<Object>( // NOPMD by georgiana
                                                          // on 10/12/11 4:45
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
                        // 4:45
                        // PM
                        @Override
                        public Object call() throws Exception { // NOPMD by
                                                                // georgiana on
                                                                // 10/12/11 4:46
                                                                // PM
                            throw e;
                        }
                    });
        }
        return operation;
    }
}
