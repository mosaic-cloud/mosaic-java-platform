package mosaic.cloudlet.tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import mosaic.cloudlet.core.CloudletException;
import mosaic.cloudlet.runtime.CloudletDummyContainer;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;

public class CloudletDummyContainerTest {
	public static void main(String... args) {
		IConfiguration configuration;

		for (int i = 0; i < args.length; i++) {
			configuration = PropertyTypeConfiguration.create(
					CloudletDummyContainerTest.class.getClassLoader(), args[i]);
			final CloudletDummyContainer container = new CloudletDummyContainer(
					CloudletDummyContainerTest.class.getClassLoader(),
					configuration);
			Runnable r = new Runnable() {

				@Override
				public void run() {
					try {
						container.start();
					} catch (CloudletException e) {
						ExceptionTracer.traceDeferred(e);
					}

				}
			};
			Thread t = new Thread(r);
			t.start();
		}
	}

	public void cloudletTest() throws FileNotFoundException, IOException {
		// final String descriptorResourceName;
		// final String classpath;
		// if (args.length == 0) {
		// MosaicLogger
		// .getLogger()
		// .warn("no cloudlet descriptor specified; using default value: `cloud.prop`");
		// descriptorResourceName = "cloud.prop";
		// classpath = null;
		// } else if (args.length == 1) {
		// if (args[0].length() == 0) {
		// MosaicLogger
		// .getLogger()
		// .warn("no cloudlet descriptor specified; using default value: `cloud.prop`");
		// descriptorResourceName = "cloud.prop";
		// } else
		// descriptorResourceName = args[0];
		// classpath = null;
		// } else if (args.length == 2) {
		// if (args[0].length() == 0) {
		// MosaicLogger
		// .getLogger()
		// .warn("no cloudlet descriptor specified; using default value: `cloud.prop`");
		// descriptorResourceName = "cloud.prop";
		// } else
		// descriptorResourceName = args[0];
		// classpath = args[1];
		// } else {
		// MosaicLogger
		// .getLogger()
		// .error("wrong arguments! expected arguments: <cloudlet-descriptor-resource> [<cloudlet-classpath>]");
		// System.exit(1);
		// return;
		// }
		// MosaicLogger.getLogger().trace(
		// "Using properties in " + descriptorResourceName);
		//
		// ClassLoader classLoader;
		// if (classpath != null) {
		// LinkedList<URL> classLoaderUrls = new LinkedList<URL>();
		// for (String classpathPart : classpath.split(":"))
		// if (classpathPart.length() > 0) {
		// try {
		// URL classpathUrl;
		// if (classpathPart.startsWith("http!"))
		// classpathUrl = new URL("http:"
		// + classpathPart.substring("http!".length()));
		// else
		// classpathUrl = new URL("file", "", classpathPart);
		// classLoaderUrls.add(classpathUrl);
		// } catch (MalformedURLException exception) {
		// MosaicLogger.getLogger().error(
		// "invalid application classpath URL: `"
		// + classpathPart + "`");
		// System.exit(1);
		// return;
		// }
		// }
		// classLoader = new URLClassLoader(
		// classLoaderUrls.toArray(new URL[0]),
		// ClassLoader.getSystemClassLoader());
		// } else
		// classLoader = ClassLoader.getSystemClassLoader();

		// Properties properties = new Properties(System.getProperties());
		// try {
		// InputStream descriptorStream = classLoader
		// .getResourceAsStream(descriptorResourceName);
		// if (descriptorStream == null) {
		// MosaicLogger.getLogger().error(
		// "no cloudlet descriptor resource found: `"
		// + descriptorResourceName + "`");
		// System.exit(1);
		// return;
		// }
		// properties.load(descriptorStream);
		// descriptorStream.close();
		// } catch (IOException exception) {
		// ExceptionTracer.traceIgnored(exception);
		// System.exit(1);
		// return;
		// }
		//
		// System.setProperties(properties);

		IConfiguration configuration;
		configuration = PropertyTypeConfiguration
				.create(CloudletDummyContainerTest.class.getClassLoader(),
						"cloud.prop");
		CloudletDummyContainer container = new CloudletDummyContainer(
				CloudletDummyContainerTest.class.getClassLoader(),
				configuration);

		try {
			container.start();
		} catch (CloudletException e) {
			ExceptionTracer.traceDeferred(e);
		}

	}

}
