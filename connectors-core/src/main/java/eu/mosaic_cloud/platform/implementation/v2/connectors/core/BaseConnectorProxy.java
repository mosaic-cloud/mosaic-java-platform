
package eu.mosaic_cloud.platform.implementation.v2.connectors.core;


import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionTrigger;


public interface BaseConnectorProxy
{
	public abstract CallbackCompletion<Void> destroy (final CallbackCompletionTrigger<Void> trigger);
	
	public abstract CallbackCompletion<Void> initialize (final CallbackCompletionTrigger<Void> trigger);
}
