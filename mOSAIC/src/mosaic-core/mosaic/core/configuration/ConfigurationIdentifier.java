package mosaic.core.configuration;

import java.util.IdentityHashMap;
import java.util.regex.Pattern;

/**
 * Class for handling identifiers of configuration parameters. The parameters in
 * a configuration file may form a hierarchy and thus an identifier string may
 * be absolute or relative.
 * 
 * @author Ciprian Craciun, Georgiana Macariu
 * 
 */
public final class ConfigurationIdentifier {

	public static final ConfigurationIdentifier root = new ConfigurationIdentifier(
			true, null);
	private static final Pattern identifierPattern = Pattern.compile(
			"^/?([a-z]([a-z0-9_.]*[a-z0-9])?/)*([a-z]([a-z0-9_.]*[a-z0-9])?)$",
			Pattern.DOTALL);

	private static final IdentityHashMap<String, ConfigurationIdentifier> identifiers = new IdentityHashMap<String, ConfigurationIdentifier>();
	/**
	 * Indicates if this identifier is absolute
	 */
	private final boolean absolute;
	/**
	 * The string identifier.
	 */
	private final String identifier;

	private ConfigurationIdentifier(final boolean absolute, String identifier) {
		super();
		this.absolute = absolute;
		this.identifier = identifier;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object identifier) {
		if (identifier == null)
			return false;
		if (!(identifier instanceof ConfigurationIdentifier))
			return false;
		ConfigurationIdentifier otherId = (ConfigurationIdentifier) identifier;
		if (absolute != otherId.absolute)
			return false;
		if (this.identifier != null
				&& !this.identifier.equals(otherId.identifier))
			return false;
		if (this.identifier == null && otherId.identifier != null)
			return false;
		return true;
	}

	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter relative to this configuration identifier.
	 * 
	 * @param identifier
	 *            the identifier to solve
	 * @return the built identifier
	 */
	public ConfigurationIdentifier resolve(ConfigurationIdentifier identifier) {
		if (identifier.absolute)
			throw (new IllegalArgumentException());
		return (ConfigurationIdentifier.resolveRelative(this,
				identifier.identifier));
	}

	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter relative to this configuration identifier.
	 * 
	 * @param specification
	 *            the string specification of the identifier to be built
	 * @return the built identifier
	 */
	public ConfigurationIdentifier resolve(String specification) {
		return (ConfigurationIdentifier.resolve(this, specification));
	}

	@Override
	public final String toString() {
		return (this.identifier);

	}

	/**
	 * Returns if this identifier is absolute or not.
	 * 
	 * @return <code>true</code> if it is absolute
	 */
	public boolean isAbsolute() {
		return absolute;
	}

	/**
	 * Returns the string identifier.
	 * 
	 * @return the string identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter given by an absolute identifier.
	 * 
	 * @param reference
	 *            the parent {@link ConfigurationIdentifier} object
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	public static ConfigurationIdentifier resolveAbsolute(String specification) {
		return ConfigurationIdentifier.resolve(ConfigurationIdentifier.root,
				specification);
	}

	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter.
	 * 
	 * @param reference
	 *            the parent {@link ConfigurationIdentifier} object
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	public static ConfigurationIdentifier resolveRelative(
			ConfigurationIdentifier reference, String specification) {
		return ConfigurationIdentifier.resolve(reference, specification);
	}

	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter.
	 * 
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	public static ConfigurationIdentifier resolveRelative(String specification) {
		return ConfigurationIdentifier.resolve(null, specification);
	}

	/**
	 * Builds the {@link ConfigurationIdentifier} object of a configuration
	 * parameter.
	 * 
	 * @param reference
	 *            the parent {@link ConfigurationIdentifier} object
	 * @param specification
	 *            the string specification of the parameter identifier
	 * @return the built {@link ConfigurationIdentifier} object
	 */
	private static ConfigurationIdentifier resolve(
			ConfigurationIdentifier reference, String specification) {
		boolean isAbsolute = false;
		String identifier_;
		final String identifier;
		// int slashIndex;
		// String parentIdentifier;
		ConfigurationIdentifier parameterIdentifier;
		// ConfigurationIdentifier parentConfigurationIdentifier;

		if (reference == null) {
			if (specification.startsWith("/"))
				isAbsolute = true;

			identifier_ = specification;
		} else if (reference.absolute) {
			isAbsolute = true;
			if (reference.identifier != null) {
				identifier_ = reference.identifier + "/" + specification;
			} else
				identifier_ = "/" + specification;
			identifier_ = identifier_.replaceAll("//", "/");
		} else {
			identifier_ = reference.identifier + "/" + specification;
		}
		if (!ConfigurationIdentifier.identifierPattern.matcher(identifier_)
				.matches())
			throw (new IllegalArgumentException(String.format(
					"Invalid configuration identifier: `%s`", identifier_)));

		identifier = identifier_.intern();

		synchronized (ConfigurationIdentifier.identifiers) {
			if (ConfigurationIdentifier.identifiers.containsKey(identifier))
				parameterIdentifier = ConfigurationIdentifier.identifiers
						.get(identifier);
			else {
				// slashIndex = identifier.lastIndexOf("/");

				// if (slashIndex >= 0)
				// parentIdentifier = identifier.substring(0, slashIndex)
				// .intern();
				// else
				// parentIdentifier = "";

				// if (parentIdentifier.length() == 0)
				// parentConfigurationIdentifier = ConfigurationIdentifier.root;
				// else
				// parentConfigurationIdentifier = ConfigurationIdentifier
				// .resolve(null, parentIdentifier);
				parameterIdentifier = new ConfigurationIdentifier(isAbsolute,
						identifier);
				ConfigurationIdentifier.identifiers.put(
						parameterIdentifier.identifier, parameterIdentifier);
			}
		}

		return parameterIdentifier;
	}

}
