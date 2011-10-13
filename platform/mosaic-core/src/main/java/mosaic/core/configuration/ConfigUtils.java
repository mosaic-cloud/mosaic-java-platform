package mosaic.core.configuration;

/**
 * Utility functions used for handling configurations.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class ConfigUtils {

	private ConfigUtils() {
	}

	/**
	 * Resolves a configuration parameter given its relative specification.
	 * 
	 * @param <T>
	 *            the type of the parameter
	 * @param configuration
	 *            the configuration to which the parameter belongs
	 * @param identifier
	 *            the relative identifier of the parameter
	 * @param valueClass
	 *            the class object representing the type of the parameter
	 * @param defaultValue
	 *            a default value to give to the parameter if it is not found in
	 *            the given configuration
	 * @return the value of the parameter
	 */
	public static <T extends Object> T resolveParameter(
			IConfiguration configuration, String identifier, // NOPMD by
																// georgiana on
																// 9/27/11 1:27
																// PM
			Class<T> valueClass, T defaultValue) { // NOPMD by georgiana on
													// 9/27/11 1:27 PM
		T retValue;
		if (configuration == null) {
			retValue = defaultValue;

		} else {
			retValue = configuration.getParameter(
					ConfigurationIdentifier.resolveRelative(identifier),
					valueClass).getValue(defaultValue);
		}
		return retValue;
	}
}
