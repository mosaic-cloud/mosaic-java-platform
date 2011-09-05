package mosaic.cloudlet.runtime;

import mosaic.cloudlet.ConfigProperties;
import mosaic.cloudlet.runtime.ContainerComponentCallbacks.ResourceType;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.interop.idl.ChannelData;

/**
 * Finder for resource drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResourceFinder {
	private static ResourceFinder finder;

	private ResourceFinder() {

	}

	/**
	 * Returns a finder object.
	 * 
	 * @return the finder object
	 */
	public static ResourceFinder getResourceFinder() {
		if (ResourceFinder.finder == null) {
			ResourceFinder.finder = new ResourceFinder();
		}
		return ResourceFinder.finder;
	}

	/**
	 * Starts an asynchronous driver lookup. When the result from the mOSAIC
	 * platform arrives the provided callback will be invoked.
	 * 
	 * @param type
	 *            the type of resource to find
	 * @param callback
	 *            the callback to be called when the resource is found
	 */
	public boolean findResource(ResourceType type, IConfiguration configuration) {
		ChannelData channel = null;
		boolean found = false;

		channel = ContainerComponentCallbacks.callbacks.findDriver(type);
		MosaicLogger.getLogger().trace(
				"ResourceFinder - found resource " + channel);
		if (channel != null) {
			String prefix = (configuration.getRootIdentifier().getIdentifier() + ".")
					.substring(1).replace('/', '.');

			ConfigurationIdentifier id1 = ConfigurationIdentifier
					.resolveRelative(prefix
							+ ConfigProperties
									.getString("ContainerComponentCallbacks.6"));
			ConfigurationIdentifier id2 = ConfigurationIdentifier
					.resolveRelative(prefix
							+ ConfigProperties
									.getString("ContainerComponentCallbacks.5"));

			configuration.addParameter(id1, channel.getChannelIdentifier());
			configuration.addParameter(id2, channel.getChannelEndpoint());
//			MosaicLogger.getLogger().debug(
//					"ResourceFinder - config: " + configuration);
			found = true;
		}

		return found;
	}

}
