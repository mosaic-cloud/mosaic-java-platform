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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.CASResponse;
import net.spy.memcached.MemcachedClient;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;

import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.IOperation;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IOperationType;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * in the memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class MemcachedOperationFactory implements IOperationFactory { // NOPMD

                                                                            // by
                                                                            // georgiana
                                                                            // on
                                                                            // 10/12/11
                                                                            // 4:57
                                                                            // PM
    private final MemcachedClient mcClient;

    private MemcachedOperationFactory(List<?> servers, String user, String password, String bucket,
            boolean useBucket) throws IOException {
        super();
        if (useBucket) {
            @SuppressWarnings("unchecked")
            final List<URI> nodes = (List<URI>) servers;
            this.mcClient = new CouchbaseClient(nodes, bucket, user, password);
        } else {
            @SuppressWarnings("unchecked")
            final List<URI> nodes = (List<URI>) servers;
            final CouchbaseConnectionFactory factory = new CouchbaseConnectionFactory(nodes,
                    "default", "");
            this.mcClient = new CouchbaseClient(factory);
        }
    }

    /**
     * Creates a new factory.
     * 
     * @param hosts
     *            the hostname and port of the Memcached servers
     * @param user
     *            the username for connecting to the server
     * @param passwd
     *            the password for connecting to the server
     * @param bucket
     *            the bucket where all operations are applied
     * @param useBucket
     *            whether to connect to the specified bucket or to the default
     * @return the factory
     */
    public static MemcachedOperationFactory getFactory(List<?> hosts, String user, String password,
            String bucket, boolean useBucket) {
        try {
            return new MemcachedOperationFactory(hosts, user, password, bucket, // NOPMD
                                                                                // by
                                                                                // georgiana
                                                                                // on
                                                                                // 10/12/11
                                                                                // 4:56
                                                                                // PM
                    useBucket);
        } catch (final IOException e) {
            ExceptionTracer.traceIgnored(e);
        }
        return null;
    }

    private IOperation<?> buildAddOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                final int exp = (Integer) parameters[1];
                final byte[] data = (byte[]) parameters[2];
                final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.add(key,
                        exp, data);
                return opResult.get();
            }
        });
    }

    private IOperation<?> buildAppendOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                final byte[] data = (byte[]) parameters[1];
                final long cas = MemcachedOperationFactory.this.mcClient.gets(key).getCas();
                final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.append(
                        cas, key, data);
                return opResult.get();
            }
        });
    }

    private IOperation<?> buildCasOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                final byte[] data = (byte[]) parameters[1];
                final long cas = MemcachedOperationFactory.this.mcClient.gets(key).getCas();
                final Future<CASResponse> opResult = MemcachedOperationFactory.this.mcClient
                        .asyncCAS(key, cas, data);
                return (opResult.get() == CASResponse.OK);
            }
        });
    }

    private IOperation<?> buildDeleteOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient
                        .delete(key);
                return opResult.get();
            }
        });
    }

    private IOperation<?> buildGetBulkOperation(final Object... parameters) {
        return new GenericOperation<Map<String, byte[]>>(new Callable<Map<String, byte[]>>() {

            @Override
            public Map<String, byte[]> call() throws ExecutionException, InterruptedException {
                final String[] keys = (String[]) parameters;
                final Future<Map<String, Object>> opResult = MemcachedOperationFactory.this.mcClient
                        .asyncGetBulk(keys);
                final Map<String, byte[]> result = new HashMap<String, byte[]>();
                for (final Map.Entry<String, Object> entry : opResult.get().entrySet()) {
                    result.put(entry.getKey(), (byte[]) entry.getValue());
                }
                return result;
            }
        });
    }

    private IOperation<?> buildGetOperation(final Object... parameters) {
        return new GenericOperation<byte[]>(new Callable<byte[]>() {

            @Override
            public byte[] call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                final Future<Object> opResult = MemcachedOperationFactory.this.mcClient
                        .asyncGet(key);
                return (byte[]) opResult.get();
            }
        });
    }

    private IOperation<?> buildPrependOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                final byte[] data = (byte[]) parameters[1];
                final long cas = MemcachedOperationFactory.this.mcClient.gets(key).getCas();
                final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.prepend(
                        cas, key, data);
                return opResult.get();
            }
        });
    }

    private IOperation<?> buildReplaceOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                final int exp = (Integer) parameters[1];
                final byte[] data = (byte[]) parameters[2];
                final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.replace(
                        key, exp, data);
                return opResult.get();
            }
        });
    }

    private IOperation<?> buildSetOperation(final Object... parameters) {
        return new GenericOperation<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws ExecutionException, InterruptedException {
                final String key = (String) parameters[0];
                int exp;
                byte[] data;
                if (parameters.length == 3) {
                    exp = (Integer) parameters[1];
                    data = (byte[]) parameters[2];
                } else {
                    exp = 0;
                    data = (byte[]) parameters[1];
                }
                final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.set(key,
                        exp, data);
                return opResult.get();
            }
        });
    }

    @Override
    public void destroy() {
        this.mcClient.shutdown(30, TimeUnit.SECONDS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.mosaic_cloud.platform.core.IOperationFactory#getOperation(eu.mosaic_cloud
     * .platform.core.IOperationType, java.lang.Object[])
     */
    @Override
    public IOperation<?> getOperation(final IOperationType type, // NOPMD by
                                                                 // georgiana
                                                                 // on
                                                                 // 10/12/11
                                                                 // 4:57 PM
            Object... parameters) {
        IOperation<?> operation = null; // NOPMD by georgiana on 10/12/11 4:56
                                        // PM
        if (!(type instanceof KeyValueOperations)) {
            return new GenericOperation<Object>(new Callable<Object>() { // NOPMD

                        // by
                        // georgiana
                        // on
                        // 10/12/11
                        // 4:56
                        // PM
                        @Override
                        public Object call() throws UnsupportedOperationException {
                            throw new UnsupportedOperationException("Unsupported operation: "
                                    + type.toString());
                        }
                    });
        }
        final KeyValueOperations mType = (KeyValueOperations) type;
        switch (mType) {
        case SET:
            operation = buildSetOperation(parameters);
            break;
        case ADD:
            operation = buildAddOperation(parameters);
            break;
        case REPLACE:
            operation = buildReplaceOperation(parameters);
            break;
        case APPEND:
            operation = buildAppendOperation(parameters);
            break;
        case PREPEND:
            operation = buildPrependOperation(parameters);
            break;
        case CAS:
            operation = buildCasOperation(parameters);
            break;
        case GET:
            operation = buildGetOperation(parameters);
            break;
        case GET_BULK:
            operation = buildGetBulkOperation(parameters);
            break;
        case DELETE:
            operation = buildDeleteOperation(parameters);
            break;
        default:
            operation = new GenericOperation<Object>(new Callable<Object>() { // NOPMD

                                                                              // by
                                                                              // georgiana
                                                                              // on
                                                                              // 10/12/11
                                                                              // 4:56
                                                                              // PM
                        @Override
                        public Object call() throws UnsupportedOperationException {
                            throw new UnsupportedOperationException("Unsupported operation: "
                                    + mType.toString());
                        }
                    });
        }
        return operation;
    }
}
