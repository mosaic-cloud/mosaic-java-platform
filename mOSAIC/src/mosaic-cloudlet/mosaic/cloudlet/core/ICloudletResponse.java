package mosaic.cloudlet.core;

public interface ICloudletResponse {

	/**
	 * Status code (404) indicating that the requested cloudlet is not
	 * available.
	 */
	int CLOUDLET_NOT_FOUND = 404;

	void sendError(int cloudletNotFound);

}
