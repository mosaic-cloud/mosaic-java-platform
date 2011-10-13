package mosaic.connector;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class ConfigProperties {
	private static final String BUNDLE_NAME = "mosaic.connector.config"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(ConfigProperties.BUNDLE_NAME);

	private ConfigProperties() {
	}

	public static String getString(String key) {
		try {
			return ConfigProperties.RESOURCE_BUNDLE.getString(key); // NOPMD by georgiana on 10/13/11 10:05 AM
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
