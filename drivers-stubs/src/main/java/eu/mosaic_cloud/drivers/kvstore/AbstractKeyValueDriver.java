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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import eu.mosaic_cloud.drivers.AbstractResourceDriver;
import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.GenericResult;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;

/**
 * Base class for key-value store drivers. Implements only the basic set, get,
 * list and delete operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractKeyValueDriver extends AbstractResourceDriver {

    private class BucketData {

        private final String bucketName;
        private final AtomicInteger noClients;
        private final IOperationFactory opFactory;

        public BucketData(String bucket) {
            this.bucketName = bucket;
            this.noClients = new AtomicInteger(0);
            this.opFactory = AbstractKeyValueDriver.this.createOperationFactory(bucket);
        }

        private void destroy() {
            this.opFactory.destroy();
        }
    }

    /**
     * Map between bucket name and bucket data.
     */
    private final Map<String, BucketData> bucketFactories;
    /**
     * Map between clientId and bucket data.
     */
    private final Map<String, BucketData> clientBucketMap;

    protected AbstractKeyValueDriver(ThreadingContext threading, int noThreads) {
        super(threading, noThreads);
        this.bucketFactories = new HashMap<String, BucketData>();
        this.clientBucketMap = new HashMap<String, BucketData>();
    }

    protected abstract IOperationFactory createOperationFactory(Object... params);

    @Override
    public synchronized void destroy() {
        super.destroy();
        for (final Map.Entry<String, BucketData> bucket : this.bucketFactories.entrySet()) {
            bucket.getValue().destroy();
        }
        this.clientBucketMap.clear();
        this.bucketFactories.clear();
    }

    /**
     * Returns the operation factory used by the driver.
     * 
     * @return the operation factory
     */
    private IOperationFactory getOperationFactory(String clientId) {
        IOperationFactory factory = null; // NOPMD by georgiana on 10/12/11
                                          // 12:55 PM
        final BucketData bucket = this.clientBucketMap.get(clientId);
        if (bucket != null) {
            factory = bucket.opFactory;
        }
        return factory;
    }

    /**
     * Returns the operation factory used by the driver.
     * 
     * @param <T>
     *            the type of the factory
     * @param clientId
     *            the unique identifier of the driver's client
     * @param factClass
     *            the class object of the factory
     * @return the operation factory
     */
    protected <T extends IOperationFactory> T getOperationFactory(String clientId,
            Class<T> factClass) {
        T factory = null; // NOPMD by georgiana on 10/12/11 12:55 PM
        final BucketData bucket = this.clientBucketMap.get(clientId);
        if (bucket != null) {
            factory = factClass.cast(bucket.opFactory);
        }
        return factory;
    }

    public IResult<Boolean> invokeDeleteOperation(String clientId, String key,
            IOperationCompletionHandler<Boolean> complHandler) {
        final IOperationFactory opFactory = getOperationFactory(clientId);
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) opFactory
                .getOperation(KeyValueOperations.DELETE, key);
        return startOperation(operation, complHandler);
    }

    public IResult<byte[]> invokeGetOperation(String clientId, String key,
            IOperationCompletionHandler<byte[]> complHandler) {
        final IOperationFactory opFactory = getOperationFactory(clientId);
        @SuppressWarnings("unchecked")
        final GenericOperation<byte[]> operation = (GenericOperation<byte[]>) opFactory
                .getOperation(KeyValueOperations.GET, key);
        return startOperation(operation, complHandler);
    }

    public IResult<List<String>> invokeListOperation(String clientId,
            IOperationCompletionHandler<List<String>> complHandler) {
        final IOperationFactory opFactory = getOperationFactory(clientId);
        @SuppressWarnings("unchecked")
        final GenericOperation<List<String>> operation = (GenericOperation<List<String>>) opFactory
                .getOperation(KeyValueOperations.LIST);
        return startOperation(operation, complHandler);
    }

    public IResult<Boolean> invokeSetOperation(String clientId, String key, byte[] data,
            IOperationCompletionHandler<Boolean> complHandler) {
        final IOperationFactory opFactory = getOperationFactory(clientId);
        @SuppressWarnings("unchecked")
        final GenericOperation<Boolean> operation = (GenericOperation<Boolean>) opFactory
                .getOperation(KeyValueOperations.SET, key, data);
        return startOperation(operation, complHandler);
    }

    /**
     * Registers a new client for the driver.
     * 
     * @param clientId
     *            the unique ID of the client
     * @param bucket
     *            the bucket used by the client
     */
    public synchronized void registerClient(String clientId, String bucket) {
        Preconditions.checkArgument(!this.clientBucketMap.containsKey(clientId));
        BucketData bucketData = this.bucketFactories.get(bucket);
        if (bucketData == null) {
            bucketData = new BucketData(bucket);
            this.bucketFactories.put(bucket, bucketData);
            bucketData.noClients.incrementAndGet();
            this.logger.trace("Create new client for bucket " + bucket);
        }
        this.clientBucketMap.put(clientId, bucketData);
        this.logger.trace("Registered client " + clientId + " for bucket " + bucket);
    }

    @SuppressWarnings({
            "rawtypes", "unchecked" })
    private <T extends Object> IResult<T> startOperation(GenericOperation<T> operation,
            IOperationCompletionHandler complHandler) {
        final IResult<T> iResult = new GenericResult<T>(operation);
        operation.setHandler(complHandler);
        this.addPendingOperation(iResult);
        this.submitOperation(operation.getOperation());
        return iResult;
    }

    /**
     * Unregisters a client from the driver.
     * 
     * @param clientId
     *            the unique ID of the client
     */
    public synchronized void unregisterClient(String clientId) {
        Preconditions.checkArgument(this.clientBucketMap.containsKey(clientId));
        final BucketData bucketData = this.clientBucketMap.get(clientId);
        final int noClients = bucketData.noClients.decrementAndGet();
        if (noClients == 0) {
            bucketData.destroy();
            this.bucketFactories.remove(bucketData.bucketName);
        }
        this.clientBucketMap.remove(clientId);
        this.logger.trace("Unregistered client " + clientId);
    }
}
