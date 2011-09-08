package mosaic.connector.components;

import mosaic.interop.idl.ChannelData;

/**
 * Interface for callbacks called by the {@link ResourceFinder} when a resource
 * is found or not.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IFinderCallback {

	/**
	 * Called when the resource driver is found
	 * 
	 * @param channel
	 *            interoperability channel parameters (used for communicating
	 *            with the driver)
	 * @throws Throwable
	 */
	public abstract void resourceFound(ChannelData channel) throws Throwable;

	/**
	 * Called when a resource driver cannot be found.
	 */
	public abstract void resourceNotFound();

}