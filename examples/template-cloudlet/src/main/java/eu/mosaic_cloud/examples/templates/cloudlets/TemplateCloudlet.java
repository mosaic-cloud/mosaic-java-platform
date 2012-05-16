
package eu.mosaic_cloud.examples.templates.cloudlets;


import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.DefaultCloudletCallback;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class TemplateCloudlet
		extends DefaultCloudletCallback<TemplateCloudlet.Context>
{
	@Override
	public CallbackCompletion<Void> destroy (final Context context, final CloudletCallbackArguments<Context> arguments)
	{
		return (ICallback.SUCCESS);
	}
	
	@Override
	public CallbackCompletion<Void> initialize (final Context context, final CloudletCallbackArguments<Context> arguments)
	{
		context.cloudlet = arguments.getCloudlet ();
		context.configuration = context.configuration;
		return (ICallback.SUCCESS);
	}
	
	public static class Context
	{
		ICloudletController<Context> cloudlet;
		IConfiguration configuration;
	}
}
