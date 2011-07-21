package mosaic.cloudlet.component.tests;

import mosaic.cloudlet.core.CloudletException;
import mosaic.cloudlet.runtime.CloudletDummyContainer;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;

public class TestRunner {

	public static CloudletDummyContainer runHelloWorld() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "hello-cloudlet.prop");
		final CloudletDummyContainer container = startCloudlet(configuration);
		return container;
	}

	public static CloudletDummyContainer runSimpleQueueConsumer() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "consumer-cloudlet.prop");
		final CloudletDummyContainer container = startCloudlet(configuration);
		return container;
	}

	public static CloudletDummyContainer runSimpleQueuePublisher() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "publisher-cloudlet.prop");
		final CloudletDummyContainer container = startCloudlet(configuration);
		return container;
	}

	public static CloudletDummyContainer runLoggingCloudlet() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "logging-cloudlet.prop");
		final CloudletDummyContainer container = startCloudlet(configuration);
		return container;
	}

	public static CloudletDummyContainer runUserCloudlet() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "user-cloudlet.prop");
		final CloudletDummyContainer container = startCloudlet(configuration);
		return container;
	}

	private static CloudletDummyContainer startCloudlet(
			IConfiguration configuration) {
		final CloudletDummyContainer container = new CloudletDummyContainer(
				TestRunner.class.getClassLoader(), configuration);

		try {
			container.start();
		} catch (CloudletException e) {
			ExceptionTracer.traceDeferred(e);
		}
		return container;
	}

}
