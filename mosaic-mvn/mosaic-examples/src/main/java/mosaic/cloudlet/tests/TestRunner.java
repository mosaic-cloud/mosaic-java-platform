package mosaic.cloudlet.tests;

import mosaic.cloudlet.core.CloudletException;
import mosaic.cloudlet.runtime.CloudletManager;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;

public class TestRunner {

	public static CloudletManager runHelloWorld() {
		IConfiguration configuration;

		configuration = PropertyTypeConfiguration.create(
				TestRunner.class.getClassLoader(), "hello-cloudlet");
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
