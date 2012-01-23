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
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.connectors.kvstore.KeyValueStoreConnector;
import eu.mosaic_cloud.drivers.interop.kvstore.KeyValueStub;
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
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;

public class KeyValueConnectorTest {

	private KeyValueStoreConnector<String> connector;
	private static ThreadingContext threading;
	private static String keyPrefix;
	private static KeyValueStub driverStub;
	private static String storeType;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		BasicThreadingSecurityManager.initialize();
		KeyValueConnectorTest.threading = BasicThreadingContext.create(
				MemcachedConnectorTest.class,
				AbortingExceptionTracer.defaultInstance.catcher);
		IConfiguration config = PropertyTypeConfiguration.create(
				KeyValueConnectorTest.class.getClassLoader(), "kv-test.prop");
		KeyValueConnectorTest.storeType = ConfigUtils.resolveParameter(config,
				"kvstore.driver_name", String.class, "");

		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				KeyValueConnectorTest.threading,
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));

		KeyValueConnectorTest.driverStub = KeyValueStub.create(config,
				KeyValueConnectorTest.threading, driverChannel);
		KeyValueConnectorTest.keyPrefix = UUID.randomUUID().toString();
	}

	@Before
	public void setUp() throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				KeyValueConnectorTest.class.getClassLoader(), "kv-test.prop");
		this.connector = KeyValueStoreConnector.create(config,
				new PojoDataEncoder<String>(String.class),
				KeyValueConnectorTest.threading);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		KeyValueConnectorTest.driverStub.destroy();
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
		String k1 = KeyValueConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("set 1");
		IResult<Boolean> r1 = this.connector.set(k1, "fantastic", handlers1,
				null);
		Assert.assertNotNull(r1);

		String k2 = KeyValueConnectorTest.keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = this.connector.set(k2, "famous", handlers2, null);
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

	public void testDelete() {
		String k1 = KeyValueConnectorTest.keyPrefix + "_key_fantastic";
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
		BasicThreadingSecurityManager.initialize();
		ThreadingContext threading = BasicThreadingContext.create(
				KeyValueConnectorTest.class,
				AbortingExceptionTracer.defaultInstance.catcher);
		IConfiguration config = PropertyTypeConfiguration.create(
				KeyValueConnectorTest.class.getClassLoader(),
				"memcached-test.prop");
		KeyValueStoreConnector<String> connector = KeyValueStoreConnector
				.create(config, new PojoDataEncoder<String>(String.class),
						threading);
		String keyPrefix = UUID.randomUUID().toString();
		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				threading, AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));
		KeyValueStub driverStub = KeyValueStub.create(config, threading,
				driverChannel);

		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("add 1");
		IResult<Boolean> r1 = connector.set(k1, "fantastic", handlers1, null);
		boolean result = r1.getResult();
		Preconditions.checkArgument(result);

		List<IOperationCompletionHandler<String>> handlers = getHandlers("get");
		IResult<String> r3 = connector.get(k1, handlers, null);
		Preconditions.checkArgument("fantastic".equals(r3.getResult()
				.toString()));

		List<IOperationCompletionHandler<List<String>>> handlersl = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlersl.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r4 = connector.list(handlersl, null);
		List<String> list = r4.getResult();
		Preconditions.checkNotNull(list);

		List<IOperationCompletionHandler<Boolean>> handlersd = getHandlers("delete");
		IResult<Boolean> r5 = connector.delete(k1, handlersd, null);
		Preconditions.checkArgument(r5.getResult());

		IResult<String> r6 = connector.get(k1, handlers, null);
		Preconditions.checkArgument(r6.getResult() == null);

		connector.destroy();
		driverStub.destroy();
	}
}
