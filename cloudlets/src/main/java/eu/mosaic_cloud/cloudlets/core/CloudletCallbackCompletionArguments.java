
package eu.mosaic_cloud.cloudlets.core;

public class CloudletCallbackCompletionArguments<Context> extends
        CallbackCompletionArguments<Context> {
    public CloudletCallbackCompletionArguments(final ICloudletController<Context> cloudlet) {
        super(cloudlet);
    }

    public CloudletCallbackCompletionArguments(final ICloudletController<Context> cloudlet,
            final Throwable error) {
        super(cloudlet, error);
    }

    @Override
    public ICloudletController<Context> getCloudlet() {
        return (ICloudletController<Context>) this.cloudlet;
    }
}
