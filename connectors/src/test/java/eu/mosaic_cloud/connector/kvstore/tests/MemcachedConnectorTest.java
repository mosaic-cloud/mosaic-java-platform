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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;



import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.connector.kvstore.memcached.MemcachedStoreConnector;
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
import eu.mosaic_cloud.driver.interop.kvstore.memcached.MemcachedStub;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.interop.kvstore.MemcachedSession;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

@RunWith(SerialJunitRunner.class)
@Serial
@Ignore
public class MemcachedConnectorTest {
	private static MemcachedStoreConnector<String> connector;
	private static String keyPrefix;
	private static MemcachedStub driverStub;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				MemcachedConnectorTest.class.getClassLoader(),
				"memcached-test.prop");

		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.register(MemcachedSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));

		MemcachedConnectorTest.driverStub = MemcachedStub.create(config,
				driverChannel);
		MemcachedConnectorTest.connector = MemcachedStoreConnector.create(
				config, new PojoDataEncoder<String>(String.class));
		MemcachedConnectorTest.keyPrefix = UUID.randomUUID().toString();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		MemcachedConnectorTest.connector.destroy();
		MemcachedConnectorTest.driverStub.destroy();
	}

	public void testConnection() {
		Assert.assertNotNull(MemcachedConnectorTest.connector);
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
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("set 1");
		IResult<Boolean> r1 = MemcachedConnectorTest.connector.set(k1, 30,
				"fantastic", handlers1, null);
		Assert.assertNotNull(r1);

		String k2 = MemcachedConnectorTest.keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = MemcachedConnectorTest.connector.set(k2, 30,
				"famous", handlers2, null);
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
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<String>> handlers = getHandlers("get");
		IResult<String> r1 = MemcachedConnectorTest.connector.get(k1, handlers,
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

	public void testGetBulk() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fantastic";
		String k2 = MemcachedConnectorTest.keyPrefix + "_key_famous";
		List<String> keys = new ArrayList<String>();
		keys.add(k1);
		keys.add(k2);
		List<IOperationCompletionHandler<Map<String, String>>> handlersMap = new ArrayList<IOperationCompletionHandler<Map<String, String>>>();
		handlersMap
				.add(new TestLoggingHandler<Map<String, String>>("get bulk"));
		IResult<Map<String, String>> r1 = MemcachedConnectorTest.connector
				.getBulk(keys, handlersMap, null);

		try {
			Assert.assertEquals("fantastic", r1.getResult().get(k1).toString());
			Assert.assertEquals("famous", r1.getResult().get(k2).toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testAdd() throws IOException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fantastic";
		String k2 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";

		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("add 1");
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("add 2");

		IResult<Boolean> r1 = MemcachedConnectorTest.connector.add(k1, 30,
				"wrong", handlers1, null);
		IResult<Boolean> r2 = MemcachedConnectorTest.connector.add(k2, 30,
				"fabulous", handlers2, null);

		try {
			Assert.assertFalse(r1.getResult());
			Assert.assertTrue(r2.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testReplace() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("replace");

		IResult<Boolean> r1 = MemcachedConnectorTest.connector.replace(k1, 30,
				"fantabulous", handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after replace");
		IResult<String> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

		try {
			Assert.assertEquals("fantabulous", r2.getResult().toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

	}

	public void testAppend() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("append");

		IResult<Boolean> r1 = MemcachedConnectorTest.connector.append(k1,
				" and miraculous", handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		try {
			Thread.sleep(1000);
			List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after append");
			IResult<String> r2 = MemcachedConnectorTest.connector.get(k1,
					handlers1, null);

			Assert.assertEquals("fantabulous and miraculous", r2.getResult()
					.toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testPrepend() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("prepend");

		IResult<Boolean> r1 = MemcachedConnectorTest.connector.prepend(k1,
				"it is ", handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after prepend");
		IResult<String> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

		try {
			Assert.assertEquals("it is fantabulous and miraculous", r2
					.getResult().toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testCas() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("cas");
		IResult<Boolean> r1 = MemcachedConnectorTest.connector.cas(k1,
				"replaced by dummy", handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after cas");
		IResult<String> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

		try {
			Assert.assertEquals("replaced by dummy", r2.getResult().toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testDelete() {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("delete");
		IResult<Boolean> r1 = MemcachedConnectorTest.connector.delete(k1,
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
		IResult<String> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

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
		IResult<List<String>> r1 = MemcachedConnectorTest.connector.list(
				handlers, null);
		try {
			Assert.assertNull(r1.getResult());
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
		try {
			testConnection();
			testSet();
			testGet();
			testGetBulk();
			testAdd();
			testReplace();
//			testAppend();
//			testPrepend();
			testCas();
			testList();
			testDelete();
		} catch (Throwable e) {
			ExceptionTracer.traceIgnored(e);
		}
	}

	public static void main(String... args) throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				MemcachedConnectorTest.class.getClassLoader(),
				"memcached-test.prop");
		MemcachedStoreConnector<String> connector = MemcachedStoreConnector
				.create(config, new PojoDataEncoder<String>(String.class));
		String keyPrefix = UUID.randomUUID().toString();
		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));

		MemcachedStub driverStub = MemcachedStub.create(config, driverChannel);

		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("add 1");
		IResult<Boolean> r1 = connector.set(k1, 30, "fantastic", handlers1,
				null);
		boolean result = r1.getResult();
		Preconditions.checkArgument(result);

		String k2 = keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = connector.set(k2, 30, "famous", handlers2, null);
		result = r2.getResult();
		Preconditions.checkArgument(result);

		List<IOperationCompletionHandler<String>> handlers = getHandlers("get");
		IResult<String> r3 = connector.get(k1, handlers, null);
		Preconditions.checkArgument("fantastic".equals(r3.getResult()));

		// String k2 = keyPrefix + "_key_famous";
		// List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers(
		// "set 2");
		// IResult<Boolean> r2 = connector.set(k2, 30, "famous", handlers2,
		// null);
		connector.destroy();
		driverStub.destroy();

	}
}
