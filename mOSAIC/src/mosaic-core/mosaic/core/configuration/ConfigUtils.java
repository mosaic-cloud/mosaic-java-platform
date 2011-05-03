package mosaic.core.configuration;

public class ConfigUtils {
	public static <T extends Object> T resolveParameter(
			IConfiguration configuration, String identifier,
			Class<T> valueClass, T defaultValue) {
		if (configuration != null)
			return (configuration.getParameter(
					ConfigurationIdentifier.resolveRelative(identifier),
					valueClass).getValue(defaultValue));
		return defaultValue;
	}
}
