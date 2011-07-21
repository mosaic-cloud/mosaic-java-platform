package mosaic.driver.kvstore.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.core.Serial;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
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
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		MemcachedDriverTest.wrapper.destroy();
	}

	@Before
	public void setUp() {
	}

	public void testConnection() {
		Assert.assertNotNull(MemcachedDriverTest.wrapper);
	}

	public void testSet() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"set 1");
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper.invokeSetOperation(
				k1, 30, "fantastic", handler1);
		Assert.assertNotNull(r1);

		String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"set 1");
		IResult<Boolean> r2 = MemcachedDriverTest.wrapper.invokeSetOperation(
				k2, 30, "famous", handler2);
		Assert.assertNotNull(r2);

		try {
			Assert.assertTrue(r1.getResult());
			Assert.assertTrue(r2.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testGet() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		IOperationCompletionHandler<Object> handler = new TestLoggingHandler<Object>(
				"get");
		IResult<Object> r1 = MemcachedDriverTest.wrapper.invokeGetOperation(k1,
				handler);

		try {
			Assert.assertEquals("fantastic", r1.getResult().toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testGetBulk() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		String k2 = MemcachedDriverTest.keyPrefix + "_key_famous";
		List<String> keys = new ArrayList<String>();
		keys.add(k1);
		keys.add(k2);
		IOperationCompletionHandler<Map<String, Object>> handler = new TestLoggingHandler<Map<String, Object>>(
				"getBulk");
		IResult<Map<String, Object>> r1 = MemcachedDriverTest.wrapper
				.invokeGetBulkOperation(keys, handler);

		try {
			Assert.assertEquals("fantastic", r1.getResult().get(k1).toString());
			Assert.assertEquals("famous", r1.getResult().get(k2).toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testAdd() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fantastic";
		String k2 = MemcachedDriverTest.keyPrefix + "_key_fabulous";
		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"add1");
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"add2");

		IResult<Boolean> r1 = MemcachedDriverTest.wrapper.invokeAddOperation(
				k1, 30, "wrong", handler1);
		IResult<Boolean> r2 = MemcachedDriverTest.wrapper.invokeAddOperation(
				k2, 30, "fabulous", handler2);

		try {
			Assert.assertFalse(r1.getResult());
			Assert.assertTrue(r2.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testReplace() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";

		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"replace");
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokeReplaceOperation(k1, 30, "fantabulous", handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
		IOperationCompletionHandler<Object> handler1 = new TestLoggingHandler<Object>(
				"Get after replace");
		IResult<Object> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(k1,
				handler1);

		try {
			Assert.assertEquals("fantabulous", r2.getResult().toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testAppend() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";

		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"append");
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokeAppendOperation(k1, " and miraculous", handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IOperationCompletionHandler<Object> handler1 = new TestLoggingHandler<Object>(
				"Get after append");
		IResult<Object> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(k1,
				handler1);

		try {
			Assert.assertEquals("fantabulous and miraculous", r2.getResult()
					.toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testPrepend() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";

		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"prepend");
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokePrependOperation(k1, "it is ", handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IOperationCompletionHandler<Object> handler1 = new TestLoggingHandler<Object>(
				"Get after prepend");
		IResult<Object> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(k1,
				handler1);

		try {
			Assert.assertEquals("it is fantabulous and miraculous", r2
					.getResult().toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testCAS() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";

		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"cas");
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper.invokeCASOperation(
				k1, "replaced by dummy", handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IOperationCompletionHandler<Object> handler1 = new TestLoggingHandler<Object>(
				"Get after cas");
		IResult<Object> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(k1,
				handler1);

		try {
			Assert.assertEquals("replaced by dummy", r2.getResult().toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testDelete() {
		String k1 = MemcachedDriverTest.keyPrefix + "_key_fabulous";

		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				"delete");
		IResult<Boolean> r1 = MemcachedDriverTest.wrapper
				.invokeDeleteOperation(k1, handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IOperationCompletionHandler<Object> handler1 = new TestLoggingHandler<Object>(
				"Get after delete");
		IResult<Object> r2 = MemcachedDriverTest.wrapper.invokeGetOperation(k1,
				handler1);

		try {
			Assert.assertNull(r2.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testDriver() {
		testConnection();
		testSet();
		testGet();
		testGetBulk();
		testAdd();
		testReplace();
		testAppend();
		testPrepend();
		testDelete();
	}
}
