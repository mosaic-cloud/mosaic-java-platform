
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnector;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface IComponentConnector<TExtra>
		extends
			IConnector
{
	CallbackCompletion<ComponentResourceDescriptor> acquire (final ComponentResourceSpecification resource, final TExtra extra);
	
	<TInputs, TOutputs> CallbackCompletion<TOutputs> call (final ComponentIdentifier component, final String operation, final TInputs inputs, final Class<TOutputs> outputs, final TExtra extra);
	
	<TInputs> CallbackCompletion<Void> cast (final ComponentIdentifier component, final String operation, final TInputs inputs);
}
