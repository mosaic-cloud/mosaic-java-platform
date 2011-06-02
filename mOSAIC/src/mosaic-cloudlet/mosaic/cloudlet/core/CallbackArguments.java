package mosaic.cloudlet.core;

public class CallbackArguments<S> {
	private ICloudletController<S> cloudlet;

	public CallbackArguments(ICloudletController<S> cloudlet) {
		super();
		this.cloudlet = cloudlet;
	}

	public ICloudletController<S> getCloudlet() {
		return cloudlet;
	}
}
