package mosaic.cloudlet.component.tests;

import mosaic.cloudlet.core.CloudletException;
import mosaic.cloudlet.runtime.CloudletManager;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;

public class TestRunner {

	public static CloudletManager runHelloWorld() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "hello-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runSimpleQueueConsumer() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "consumer-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runSimpleQueuePublisher() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "publisher-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runLoggingCloudlet() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "logging-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	public static CloudletManager runUserCloudlet() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "user-cloudlet.prop");
		final CloudletManager container = startCloudlet(configuration);
		return container;
	}

	private static CloudletManager startCloudlet(
			IConfiguration configuration) {
		final CloudletManager container = new CloudletManager(
				TestRunner.class.getClassLoader(), configuration);

		try {
			container.start();
		} catch (CloudletException e) {
			ExceptionTracer.traceDeferred(e);
		}
		return container;
	}

}
