package mosaic.driver;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class ConfigProperties {
	private static final String BUNDLE_NAME = "mosaic.driver.config"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(ConfigProperties.BUNDLE_NAME);

	private ConfigProperties() {
	}

	public static String getString(String key) {
		String retString;
		try {
			retString = ConfigProperties.RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			retString = '!' + key + '!';
		}
		return retString;
	}
}
