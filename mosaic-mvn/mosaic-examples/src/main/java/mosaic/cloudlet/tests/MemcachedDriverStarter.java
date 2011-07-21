package mosaic.cloudlet.tests;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.kvstore.memcached.MemcachedStub;
import mosaic.interop.kvstore.KeyValueSession;
import mosaic.interop.kvstore.MemcachedSession;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

public class MemcachedDriverStarter {
	public static void main(String... args) {
		IConfiguration configuration = PropertyTypeConfiguration.create(
				MemcachedDriverStarter.class.getClassLoader(), args[0]);
		IConfiguration kvConfiguration = configuration
				.spliceConfiguration(ConfigurationIdentifier
						.resolveAbsolute("kvstore"));

		ZeroMqChannel driverChannel = new ZeroMqChannel(ConfigUtils.resolveParameter(kvConfiguration,
				"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.register(MemcachedSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(kvConfiguration,
				"interop.channel.address", String.class, ""));
		final MemcachedStub stub = MemcachedStub.create(kvConfiguration,
				driverChannel);
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				stub.destroy();
				MosaicLogger.getLogger().info("MemcachedDriver stopped.");
			}
		});
	}
}
