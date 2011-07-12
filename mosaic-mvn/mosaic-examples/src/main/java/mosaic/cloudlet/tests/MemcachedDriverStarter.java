package mosaic.cloudlet.tests;

import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.kvstore.memcached.MemcachedStub;
import mosaic.interop.amqp.AmqpSession;

public class MemcachedDriverStarter {
	public static void main(String... args) {
		IConfiguration configuration = PropertyTypeConfiguration.create(
				MemcachedDriverStarter.class.getClassLoader(), args[0]);
		IConfiguration kvConfiguration = configuration
				.spliceConfiguration(ConfigurationIdentifier
						.resolveAbsolute("kvstore"));

		ZeroMqChannel driverChannel = new ZeroMqChannel("driver.memcached.1",
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(AmqpSession.DRIVER);
		driverChannel.accept("tcp://127.0.0.1:31028");
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
