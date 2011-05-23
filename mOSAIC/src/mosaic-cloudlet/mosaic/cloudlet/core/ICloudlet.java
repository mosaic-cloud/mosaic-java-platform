package mosaic.cloudlet.core;

import mosaic.core.configuration.IConfiguration;

public interface ICloudlet {

	boolean init(IConfiguration configData);

	boolean destroy();

	boolean isActive();

}
