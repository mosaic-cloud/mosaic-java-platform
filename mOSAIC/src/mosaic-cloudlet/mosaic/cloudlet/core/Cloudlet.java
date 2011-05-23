package mosaic.cloudlet.core;

import mosaic.cloudlet.runtime.CloudletExecutor;
import mosaic.core.configuration.IConfiguration;

public class Cloudlet implements ICloudlet {
	private boolean active;
	private CloudletExecutor<ICloudlet> executor;
	private IConfiguration configuration;

	/**
	 * Creates a new cloudlet instance.
	 * 
	 * @param config
	 *            configuration data required for configuring and initializing
	 *            the cloudlet instance
	 */
	public Cloudlet(IConfiguration config) {
		this.configuration = config;
		this.active = false;
		this.executor = new CloudletExecutor<ICloudlet>(this, config);
	}

	@Override
	public boolean init(IConfiguration configData) {
		boolean initialized = false;
		// TODO Auto-generated method stub
		return initialized;
	}

	@Override
	public boolean destroy() {
		boolean destroyed = false;
		// TODO Auto-generated method stub
		return destroyed;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

}
