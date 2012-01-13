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
package eu.mosaic_cloud.connector.kvstore.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;



import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.connector.kvstore.KeyValueStoreConnector;
import eu.mosaic_cloud.core.Serial;
import eu.mosaic_cloud.core.SerialJunitRunner;
import eu.mosaic_cloud.core.TestLoggingHandler;
import eu.mosaic_cloud.core.configuration.ConfigUtils;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.ops.IResult;
import eu.mosaic_cloud.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.driver.interop.kvstore.KeyValueStub;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

@RunWith(SerialJunitRunner.class)
@Serial
public class KeyValueConnectorTest {
	private static KeyValueStoreConnector<String> connector;
	private static String keyPrefix;
	private static KeyValueStub driverStub;
	private static String storeType;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				KeyValueConnectorTest.class.getClassLoader(), "kv-test.prop");
		KeyValueConnectorTest.storeType = ConfigUtils.resolveParameter(config,
				"kvstore.driver_name", String.class, "");

		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));

		KeyValueConnectorTest.driverStub = KeyValueStub.create(config,
				driverChannel);
		KeyValueConnectorTest.connector = KeyValueStoreConnector.create(config,
				new PojoDataEncoder<String>(String.class));
		KeyValueConnectorTest.keyPrefix = UUID.randomUUID().toString();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		KeyValueConnectorTest.connector.destroy();
		KeyValueConnectorTest.driverStub.destroy();
	}

	public void testConnection() {
		Assert.assertNotNull(KeyValueConnectorTest.connector);
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
		String k1 = KeyValueConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("set 1");
		IResult<Boolean> r1 = KeyValueConnectorTest.connector.set(k1,
				"fantastic", handlers1, null);
		Assert.assertNotNull(r1);

		String k2 = KeyValueConnectorTest.keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = KeyValueConnectorTest.connector.set(k2, "famous",
				handlers2, null);
		Assert.assertNotNull(r2);

		try {
			Assert.assertTrue(r1.getResult());
			Assert.assertTrue(r2.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testGet() throws IOException, ClassNotFoundException {
		String k1 = KeyValueConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<String>> handlers = getHandlers("get");
		IResult<String> r1 = KeyValueConnectorTest.connector.get(k1, handlers,
				null);

		try {
			Assert.assertEquals("fantastic", r1.getResult().toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testDelete() {
		String k1 = KeyValueConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("delete");
		IResult<Boolean> r1 = KeyValueConnectorTest.connector.delete(k1,
				handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after delete");
		IResult<String> r2 = KeyValueConnectorTest.connector.get(k1, handlers1,
				null);

		try {
			Assert.assertNull(r2.getResult());
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
		IResult<List<String>> r1 = KeyValueConnectorTest.connector.list(
				handlers, null);
		try {
			if (KeyValueConnectorTest.storeType.equalsIgnoreCase("memcached")) {
				Assert.assertNull(r1.getResult());
			} else {
				Assert.assertNotNull(r1.getResult());
			}
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	@Test
	public void testConnector() throws IOException, ClassNotFoundException {
		testConnection();
		testSet();
		testGet();
		testList();
		testDelete();
	}

	public static void main() throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				KeyValueConnectorTest.class.getClassLoader(),
				"memcached-test.prop");
		KeyValueStoreConnector<String> connector = KeyValueStoreConnector.create(
				config, new PojoDataEncoder<String>(String.class));
		String keyPrefix = UUID.randomUUID().toString();
		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));
		KeyValueStub driverStub = KeyValueStub.create(config, driverChannel);

		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("add 1");
		IResult<Boolean> r1 = connector.set(k1, "fantastic", handlers1, null);
		boolean result = r1.getResult();
		Preconditions.checkArgument(result);

		List<IOperationCompletionHandler<String>> handlers = getHandlers("get");
		IResult<String> r3 = connector.get(k1, handlers, null);
		Preconditions.checkArgument("fantastic".equals(r3.getResult().toString()));

		List<IOperationCompletionHandler<List<String>>> handlersl = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlersl.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r4 = connector.list(handlersl, null);
		List<String> list = r4.getResult();
		Preconditions.checkNotNull(list);

		List<IOperationCompletionHandler<Boolean>> handlersd = getHandlers("delete");
		IResult<Boolean> r5 = connector.delete(k1, handlersd, null);
		Preconditions.checkArgument(r5.getResult());

		IResult<String> r6 = connector.get(k1, handlers, null);
		Preconditions.checkArgument(r6.getResult()==null);

		connector.destroy();
		driverStub.destroy();
	}
}
