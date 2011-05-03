package mosaic.core.configuration;

public interface IConfiguration {
	<T extends Object> IConfigurationParameter<T> getParameter(
			final ConfigurationIdentifier identifier, final Class<T> valueClass);

	IConfiguration spliceConfiguration(final ConfigurationIdentifier relative);
}
