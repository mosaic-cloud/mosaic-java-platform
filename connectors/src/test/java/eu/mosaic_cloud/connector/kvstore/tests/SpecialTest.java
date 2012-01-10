/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
package eu.mosaic_cloud.connector.kvstore.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.mosaic_cloud.connector.kvstore.KeyValueStoreConnector;
import eu.mosaic_cloud.core.TestLoggingHandler;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.utils.PojoDataEncoder;



public class SpecialTest {

	private static boolean done = false;

	public static void main(String[] args) {
		KeyValueStoreConnector<String> connector = null;
		try {
			IConfiguration config = PropertyTypeConfiguration.create(
					KeyValueConnectorTest.class.getClassLoader(),
					"special-test.prop");

			connector = KeyValueStoreConnector.create(config,
					new PojoDataEncoder<String>(String.class));

			Runtime.getRuntime().addShutdownHook(new Worker());
			while (!done) {
				doWork(connector);
			}
		} catch (Throwable e) {
			ExceptionTracer.traceIgnored(e);
		} finally {
			shutDown(connector);
		}

	}

	private static void shutDown(KeyValueStoreConnector<?> connector) {
		if (connector != null) {
			try {
				connector.destroy();
			} catch (Throwable e) {
				ExceptionTracer.traceIgnored(e);
			}
		}
	}

	@SuppressWarnings("static-access")
	private static void doWork(KeyValueStoreConnector<String> connector) {
		String key;
		String value;

		List<IOperationCompletionHandler<Boolean>> handlersSet = getHandlers("special set");
		List<IOperationCompletionHandler<String>> handlersGet = getHandlers("special get");
		List<IOperationCompletionHandler<Boolean>> handlersDel = getHandlers("special delete");

		while (true) {
			try {
				key = "key_" + UUID.randomUUID().toString();
				value = "value_" + UUID.randomUUID().toString();
				connector.set(key, value, handlersSet, null);
				Thread.currentThread().sleep(1000);
				connector.get(key, handlersGet, null);
				Thread.currentThread().sleep(1000);
				connector.delete(key, handlersDel, null);
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				ExceptionTracer.traceIgnored(e);
			}

		}
	}

	private static <T> List<IOperationCompletionHandler<T>> getHandlers(
			String testName) {
		IOperationCompletionHandler<T> handler = new TestLoggingHandler<T>(
				testName);
		List<IOperationCompletionHandler<T>> list = new ArrayList<IOperationCompletionHandler<T>>();
		list.add(handler);
		return list;
	}

	private static class Worker extends Thread {

		public void run() {
			SpecialTest.done = true;
		}
	}

}
