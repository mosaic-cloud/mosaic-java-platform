
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface IComponentConnectorCallbacks<TContext, TExtra>
		extends
			IConnectorCallback<TContext>
{
	CallbackCompletion<Void> acquireFailed (TContext context, ComponentRequestFailedCallbackArguments<TExtra> arguments);
	
	CallbackCompletion<Void> acquireSucceeded (TContext context, ComponentAcquireSucceededCallbackArguments<TExtra> arguments);
}
