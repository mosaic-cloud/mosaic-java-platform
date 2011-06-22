package mosaic.cloudlet.core;

import mosaic.core.configuration.IConfiguration;

/**
 * Interface defining the contract of the internal representation of a cloudlet.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface ICloudlet {

	/**
	 * Initializes the cloudlet.
	 * 
	 * @param configData
	 *            configuration data of the cloudlet
	 * @return <code>true</code> if cloudlet was successfully initialized
	 */
	boolean initialize(IConfiguration configData);

	/**
	 * Destroys the cloudlet.
	 * 
	 * @return <code>true</code> if cloudlet was successfully destroyed
	 */
	boolean destroy();

	/**
	 * Indicates if the cloudlet is alive and can receive messages or not.
	 * 
	 * @return <code>true</code> if cloudlet is alive
	 */
	boolean isActive();

}
