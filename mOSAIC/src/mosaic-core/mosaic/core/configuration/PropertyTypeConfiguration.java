package mosaic.core.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import mosaic.core.exceptions.ExceptionTracer;

public final class PropertyTypeConfiguration implements IConfiguration {

	private final Properties properties;

	private final ConfigurationIdentifier root;

	public PropertyTypeConfiguration(Properties properties,
			ConfigurationIdentifier root) {
		super();
		this.properties = properties;
		this.root = root;
	}

	public PropertyTypeConfiguration(Properties properties) {
		super();
		this.properties = properties;
		this.root = ConfigurationIdentifier.root;
	}

	public final static PropertyTypeConfiguration create(
			ClassLoader classLoader, String resource) {
		final InputStream stream = classLoader.getResourceAsStream(resource);
		PropertyTypeConfiguration configuration = null;
		if (stream != null) {
			final Properties properties = new Properties(System.getProperties());
			try {
				properties.load(stream);
				stream.close();
				configuration = new PropertyTypeConfiguration(properties);
			} catch (final IOException exception) {
				ExceptionTracer.traceIgnored(exception);
			}
		}
		return configuration;
	}

	public final static PropertyTypeConfiguration create(InputStream stream)
			throws IOException {
		final Properties properties = new Properties(System.getProperties());
		if (stream != null) {
			properties.load(stream);
			stream.close();
		}
		PropertyTypeConfiguration configuration = new PropertyTypeConfiguration(
				properties);
		return configuration;
	}

	@Override
	public final <T extends Object> PropertyTypeParameter<T> getParameter(
			final ConfigurationIdentifier identifier, final Class<T> valueClass) {
		return (new PropertyTypeParameter<T>(identifier, valueClass));
	}

	@Override
	public final PropertyTypeConfiguration spliceConfiguration(
			final ConfigurationIdentifier relative) {
		final ConfigurationIdentifier root;
		if (relative.isAbsolute())
			root = relative;
		else
			root = this.root.resolve(relative);
		return (new PropertyTypeConfiguration(this.properties, root));
	}

	private <T extends Object> T decodeValue(Class<T> valueClass,
			String encodedValue) {
		T value;
		if (valueClass == String.class)
			value = valueClass.cast(encodedValue);
		else if (valueClass == Boolean.class)
			value = valueClass.cast(Boolean.parseBoolean(encodedValue));
		else if (valueClass == Integer.class)
			value = valueClass.cast(Integer.parseInt(encodedValue));
		else if (valueClass == Long.class)
			value = valueClass.cast(Long.parseLong(encodedValue));
		else if (valueClass == Double.class)
			value = valueClass.cast(Double.parseDouble(encodedValue));
		else if (valueClass == Float.class)
			value = valueClass.cast(Float.parseFloat(encodedValue));
		else if (valueClass == Character.class) {
			if (encodedValue.length() != 1)
				throw (new IllegalArgumentException());
			value = valueClass.cast(encodedValue.charAt(0));
		} else
			throw (new IllegalAccessError());
		return (value);
	}

	private final String selectEncodedValue(
			final ConfigurationIdentifier identifier) {
		final String key_;
		if (identifier.isAbsolute())
			if (identifier == ConfigurationIdentifier.root)
				key_ = "";
			else
				key_ = identifier.getIdentifier();
		else
			key_ = this.root.resolve(identifier).getIdentifier();
		final String key = key_.substring(1).replace('/', '.');
		synchronized (this) {
			final String encodedValue = this.properties.getProperty(key, null);
			return (encodedValue);
		}
	}

	public final class PropertyTypeParameter<T> implements
			IConfigurationParameter<T> {
		private final ConfigurationIdentifier identifier;
		private T value;
		private final Class<T> valueClass;

		public PropertyTypeParameter(ConfigurationIdentifier identifier,
				Class<T> valueClass) {
			super();
			this.identifier = identifier;
			this.valueClass = valueClass;
		}

		@Override
		public final ConfigurationIdentifier getIdentifier() {
			return (this.identifier);
		}

		@Override
		public final T getValue(final T defaultValue) {
			final T value;

			if (this.value == null) {
				if (this.valueClass == IConfiguration.class)
					this.value = this.valueClass
							.cast(PropertyTypeConfiguration.this
									.spliceConfiguration(this.identifier));
				else {
					final String encodedValue = PropertyTypeConfiguration.this
							.selectEncodedValue(this.identifier);
					if (encodedValue != null)
						this.value = PropertyTypeConfiguration.this
								.decodeValue(this.valueClass, encodedValue);
				}
			}

			if (this.value != null)
				value = this.value;
			else
				value = defaultValue;
			return (value);
		}

		@Override
		public final Class<T> getValueClass() {
			return (this.valueClass);
		}

	}
}
