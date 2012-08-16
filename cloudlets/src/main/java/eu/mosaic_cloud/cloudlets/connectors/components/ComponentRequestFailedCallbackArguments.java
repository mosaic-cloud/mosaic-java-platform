
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;


public final class ComponentRequestFailedCallbackArguments<TExtra>
		extends CallbackArguments
{
	public ComponentRequestFailedCallbackArguments (final ICloudletController<?> cloudlet, final Throwable exception, final TExtra extra)
	{
		super (cloudlet);
		this.exception = exception;
		this.extra = extra;
	}
	
	public Throwable getException ()
	{
		return (this.exception);
	}
	
	public TExtra getExtra ()
	{
		return (this.extra);
	}
	
	private final Throwable exception;
	private final TExtra extra;
}
