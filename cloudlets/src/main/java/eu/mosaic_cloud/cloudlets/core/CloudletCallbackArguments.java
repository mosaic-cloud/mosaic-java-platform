
package eu.mosaic_cloud.cloudlets.core;


public class CloudletCallbackArguments<C>
		extends CallbackArguments<C>
{
	public CloudletCallbackArguments (final ICloudletController<C> cloudlet)
	{
		super (cloudlet);
	}
	
	@Override
	public ICloudletController<C> getCloudlet ()
	{
		return (ICloudletController<C>) this.cloudlet;
	}
}
