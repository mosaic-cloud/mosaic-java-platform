package mosaic.cloudlet.tests;

import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.driver.interop.kvstore.memcached.MemcachedStub;

public class MemcachedDriverStarter {
	public static void main(String... args) {
		IConfiguration configuration = PropertyTypeConfiguration.create(
				MemcachedDriverStarter.class.getClassLoader(), args[0]);
		IConfiguration kvConfiguration = configuration
				.spliceConfiguration(ConfigurationIdentifier
						.resolveAbsolute("kvstore"));
		MemcachedStub.create(kvConfiguration);
	}
}
