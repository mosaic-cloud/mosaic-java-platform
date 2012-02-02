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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.connectors.kvstore.memcached.MemcachedStoreConnector;
import eu.mosaic_cloud.drivers.interop.kvstore.memcached.MemcachedStub;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.tests.TestLoggingHandler;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.platform.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.platform.interop.kvstore.MemcachedSession;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MemcachedConnectorTest {

	private MemcachedStoreConnector<String> connector;
	private static BasicThreadingContext threading;
	private static String keyPrefix;
	private static MemcachedStub driverStub;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		BasicThreadingSecurityManager.initialize();
		MemcachedConnectorTest.threading = BasicThreadingContext.create(
				MemcachedConnectorTest.class,
				AbortingExceptionTracer.defaultInstance.catcher);
		MemcachedConnectorTest.threading.initialize();
		IConfiguration config = PropertyTypeConfiguration.create(
				MemcachedConnectorTest.class.getClassLoader(),
				"memcached-test.prop");

		ZeroMqChannel driverChannel = ZeroMqChannel.create(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				MemcachedConnectorTest.threading,
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.register(MemcachedSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));

		MemcachedConnectorTest.driverStub = MemcachedStub.create(config,
				driverChannel, MemcachedConnectorTest.threading);
		MemcachedConnectorTest.keyPrefix = UUID.randomUUID().toString();
	}

	@Before
	public void setUp() throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				MemcachedConnectorTest.class.getClassLoader(),
				"memcached-test.prop");
		this.connector = MemcachedStoreConnector.create(config,
				new PojoDataEncoder<String>(String.class),
				MemcachedConnectorTest.threading);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		MemcachedConnectorTest.driverStub.destroy();
		MemcachedConnectorTest.threading.destroy();
	}

	@After
	public void tearDown() throws Throwable {
		this.connector.destroy();
	}

	public void testConnection() {
		Assert.assertNotNull(this.connector);
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
		IResult<Boolean> r1 = this.connector.set(k1, 30, "fantastic",
				handlers1, null);
		Assert.assertNotNull(r1);

		String k2 = MemcachedConnectorTest.keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = this.connector.set(k2, 30, "famous", handlers2,
				null);
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
		IResult<String> r1 = this.connector.get(k1, handlers, null);

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
		IResult<Map<String, String>> r1 = this.connector.getBulk(keys,
				handlersMap, null);

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

		IResult<Boolean> r1 = this.connector.add(k1, 30, "wrong", handlers1,
				null);
		IResult<Boolean> r2 = this.connector.add(k2, 30, "fabulous", handlers2,
				null);

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

		IResult<Boolean> r1 = this.connector.replace(k1, 30, "fantabulous",
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

		List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after replace");
		IResult<String> r2 = this.connector.get(k1, handlers1, null);

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

		IResult<Boolean> r1 = this.connector.append(k1, " and miraculous",
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

		try {
			Threading.sleep(1000);
			List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after append");
			IResult<String> r2 = this.connector.get(k1, handlers1, null);

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

		IResult<Boolean> r1 = this.connector.prepend(k1, "it is ", handlers,
				null);
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
		IResult<String> r2 = this.connector.get(k1, handlers1, null);

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
		IResult<Boolean> r1 = this.connector.cas(k1, "replaced by dummy",
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

		List<IOperationCompletionHandler<String>> handlers1 = getHandlers("get after cas");
		IResult<String> r2 = this.connector.get(k1, handlers1, null);

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
		IResult<Boolean> r1 = this.connector.delete(k1, handlers, null);
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
		IResult<String> r2 = this.connector.get(k1, handlers1, null);

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
		IResult<List<String>> r1 = this.connector.list(handlers, null);
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
		BasicThreadingSecurityManager.initialize();
		BasicThreadingContext threading = BasicThreadingContext.create(
				MemcachedConnectorTest.class,
				AbortingExceptionTracer.defaultInstance.catcher);
		threading.destroy();
		IConfiguration config = PropertyTypeConfiguration.create(
				MemcachedConnectorTest.class.getClassLoader(),
				"memcached-test.prop");
		MemcachedStoreConnector<String> connector = MemcachedStoreConnector
				.create(config, new PojoDataEncoder<String>(String.class),
						threading);
		String keyPrefix = UUID.randomUUID().toString();
		ZeroMqChannel driverChannel = ZeroMqChannel.create(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				threading, AbortingExceptionTracer.defaultInstance);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));

		MemcachedStub driverStub = MemcachedStub.create(config, driverChannel,
				threading);

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
		threading.destroy();
	}
}
