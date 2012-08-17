
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;


public class ComponentCallSucceededCallbackArguments<TOutputs, TExtra>
		extends CallbackArguments
{
	public ComponentCallSucceededCallbackArguments (final ICloudletController<?> cloudlet, final TOutputs outputs, final TExtra extra)
	{
		super (cloudlet);
		this.outputs = outputs;
		this.extra = extra;
	}
	
	public TExtra getExtra ()
	{
		return (this.extra);
	}
	
	public TOutputs getOutputs ()
	{
		return (this.outputs);
	}
	
	private final TExtra extra;
	private final TOutputs outputs;
}
