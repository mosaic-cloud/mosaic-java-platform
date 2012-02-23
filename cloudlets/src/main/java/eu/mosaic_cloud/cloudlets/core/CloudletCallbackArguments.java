
package eu.mosaic_cloud.cloudlets.core;

public class CloudletCallbackArguments<Context> extends CallbackArguments<Context> {
    public CloudletCallbackArguments(final ICloudletController<Context> cloudlet) {
        super(cloudlet);
    }

    @Override
    public ICloudletController<Context> getCloudlet() {
        return (ICloudletController<Context>) this.cloudlet;
    }
}
