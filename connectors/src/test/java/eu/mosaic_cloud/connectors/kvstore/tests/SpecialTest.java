/*
 * #%L
 * mosaic-connectors
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
package eu.mosaic_cloud.connectors.kvstore.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.mosaic_cloud.connectors.kvstore.KeyValueStoreConnector;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.tests.TestLoggingHandler;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

public class SpecialTest {

	private static boolean done = false;

	public static void main(String[] args) {
		BasicThreadingSecurityManager.initialize();
		ThreadingContext threading = BasicThreadingContext.create(
				MemcachedConnectorTest.class,
				AbortingExceptionTracer.defaultInstance.catcher);
		KeyValueStoreConnector<String> connector = null;
		try {
			IConfiguration config = PropertyTypeConfiguration.create(
					KeyValueConnectorTest.class.getClassLoader(),
					"special-test.prop");

			connector = KeyValueStoreConnector.create(config,
					new PojoDataEncoder<String>(String.class), threading);
			Threading.registerExitCallback(threading, SpecialTest.class,
					"exit-hook", new Worker());
			while (!SpecialTest.done) {
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
			key = "key_" + UUID.randomUUID().toString();
			value = "value_" + UUID.randomUUID().toString();
			connector.set(key, value, handlersSet, null);
			Threading.sleep(1000);
			connector.get(key, handlersGet, null);
			Threading.sleep(1000);
			connector.delete(key, handlersDel, null);
			Threading.sleep(1000);
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

	private static class Worker implements Runnable {

		@Override
		public void run() {
			SpecialTest.done = true;
		}
	}

}
