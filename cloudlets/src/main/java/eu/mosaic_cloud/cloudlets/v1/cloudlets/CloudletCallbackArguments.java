
package eu.mosaic_cloud.cloudlets.v1.cloudlets;


import eu.mosaic_cloud.cloudlets.v1.core.CallbackArguments;


public abstract class CloudletCallbackArguments
			extends CallbackArguments
{
	protected CloudletCallbackArguments (final CloudletController<?> cloudlet) {
		super (cloudlet);
	}
}
