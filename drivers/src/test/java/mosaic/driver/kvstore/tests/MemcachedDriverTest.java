/*
 * #%L
 * mosaic-driver
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
package mosaic.driver.kvstore.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.core.exceptions.ExceptionTracer;

import mosaic.core.Serial;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.kvstore.memcached.MemcachedDriver;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SerialJunitRunner.class)
@Serial
public class MemcachedDriverTest {
	private static MemcachedDriver wrapper;
	private static String keyPrefix;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		MemcachedDriverTest.wrapper = MemcachedDriver
				.create(PropertyTypeConfiguration.create(
						MemcachedDriverTest.class.getClassLoader(),
						"memcached-test.prop"));
		MemcachedDriverTest.keyPrefix = UUID.randomUUID().toString();
		MemcachedDriverTest.wrapper.registerClient(
				MemcachedDriverTest.keyPrefix, "test");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		MemcachedDriverTest.wrapper
				.unregisterClient(MemcachedDriverTest.keyPrefix);
		MemcachedDriverTest.wrapper.destroy();
	}

	@Before
	public void setUp() {
	}

	public void testConnection() {
		Assert.assertNotNull(MemcachedDriverTest.wrapper);
	}

	public void testSet() throws IOException {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		byte[] bytes1 = SerDesUtils.pojoToBytes("fantastic");
		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"set 1");
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper.invokeSetOperation(
				MemcachedDriverTest.keyPrefix, k1, 30, bytes1, handler1);
		Assert.assertNotNull(r1);

		String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
		byte[] bytes2 = SerDesUtils.pojoToBytes("famous");
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"set 2");
		IResult<Boolean> r2 = MemcachedDriverTest.wrapper.invokeSetOperation(
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
		IResult<byte[]> r1 = MemcachedDriverTest.wrapper.invokeGetOperation(
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
		IResult<Map<String, byte[]>> r1 = MemcachedDriverTest.wrapper
				.invokeGetBulkOperation(MemcachedDriverTest.keyPrefix, keys,
						handler);

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

		IResult<Boolean> r1 = MemcachedDriverTest.wrapper.invokeAddOperation(
				MemcachedDriverTest.keyPrefix, k1, 30, b1, handler1);
		IResult<Boolean> r2 = MemcachedDriverTest.wrapper.invokeAddOperation(
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
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokeReplaceOperation(MemcachedDriverTest.keyPrefix, k1, 30,
						b1, handler);
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
		IResult<byte[]> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(
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
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokeAppendOperation(MemcachedDriverTest.keyPrefix, k1, b1,
						handler);
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
		IResult<byte[]> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(
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
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokePrependOperation(MemcachedDriverTest.keyPrefix, k1, b1,
						handler);
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
		IResult<byte[]> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(
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
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper.invokeCASOperation(
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
		IResult<byte[]> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(
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
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokeDeleteOperation(MemcachedDriverTest.keyPrefix, k1,
						handler);
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
		IResult<byte[]> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(
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
		// FIXME
		// testAppend();
		// testPrepend();
		testDelete();
	}
}
