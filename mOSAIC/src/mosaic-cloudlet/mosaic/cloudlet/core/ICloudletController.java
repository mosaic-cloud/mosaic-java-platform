package mosaic.cloudlet.core;

import mosaic.cloudlet.resources.IResourceAccessor;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.IOperationCompletionHandler;

/**
 * Interface for cloudlet control operations. Each cloudlet has access to an
 * object implementing this interface and uses it to ask for resources or
 * destroying them when they are not required anymore.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet
 */
public interface ICloudletController<S> extends ICloudlet {
	IConfiguration getConfiguration();

	<T> CompletionInvocationHandler<T> getResponseInvocationHandler(
			IOperationCompletionHandler<T> handler);

	<T> T buildCallbackInvoker(T callback, Class<T> callbackType);

	/**
	 * Initializes the resource accessor for a given resource.
	 * 
	 * @param accessor
	 *            the resource accessor
	 * @param callbackHandler
	 *            the cloudlet callback handler which must handle callbacks to
	 *            operations invoked on the accessor
	 * @param cloudletState
	 *            the cloudlet state
	 */
	void initializeResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler, S cloudletState);

	/**
	 * Destroys the resource accessor for a given resource.
	 * 
	 * @param accessor
	 *            the resource accessor
	 * @param callbackHandler
	 *            the cloudlet callback handler which must handle callbacks to
	 *            operations invoked on the accessor
	 */
	void destroyResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler);

}
