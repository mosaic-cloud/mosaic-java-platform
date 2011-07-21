package mosaic.cloudlet.tests;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.queue.amqp.AmqpStub;
import mosaic.interop.amqp.AmqpSession;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

public class AmqpDriverStarter {
	public static void main(String... args) {
		IConfiguration configuration = PropertyTypeConfiguration.create(
				AmqpDriverStarter.class.getClassLoader(), args[0]);
		IConfiguration amqpConfiguration = configuration
				.spliceConfiguration(ConfigurationIdentifier
						.resolveAbsolute("queue"));

		ZeroMqChannel driverChannel = new ZeroMqChannel(ConfigUtils.resolveParameter(amqpConfiguration,
				"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(AmqpSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(
				amqpConfiguration, "interop.channel.address",
				String.class, ""));
		final AmqpStub stub = AmqpStub.create(amqpConfiguration, driverChannel);
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				stub.destroy();
				MosaicLogger.getLogger().info("AmqpDriver stopped.");
			}
		});

	}
}
