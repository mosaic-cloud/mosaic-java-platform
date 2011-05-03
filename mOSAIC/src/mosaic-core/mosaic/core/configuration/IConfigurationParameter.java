package mosaic.core.configuration;

public interface IConfigurationParameter<T extends Object> {
	ConfigurationIdentifier getIdentifier();

	T getValue(final T defaultValue);

	Class<T> getValueClass();
}
