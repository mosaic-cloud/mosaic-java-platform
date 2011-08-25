package mosaic.driver.kvstore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.GenericResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IResult;
import mosaic.driver.AbstractResourceDriver;

import com.google.common.base.Preconditions;

/**
 * Base class for key-value store drivers. Implements only the basic set, get,
 * list and delete operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class BaseKeyValueDriver extends AbstractResourceDriver {
	/**
	 * Map between bucket name and bucket data.
	 */
	private Map<String, BucketData> bucketFactories;
	/**
	 * Map between clientId and bucket data.
	 */
	private Map<String, BucketData> clientBucketMap;

	protected BaseKeyValueDriver(int noThreads) {
		super(noThreads);
		this.bucketFactories = new HashMap<String, BucketData>();
		this.clientBucketMap = new HashMap<String, BucketData>();
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		for (Map.Entry<String, BucketData> bucket : this.bucketFactories
				.entrySet()) {
			bucket.getValue().destroy();
		}
		this.clientBucketMap.clear();
		this.bucketFactories.clear();
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
		Preconditions
				.checkArgument(!this.clientBucketMap.containsKey(clientId));
		BucketData bucketData = this.bucketFactories.get(bucket);
		if (bucketData == null) {
			bucketData = new BucketData(bucket);
			this.bucketFactories.put(bucket, bucketData);
			bucketData.noClients.incrementAndGet();
			MosaicLogger.getLogger().trace(
					"Create new client for bucket " + bucket);
		}
		this.clientBucketMap.put(clientId, bucketData);
		MosaicLogger.getLogger().trace(
				"Registered client " + clientId + " for bucket " + bucket);
	}

	/**
	 * Unregisters a client from the driver.
	 * 
	 * @param clientId
	 *            the unique ID of the client
	 */
	public synchronized void unregisterClient(String clientId) {
		Preconditions.checkArgument(this.clientBucketMap.containsKey(clientId));
		BucketData bucketData = this.clientBucketMap.get(clientId);
		int noClients = bucketData.noClients.decrementAndGet();
		if (noClients == 0) {
			bucketData.destroy();
			this.bucketFactories.remove(bucketData.bucketName);
		}
		this.clientBucketMap.remove(clientId);
		MosaicLogger.getLogger().trace("Unregistered client " + clientId);
	}

	public synchronized IResult<Boolean> invokeSetOperation(String clientId,
			String key, Object data,
			IOperationCompletionHandler<Boolean> complHandler) {
		IOperationFactory opFactory = getOperationFactory(clientId);
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) opFactory
				.getOperation(KeyValueOperations.SET, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Object> invokeGetOperation(String clientId,
			String key, IOperationCompletionHandler<Object> complHandler) {
		IOperationFactory opFactory = getOperationFactory(clientId);
		@SuppressWarnings("unchecked")
		GenericOperation<Object> op = (GenericOperation<Object>) opFactory
				.getOperation(KeyValueOperations.GET, key);

		IResult<Object> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<List<String>> invokeListOperation(
			String clientId,
			IOperationCompletionHandler<List<String>> complHandler) {
		IOperationFactory opFactory = getOperationFactory(clientId);
		@SuppressWarnings("unchecked")
		GenericOperation<List<String>> op = (GenericOperation<List<String>>) opFactory
				.getOperation(KeyValueOperations.LIST);

		IResult<List<String>> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeDeleteOperation(String clientId,
			String key, IOperationCompletionHandler<Boolean> complHandler) {
		IOperationFactory opFactory = getOperationFactory(clientId);
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) opFactory
				.getOperation(KeyValueOperations.DELETE, key);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T extends Object> IResult<T> startOperation(
			GenericOperation<T> op, IOperationCompletionHandler complHandler) {
		IResult<T> iResult = new GenericResult<T>(op);
		op.setHandler(complHandler);
		this.addPendingOperation(iResult);

		this.submitOperation(op.getOperation());
		return iResult;
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
	protected synchronized <T extends IOperationFactory> T getOperationFactory(
			String clientId, Class<T> factClass) {
		T factory = null;
		BucketData bucket = this.clientBucketMap.get(clientId);
		if (bucket != null) {
			factory = factClass.cast(bucket.opFactory);
		}
		return factory;
	}

	/**
	 * Returns the operation factory used by the driver.
	 * 
	 * @return the operation factory
	 */
	private synchronized IOperationFactory getOperationFactory(String clientId) {
		IOperationFactory factory = null;
		BucketData bucket = this.clientBucketMap.get(clientId);
		if (bucket != null) {
			factory = bucket.opFactory;
		}
		return factory;
	}

	protected abstract IOperationFactory createOperationFactory(
			Object... params);

	private class BucketData {
		private String bucketName;
		private AtomicInteger noClients;
		private IOperationFactory opFactory;

		public BucketData(String bucket) {
			bucketName = bucket;
			noClients = new AtomicInteger(0);
			opFactory = BaseKeyValueDriver.this.createOperationFactory(bucket);
		}

		private void destroy() {
			this.opFactory.destroy();
		}
	}
}