
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorFactory;


public interface IComponentConnectorFactory
		extends
			IConnectorFactory<IComponentConnector<?>>
{
	<TContext, TExtra> IComponentConnector<TExtra> create (IComponentConnectorCallbacks<TContext, TExtra> callbacks, TContext callbacksContext);
}
