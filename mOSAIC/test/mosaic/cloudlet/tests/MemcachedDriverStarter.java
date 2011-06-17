package mosaic.cloudlet.tests;

import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.kvstore.memcached.MemcachedStub;

public class MemcachedDriverStarter {
	public static void main(String... args) {
		IConfiguration configuration = PropertyTypeConfiguration.create(
				MemcachedDriverStarter.class.getClassLoader(), args[0]);
		IConfiguration kvConfiguration = configuration
				.spliceConfiguration(ConfigurationIdentifier
						.resolveAbsolute("kvstore"));
		final MemcachedStub stub = MemcachedStub.create(kvConfiguration);
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				stub.destroy();
				MosaicLogger.getLogger().info("MemcachedDriver stopped.");
			}
		});
	}
}
