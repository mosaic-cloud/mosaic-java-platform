
package eu.mosaic_cloud.cloudlets.implementation.container;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnector;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface IComponentConnector
		extends
			IConnector
{
	CallbackCompletion<ComponentResourceDescriptor> acquire (final ComponentResourceSpecification resource);
}
