
package eu.mosaic_cloud.cloudlets.implementation.container;


import eu.mosaic_cloud.cloudlets.connectors.core.IConnector;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public interface IComponentConnector
		extends
			IConnector
{
	CallbackCompletion<ComponentResourceDescriptor> acquire (final ComponentResourceSpecification resource);
	
	<TInputs, TOutputs> CallbackCompletion<TOutputs> call (final ComponentIdentifier component, final String operation, final TInputs inputs, final Class<TOutputs> outputs);
	
	<TInputs> CallbackCompletion<Void> cast (final ComponentIdentifier component, final String operation, final TInputs inputs);
}
