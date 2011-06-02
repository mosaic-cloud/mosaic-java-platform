package mosaic.cloudlet.core;

import mosaic.cloudlet.resources.IResourceAccessor;
import mosaic.cloudlet.resources.IResourceAccessorCallback;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.ops.CompletionInvocationHandler;
import mosaic.core.ops.IOperationCompletionHandler;

public interface ICloudletController<S> extends ICloudlet {
	IConfiguration getConfiguration();

	<T> CompletionInvocationHandler<T> getResponseInvocationHandler(
			IOperationCompletionHandler<T> handler);

	<T> T buildCallbackInvoker(T callback, Class<T> callbackType);

	void initializeResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler, S cloudletState);

	void destroyResource(IResourceAccessor<S> accessor,
			IResourceAccessorCallback<S> callbackHandler);

}
