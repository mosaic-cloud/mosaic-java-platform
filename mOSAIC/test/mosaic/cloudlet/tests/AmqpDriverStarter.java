package mosaic.cloudlet.tests;

import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.queue.amqp.AmqpStub;

public class AmqpDriverStarter {
	public static void main(String... args) {
		IConfiguration configuration = PropertyTypeConfiguration.create(
				AmqpDriverStarter.class.getClassLoader(), args[0]);
		IConfiguration kvConfiguration = configuration
				.spliceConfiguration(ConfigurationIdentifier
						.resolveAbsolute("queue"));
		final AmqpStub stub=AmqpStub.create(kvConfiguration);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			@Override
			public void run() {
				stub.destroy();
				MosaicLogger.getLogger().info("AmqpDriver stopped.");
			}
		});
		
	}
}
