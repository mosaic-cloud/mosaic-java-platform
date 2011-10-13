package mosaic.core.configuration;

/**
 * Interface for configuration parameters.
 * 
 * @author Ciprian Craciun, Georgiana Macariu
 * 
 * @param <T>
 *            the type of the value of the parameter
 */
public interface IConfigurationParameter<T extends Object> {

	/**
	 * Returns the configuration identifier of the parameter.
	 * 
	 * @return the configuration identifier of the parameter
	 */
	ConfigurationIdentifier getIdentifier();

	/**
	 * Returns the value of the parameter.
	 * 
	 * @param defaultValue
	 *            a default value of the parameter used in case the parameter is
	 *            not found in the configuration
	 * @return the value of the parameter
	 */
	T getValue(final T defaultValue);

	/**
	 * Returns the type of the value of the parameter.
	 * 
	 * @return the type of the value of the parameter
	 */
	Class<T> getValueClass();
}
