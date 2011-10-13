package mosaic.examples.feeds.test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;

public class ClassloaderTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String classPath = System.getProperty("java.class.path");
		String classpathArgument = "file:///D:/maven-repo/org/slf4j/slf4j-api/1.6.1/slf4j-api-1.6.1.jar|file:///d:/jdom-1.0.jar";
		final ClassLoader classLoader;
		final LinkedList<URL> classLoaderUrls = new LinkedList<URL>();
		for (final String classpathPart : classpathArgument.split("\\|"))
			if (classpathPart.length() > 0) {
				final URL classpathUrl;
				if (classpathPart.startsWith("http:")
						|| classpathPart.startsWith("file:")) {
					try {
						classpathUrl = new URL(classpathPart);
					} catch (final Exception exception) {
						throw (new IllegalArgumentException(String.format(
								"invalid class-path URL `%s`", classpathPart),
								exception));
					}
				} else
					throw (new IllegalArgumentException(String.format(
							"invalid class-path URL `%s`", classpathPart)));
				classLoaderUrls.add(classpathUrl);
				System.out.println("classpathurl: " + classpathUrl);
			}
		classLoader = new URLClassLoader(classLoaderUrls.toArray(new URL[0]),
				ClassloaderTest.class.getClassLoader());
		try {
			classLoader.loadClass("org.jdom.Element");
		} catch (final Exception exception) {
			throw (new IllegalArgumentException(
					String.format(
							"invalid component class `%s` (error encountered while resolving)",
							"org.jdom.Element"), exception));
		}
	}

}
