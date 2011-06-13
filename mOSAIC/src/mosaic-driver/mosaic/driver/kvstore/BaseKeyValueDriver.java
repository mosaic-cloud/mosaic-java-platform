package mosaic.driver.kvstore;

import java.util.List;

import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.GenericResult;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IResult;
import mosaic.driver.AbstractResourceDriver;
import mosaic.driver.IResourceDriver;

/**
 * Base class for key-value store drivers. Implements only the basic set, get,
 * list and delete operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class BaseKeyValueDriver extends AbstractResourceDriver {
	private IOperationFactory opFactory;

	protected BaseKeyValueDriver(int noThreads, IOperationFactory opFactory) {
		super(noThreads);
		this.opFactory = opFactory;
	}

	public synchronized IResult<Boolean> invokeSetOperation(String key,
			Object data, IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
				.getOperation(KeyValueOperations.SET, key, data);

		IResult<Boolean> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Object> invokeGetOperation(String key,
			IOperationCompletionHandler<Object> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Object> op = (GenericOperation<Object>) this.opFactory
				.getOperation(KeyValueOperations.GET, key);

		IResult<Object> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<List<String>> invokeListOperation(
			IOperationCompletionHandler<List<String>> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<List<String>> op = (GenericOperation<List<String>>) this.opFactory
				.getOperation(KeyValueOperations.LIST);

		IResult<List<String>> iResult = startOperation(op, complHandler);
		return iResult;
	}

	public synchronized IResult<Boolean> invokeDeleteOperation(String key,
			IOperationCompletionHandler<Boolean> complHandler) {
		@SuppressWarnings("unchecked")
		GenericOperation<Boolean> op = (GenericOperation<Boolean>) this.opFactory
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
	 * @param factClass
	 *            the class object of the factory
	 * @return the operation factory
	 */
	protected <T extends IOperationFactory> T getOperationFactory(
			Class<T> factClass) {
		return factClass.cast(this.opFactory);
	}

}