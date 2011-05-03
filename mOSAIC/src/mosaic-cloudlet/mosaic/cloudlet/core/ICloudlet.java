package mosaic.cloudlet.core;

import mosaic.core.configuration.IConfiguration;

public interface ICloudlet {

	void init(IConfiguration configData);

	void destroy();

	void service(ICloudletRequest aRequest, ICloudletResponse aResponse);

}
