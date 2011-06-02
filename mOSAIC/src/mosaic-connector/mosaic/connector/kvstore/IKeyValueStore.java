package mosaic.connector.kvstore;

import java.util.List;

import mosaic.connector.IResourceConnector;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;

/**
 * Interface for working with key-value stores.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IKeyValueStore extends IResourceConnector {
	/**
	 * Stores the given data and associates it with the specified key.
	 * 
	 * @param key
	 *            the key under which this data should be stored
	 * @param data
	 *            the data
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return a result handle for the operation
	 */
	IResult<Boolean> set(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);

	/**
	 * Gets data associated with a single key.
	 * 
	 * @param key
	 *            the key
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return a result handle for the operation
	 */
	IResult<Object> get(String key,
			List<IOperationCompletionHandler<Object>> handlers,
			CompletionInvocationHandler<Object> iHandler);

	/**
	 * Deletes the given key.
	 * 
	 * @param key
	 *            the key to delete
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @param iHandler
	 *            an invocation handler which shall be used to invoke the
	 *            completion handlers. This can be used for controlling how the
	 *            completion handlers are executed
	 * @return a result handle for the operation
	 */
	IResult<Boolean> delete(String key,
			List<IOperationCompletionHandler<Boolean>> handlers,
			CompletionInvocationHandler<Boolean> iHandler);
}
