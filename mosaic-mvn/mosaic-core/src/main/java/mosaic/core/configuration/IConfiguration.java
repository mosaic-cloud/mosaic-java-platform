package mosaic.core.configuration;

/**
 * Generic interface for handling configuration files.
 * 
 * @author Ciprian Craciun, Georgiana Macariu
 * 
 */
public interface IConfiguration {
	/**
	 * Returns a configuration parameter.
	 * 
	 * @param <T>
	 *            the type of the parameter
	 * @param identifier
	 *            the identifier of the parameter
	 * @param valueClass
	 *            the class object representing the type of the configuration
	 *            parameter
	 * @return the configuration parameter
	 */
	<T extends Object> IConfigurationParameter<T> getParameter(
			final ConfigurationIdentifier identifier, final Class<T> valueClass);

	/**
	 * Returns a configuration containing all parameters which names start with
	 * a given identifier (root identifier).
	 * 
	 * @param relative
	 *            the root identifier
	 * @return the configuration object
	 */
	IConfiguration spliceConfiguration(final ConfigurationIdentifier relative);

	/**
	 * Adds a parameter to the configuration.
	 * 
	 * @param identifier
	 *            the identifier of the parameter
	 * @param value
	 *            the value of the parameter
	 */
	<T extends Object> void addParameter(
			final ConfigurationIdentifier identifier, final T value);

	/**
	 * Adds a parameter to the configuration.
	 * 
	 * @param property
	 *            the identifier of the parameter
	 * @param value
	 *            the value of the parameter
	 */
	<T extends Object> void addParameter(final String property, final T value);

	/**
	 * Returns the root identifier.
	 * 
	 * @return the root identifier
	 */
	ConfigurationIdentifier getRootIdentifier();
}
