/*
 * #%L
 * mosaic-drivers
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
package eu.mosaic_cloud.drivers.kvstore.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.drivers.kvstore.memcached.MemcachedDriver;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.tests.TestLoggingHandler;
import eu.mosaic_cloud.platform.core.utils.SerDesUtils;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MemcachedDriverTest {

	private BasicThreadingContext threadingContext;
	private MemcachedDriver wrapper;
	private static String keyPrefix;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		MemcachedDriverTest.keyPrefix = UUID.randomUUID().toString();
	}

	@Before
	public void setUp() throws IOException {
		QueueingExceptionTracer exceptions = QueueingExceptionTracer
				.create(NullExceptionTracer.defaultInstance);
		BasicThreadingSecurityManager.initialize();
		this.threadingContext = BasicThreadingContext.create(this,
				exceptions.catcher);
		this.threadingContext.initialize();
		this.wrapper = MemcachedDriver.create(PropertyTypeConfiguration.create(
				MemcachedDriverTest.class.getClassLoader(),
				"memcached-test.prop"), this.threadingContext);
		this.wrapper.registerClient(MemcachedDriverTest.keyPrefix, "test");
	}

	@After
	public void tearDown() throws Exception {
		this.wrapper.unregisterClient(MemcachedDriverTest.keyPrefix);
		this.wrapper.destroy();
		this.threadingContext.destroy();
	}

	public void testConnection() {
		Assert.assertNotNull(this.wrapper);
	}

	public void testSet() throws IOException {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		byte[] bytes1 = SerDesUtils.pojoToBytes("fantastic");
		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"set 1");
		IResult<Boolean> r1 = this.wrapper.invokeSetOperation(
				MemcachedDriverTest.keyPrefix, k1, 30, bytes1, handler1);
		Assert.assertNotNull(r1);

		String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
		byte[] bytes2 = SerDesUtils.pojoToBytes("famous");
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"set 2");
		IResult<Boolean> r2 = this.wrapper.invokeSetOperation(
				MemcachedDriverTest.keyPrefix, k2, 30, bytes2, handler2);
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
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		IOperationCompletionHandler<byte[]> handler = new TestLoggingHandler<byte[]>(
				"get");
		IResult<byte[]> r1 = this.wrapper.invokeGetOperation(
				MemcachedDriverTest.keyPrefix, k1, handler);

		try {
			Assert.assertEquals("fantastic",
					SerDesUtils.toObject(r1.getResult()));
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testGetBulk() throws IOException, ClassNotFoundException {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
		List<String> keys = new ArrayList<String>();
		keys.add(k1);
		keys.add(k2);
		IOperationCompletionHandler<Map<String, byte[]>> handler = new TestLoggingHandler<Map<String, byte[]>>(
				"getBulk");
		IResult<Map<String, byte[]>> r1 = this.wrapper.invokeGetBulkOperation(
				MemcachedDriverTest.keyPrefix, keys, handler);

		try {
			Assert.assertEquals("fantastic",
					SerDesUtils.toObject(r1.getResult().get(k1)).toString());
			Assert.assertEquals("famous",
					SerDesUtils.toObject(r1.getResult().get(k2)).toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testAdd() throws IOException {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		String k2 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		byte[] b1 = SerDesUtils.pojoToBytes("wrong");
		byte[] b2 = SerDesUtils.pojoToBytes("fabulous");
		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"add1");
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"add2");

		IResult<Boolean> r1 = this.wrapper.invokeAddOperation(
				MemcachedDriverTest.keyPrefix, k1, 30, b1, handler1);
		IResult<Boolean> r2 = this.wrapper.invokeAddOperation(
				MemcachedDriverTest.keyPrefix, k2, 30, b2, handler2);

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
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		byte[] b1 = SerDesUtils.pojoToBytes("fantabulous");
		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"replace");
		IResult<Boolean> r1 = this.wrapper.invokeReplaceOperation(
				MemcachedDriverTest.keyPrefix, k1, 30, b1, handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
		IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
				"Get after replace");
		IResult<byte[]> r2 = this.wrapper.invokeGetOperation(
				MemcachedDriverTest.keyPrefix, k1, handler1);

		try {
			Assert.assertEquals("fantabulous",
					SerDesUtils.toObject(r2.getResult()).toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testAppend() throws IOException, ClassNotFoundException {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		byte[] b1 = SerDesUtils.pojoToBytes(" and miraculous");
		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"append");
		IResult<Boolean> r1 = this.wrapper.invokeAppendOperation(
				MemcachedDriverTest.keyPrefix, k1, b1, handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
				"Get after append");
		IResult<byte[]> r2 = this.wrapper.invokeGetOperation(
				MemcachedDriverTest.keyPrefix, k1, handler1);

		try {
			Assert.assertEquals("fantabulous and miraculous", SerDesUtils
					.toObject(r2.getResult()).toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testPrepend() throws IOException, ClassNotFoundException {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		byte[] b1 = SerDesUtils.pojoToBytes("it is ");
		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"prepend");
		IResult<Boolean> r1 = this.wrapper.invokePrependOperation(
				MemcachedDriverTest.keyPrefix, k1, b1, handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
				"Get after prepend");
		IResult<byte[]> r2 = this.wrapper.invokeGetOperation(
				MemcachedDriverTest.keyPrefix, k1, handler1);

		try {
			Assert.assertEquals("it is fantabulous and miraculous", SerDesUtils
					.toObject(r2.getResult()).toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testCAS() throws IOException, ClassNotFoundException {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		byte[] b1 = SerDesUtils.pojoToBytes("replaced by dummy");
		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"cas");
		IResult<Boolean> r1 = this.wrapper.invokeCASOperation(
				MemcachedDriverTest.keyPrefix, k1, b1, handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
				"Get after cas");
		IResult<byte[]> r2 = this.wrapper.invokeGetOperation(
				MemcachedDriverTest.keyPrefix, k1, handler1);

		try {
			Assert.assertEquals("replaced by dummy",
					SerDesUtils.toObject(r2.getResult()).toString());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}
	}

	public void testDelete() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";

		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"delete");
		IResult<Boolean> r1 = this.wrapper.invokeDeleteOperation(
				MemcachedDriverTest.keyPrefix, k1, handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		} catch (ExecutionException e) {
			ExceptionTracer.traceIgnored(e);
			Assert.fail();
		}

		IOperationCompletionHandler<byte[]> handler1 = new TestLoggingHandler<byte[]>(
				"Get after delete");
		IResult<byte[]> r2 = this.wrapper.invokeGetOperation(
				MemcachedDriverTest.keyPrefix, k1, handler1);

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

	@Test
	public void testDriver() throws IOException, ClassNotFoundException {
		testConnection();
		testSet();
		testGet();
		testGetBulk();
		testAdd();
		testReplace();
		testDelete();
	}
}