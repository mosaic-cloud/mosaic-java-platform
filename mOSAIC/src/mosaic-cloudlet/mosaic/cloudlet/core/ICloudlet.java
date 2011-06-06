package mosaic.cloudlet.core;

import mosaic.core.configuration.IConfiguration;

public interface ICloudlet {

	boolean initialize(IConfiguration configData);

	boolean destroy();

	boolean isActive();

}
