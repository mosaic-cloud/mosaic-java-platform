package mosaic.driver.kvstore.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.core.Serial;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.kvstore.BaseKeyValueDriver;
import mosaic.driver.kvstore.RedisDriver;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

@RunWith(SerialJunitRunner.class)
@Serial
public class RedisDriverTest {
	private static BaseKeyValueDriver wrapper;
	private static String keyPrefix;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		wrapper = RedisDriver.create(PropertyTypeConfiguration.create(
				RedisDriverTest.class.getClassLoader(), "redis-test.prop"));
		keyPrefix = UUID.randomUUID().toString();
		MosaicLogger.getLogger().trace("KEY: "+keyPrefix);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wrapper.destroy();
	}

	@Before
	public void setUp() {
	}

	public void testConnection() {
		Assert.assertNotNull(wrapper);
	}

	public void testSet() {
		String k1 = keyPrefix + "_key_fantastic";
		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"set 1");
		IResult<Boolean> r1 = wrapper.invokeSetOperation(k1, "fantastic",
				handler1);
		Assert.assertNotNull(r1);

		String k2 = keyPrefix + "_key_famous";
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"set 2");
		IResult<Boolean> r2 = wrapper
				.invokeSetOperation(k2, "famous", handler2);
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
		String k1 = keyPrefix + "_key_fantastic";
		IOperationCompletionHandler<Object> handler = new TestLoggingHandler<Object>(
				"get");
		IResult<Object> r1 = wrapper.invokeGetOperation(k1, handler);

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

	public void testList() {
		String k1 = keyPrefix + "_key_fantastic";
		String k2 = keyPrefix + "_key_famous";
		List<String> keys = new ArrayList<String>();
		keys.add(k1);
		keys.add(k2);
		IOperationCompletionHandler<List<String>> handler = new TestLoggingHandler<List<String>>(
				"list");
		IResult<List<String>> r1 = wrapper.invokeListOperation(handler);

		try {
			List<String> lresult = r1.getResult();
			Assert.assertNotNull(lresult);
			Assert.assertTrue(lresult.contains(k1));
			Assert.assertTrue(lresult.contains(k2));
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testDelete() {
		String k1 = keyPrefix + "_key_fantastic";
		String k2 = keyPrefix + "_key_famous";

		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"delete 1");
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"delete 2");
		IResult<Boolean> r1 = wrapper.invokeDeleteOperation(k1, handler1);
		IResult<Boolean> r2 = wrapper.invokeDeleteOperation(k2, handler2);
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

		IOperationCompletionHandler<Object> handler3 = new TestLoggingHandler<Object>(
				"check deleted");
		IResult<Object> r3 = wrapper.invokeGetOperation(k1, handler3);

		try {
			Assert.assertNull(r3.getResult());
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
		testList();
		testDelete();
	}

	public static void main(String... args) {
		JUnitCore.main("mosaic.driver.kvstore.tests.RedisDriverTest");
	}
}
