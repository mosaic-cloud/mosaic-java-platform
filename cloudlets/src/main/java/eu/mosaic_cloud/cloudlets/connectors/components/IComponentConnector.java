
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnector;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface IComponentConnector<TExtra>
		extends
			IConnector
{
	CallbackCompletion<ComponentResourceDescriptor> acquire (final ComponentResourceSpecification resource, final TExtra extra);
}
