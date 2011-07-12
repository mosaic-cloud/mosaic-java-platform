package mosaic.cloudlet.tests;

import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.queue.amqp.AmqpStub;
import mosaic.interop.amqp.AmqpSession;

public class AmqpDriverStarter {
	public static void main(String... args) {
		IConfiguration configuration = PropertyTypeConfiguration.create(
				AmqpDriverStarter.class.getClassLoader(), args[0]);
		IConfiguration kvConfiguration = configuration
				.spliceConfiguration(ConfigurationIdentifier
						.resolveAbsolute("queue"));

		ZeroMqChannel driverChannel = new ZeroMqChannel("driver.amqp.1",
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(AmqpSession.DRIVER);
		driverChannel.accept("tcp://127.0.0.1:31028");
		final AmqpStub stub = AmqpStub.create(kvConfiguration, driverChannel);
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				stub.destroy();
				MosaicLogger.getLogger().info("AmqpDriver stopped.");
			}
		});

	}
}
