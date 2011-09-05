package mosaic.core.ops;

/**
 * Factory class which builds the asynchronous calls for the operations
 * supported by a specific resource. This interface should be implemented for
 * each resource kind supported by the platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IOperationFactory {
	/**
	 * Builds the asynchronous operation.
	 * 
	 * @param type
	 *            the type of the operation
	 * @param parameters
	 *            the parameters of the operation
	 * @return the operation
	 */
	IOperation<?> getOperation(IOperationType type, Object... parameters);

	/**
	 * Destroys a facory..
	 */
	void destroy();

}
