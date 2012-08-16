
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;


public class ComponentAcquireSucceededCallbackArguments<TExtra>
		extends CallbackArguments
{
	public ComponentAcquireSucceededCallbackArguments (final ICloudletController<?> cloudlet, final ComponentResourceDescriptor descriptor, final TExtra extra)
	{
		super (cloudlet);
		this.descriptor = descriptor;
		this.extra = extra;
	}
	
	public ComponentResourceDescriptor getDescriptor ()
	{
		return (this.descriptor);
	}
	
	public TExtra getExtra ()
	{
		return (this.extra);
	}
	
	private final ComponentResourceDescriptor descriptor;
	private final TExtra extra;
}
