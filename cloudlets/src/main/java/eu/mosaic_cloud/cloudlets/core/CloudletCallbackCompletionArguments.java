
package eu.mosaic_cloud.cloudlets.core;


public class CloudletCallbackCompletionArguments<C>
		extends CallbackCompletionArguments<C>
{
	public CloudletCallbackCompletionArguments (final ICloudletController<C> cloudlet)
	{
		super (cloudlet);
	}
	
	public CloudletCallbackCompletionArguments (final ICloudletController<C> cloudlet, final Throwable error)
	{
		super (cloudlet, error);
	}
	
	@Override
	public ICloudletController<C> getCloudlet ()
	{
		return (ICloudletController<C>) this.cloudlet;
	}
}
