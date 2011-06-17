package mosaic.connector.kvstore.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.interop.kvstore.KeyValueStub;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

@RunWith(SerialJunitRunner.class)
public class RedisConnectorTest {
	private static KeyValueStoreConnector connector;
	private static String keyPrefix;
	private static KeyValueStub driverStub;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				RedisConnectorTest.class.getClassLoader(),
				"redis-test.prop");
		connector = KeyValueStoreConnector.create(config);
		keyPrefix = UUID.randomUUID().toString();
		driverStub = KeyValueStub.create(config);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		connector.destroy();
		driverStub.destroy();
	}

	@Test
	public void testConnection() {
		Assert.assertNotNull(connector);
	}

	private static <T> List<IOperationCompletionHandler<T>> getHandlers(
			String testName) {
		IOperationCompletionHandler<T> handler = new TestLoggingHandler<T>(
				testName);
		List<IOperationCompletionHandler<T>> list = new ArrayList<IOperationCompletionHandler<T>>();
		list.add(handler);
		return list;
	}

	@Test
	public void testSet() {
		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("set 1");
		IResult<Boolean> r1 = connector.set(k1, "fantastic", handlers1, null);
		Assert.assertNotNull(r1);

		String k2 = keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = connector.set(k2, "famous", handlers2, null);
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

	@Test
	public void testGet() {
		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Object>> handlers = getHandlers("get");
		IResult<Object> r1 = connector.get(k1, handlers, null);

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

	@Test
	public void testDelete() {
		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("delete");
		IResult<Boolean> r1 = connector.delete(k1, handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List<IOperationCompletionHandler<Object>> handlers1 = getHandlers("get after delete");
		IResult<Object> r2 = connector.get(k1, handlers1, null);

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
	public void testList() {
		List<IOperationCompletionHandler<List<String>>> handlers = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlers.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r1 = connector.list(handlers, null);
		try {

			Assert.assertNotNull(r1.getResult());
			for (String key : r1.getResult()) {
				System.out.println(key);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public static void main(String... args) {
		JUnitCore.main("mosaic.connector.kvstore.tests.RedisConnectorTest");
	}

	public static void _main(String... args) throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				RedisConnectorTest.class.getClassLoader(), "redis-test.prop");
		KeyValueStoreConnector connector = KeyValueStoreConnector
				.create(config);
		String keyPrefix = UUID.randomUUID().toString();
		KeyValueStub driverStub = KeyValueStub.create(config);

		String k1 = keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("add 1");
		IResult<Boolean> r1 = connector.set(k1, "fantastic", handlers1, null);
		boolean result = r1.getResult();
		System.out.println("Set 1 result=" + result);

		List<IOperationCompletionHandler<Object>> handlers = getHandlers("get");
		IResult<Object> r3 = connector.get(k1, handlers, null);
		System.out.println(r3.getResult().toString());

		List<IOperationCompletionHandler<List<String>>> handlersl = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlersl.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r4 = connector.list(handlersl, null);
		List<String> list = r4.getResult();
		if (list != null)
			for (String key : list) {
				System.out.println(key);
			}

		List<IOperationCompletionHandler<Boolean>> handlersd = getHandlers("delete");
		IResult<Boolean> r5 = connector.delete(k1, handlersd, null);
		System.out.println(r5.getResult().toString());

		IResult<Object> r6 = connector.get(k1, handlers, null);
		System.out.println(r6.getResult());

		connector.destroy();
		driverStub.destroy();
	}
}
