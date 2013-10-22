
package eu.mosaic_cloud.platform.implementation.v2.connectors.kvstore;


import eu.mosaic_cloud.platform.implementation.v2.connectors.core.BaseConnectorProxy;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionTrigger;


public interface BaseKvStoreConnectorProxy<TValue extends Object>
			extends
				BaseConnectorProxy
{
	CallbackCompletion<Void> delete (String key, final CallbackCompletionTrigger<Void> trigger);
	
	CallbackCompletion<TValue> get (String key, final CallbackCompletionTrigger<TValue> trigger);
	
	CallbackCompletion<Void> set (String key, TValue value, CallbackCompletionTrigger<Void> trigger);
}
