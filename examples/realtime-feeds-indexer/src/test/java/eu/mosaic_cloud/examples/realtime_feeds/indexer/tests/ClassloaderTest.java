/*
 * #%L
 * mosaic-examples-realtime-feeds-indexer
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package eu.mosaic_cloud.examples.realtime_feeds.indexer.tests;

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
		for (final String classpathPart : classpathArgument.split("\\|")) {
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
				} else {
					throw (new IllegalArgumentException(String.format(
							"invalid class-path URL `%s`", classpathPart)));
				}
				classLoaderUrls.add(classpathUrl);
				System.out.println("classpathurl: " + classpathUrl);
			}
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