/*
 * #%L
 * mosaic-connector
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.connector.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;


import mosaic.core.exceptions.ExceptionTracer;

import mosaic.connector.components.ResourceComponentCallbacks.ResourceType;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.PojoDataEncoder;
import mosaic.interop.idl.ChannelData;

import com.google.common.base.Preconditions;

public class KeyValueConnectorCompTest {
	private IConfiguration configuration;
	private KeyValueStoreConnector<String> connector;
	private String keyPrefix;
	private String storeType;

	public KeyValueConnectorCompTest() throws Throwable {
		this.configuration = PropertyTypeConfiguration.create(
				KeyValueConnectorCompTest.class.getClassLoader(),
				"kv-conn-test.prop");
		storeType = ConfigUtils.resolveParameter(this.configuration,
				"kvstore.driver_name", String.class, "");
		keyPrefix = UUID.randomUUID().toString();
		ResourceFinder.getResourceFinder().findResource(ResourceType.KEY_VALUE,
				new Callback());

	}

	public void testConnection() {
		Preconditions.checkNotNull(this.connector, "Connector not created");
	}

	private static <T> List<IOperationCompletionHandler<T>> getHandlers(
			String testName) {
		IOperationCompletionHandler<T> handler = new TestLoggingHandler<T>(
				testName);
		List<IOperationCompletionHandler<T>> list = new ArrayList<IOperationCompletionHandler<T>>();
		list.add(handler);
		return list;
	}

	public void testSet() throws IOException {
		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("set 1");
		IResult<Boolean> r1 = this.connector.set(k1, "fantastic", handlers1,
				null);
		Preconditions.checkNotNull(r1);

		String k2 = keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = this.connector.set(k2, "famous", handlers2, null);
		Preconditions.checkNotNull(r2);

		try {
			Preconditions.checkState(r1.getResult(), "Set 1 returned false.");
			Preconditions.checkState(r2.getResult(), "Set 2 returned false.");
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testGet() throws IOException, ClassNotFoundException {
		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<String>> handlers = getHandlers("get");
		IResult<String> r1 = this.connector.get(k1, handlers, null);

		try {
			Preconditions.checkState(
					"fantastic".equals(r1.getResult().toString()),
					"Get returned something wrong");
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testDelete() {
		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("delete");
		IResult<Boolean> r1 = this.connector.delete(k1, handlers, null);
		try {
			Preconditions.checkState(r1.getResult(), "Object not deleted.");
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after delete");
		IResult<String> r2 = this.connector.get(k1, handlers1, null);

		try {
			Preconditions.checkState(r2.getResult() == null,
					"Object still exists after delete");
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testList() {
		List<IOperationCompletionHandler<List<String>>> handlers = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlers.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r1 = this.connector.list(handlers, null);
		try {
			if (storeType.equalsIgnoreCase("memcached")) {
				Preconditions.checkState(r1.getResult() == null);
			} else {
				Preconditions
						.checkNotNull(r1.getResult(), "List returned null");

			}
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testConnector() throws IOException, ClassNotFoundException {
		testConnection();
		testSet();
		testGet();
		testList();
		testDelete();
	}

	public static void test() throws Throwable {
		new KeyValueConnectorCompTest();
	}

	class Callback implements IFinderCallback {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * mosaic.connector.temp.IFinderCallback#resourceFound(mosaic.interop
		 * .idl.ChannelData)
		 */
		@Override
		public void resourceFound(ChannelData channel) throws Throwable {
			KeyValueConnectorCompTest.this.configuration
					.addParameter("interop.driver.identifier",
							channel.getChannelIdentifier());
			KeyValueConnectorCompTest.this.configuration.addParameter(
					"interop.channel.address", channel.getChannelEndpoint());
			KeyValueConnectorCompTest.this.connector = KeyValueStoreConnector
					.create(KeyValueConnectorCompTest.this.configuration,
							new PojoDataEncoder<String>(String.class));
			KeyValueConnectorCompTest.this.testConnector();
			KeyValueConnectorCompTest.this.connector.destroy();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see mosaic.connector.temp.IFinderCallback#resourceNotFound()
		 */
		@Override
		public void resourceNotFound() {
			MosaicLogger.getLogger().error("Callback - Resource not found");
		}
	}
}
