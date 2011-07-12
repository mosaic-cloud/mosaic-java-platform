package mosaic.connector.kvstore.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.Serial;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.interop.kvstore.KeyValueStub;
import mosaic.interop.kvstore.KeyValueSession;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

@RunWith(SerialJunitRunner.class)
@Serial
public class RedisConnectorTest {
	private static KeyValueStoreConnector connector;
	private static String keyPrefix;
	private static KeyValueStub driverStub;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				RedisConnectorTest.class.getClassLoader(), "redis-test.prop");

		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(config,
						"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(KeyValueSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(config,
				"interop.channel.address", String.class, ""));

		RedisConnectorTest.driverStub = KeyValueStub.create(config,
				driverChannel);
		RedisConnectorTest.connector = KeyValueStoreConnector.create(config);
		RedisConnectorTest.keyPrefix = UUID.randomUUID().toString();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		RedisConnectorTest.connector.destroy();
		RedisConnectorTest.driverStub.destroy();
	}

	@Test
	public void testConnection() {
		Assert.assertNotNull(RedisConnectorTest.connector);
	}

	private static <T> List<IOperationCompletionHandler<T>> getHandlers(
			String testName) {
		IOperationCompletionHandler<T> handler = new TestLoggingHandler<T>(
				testName);
		List<IOperationCompletionHandler<T>> list = new ArrayList<IOperationCompletionHandler<T>>();
		list.add(handler);
		return list;
	}

	public void testSet() {
		String k1 = RedisConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("set 1");
		IResult<Boolean> r1 = RedisConnectorTest.connector.set(k1, "fantastic",
				handlers1, null);
		Assert.assertNotNull(r1);

		String k2 = RedisConnectorTest.keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = RedisConnectorTest.connector.set(k2, "famous",
				handlers2, null);
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
		String k1 = RedisConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Object>> handlers = getHandlers("get");
		IResult<Object> r1 = RedisConnectorTest.connector.get(k1, handlers,
				null);

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

	public void testDelete() {
		String k1 = RedisConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("delete");
		IResult<Boolean> r1 = RedisConnectorTest.connector.delete(k1, handlers,
				null);
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
		IResult<Object> r2 = RedisConnectorTest.connector.get(k1, handlers1,
				null);

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

	public void testList() {
		List<IOperationCompletionHandler<List<String>>> handlers = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlers.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r1 = RedisConnectorTest.connector.list(handlers,
				null);
		try {
			Assert.assertNotNull(r1.getResult());
			String k2 = RedisConnectorTest.keyPrefix + "_key_famous";
			Assert.assertTrue(r1.getResult().contains(k2));
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testConnector() {
		testConnection();
		testSet();
		testGet();
		testList();
		testDelete();
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
		System.out.println("Set 1 result=" + result);

		List<IOperationCompletionHandler<Object>> handlers = getHandlers("get");
		IResult<Object> r3 = connector.get(k1, handlers, null);
		System.out.println(r3.getResult().toString());

		List<IOperationCompletionHandler<List<String>>> handlersl = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlersl.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r4 = connector.list(handlersl, null);
		List<String> list = r4.getResult();
		if (list != null) {
			for (String key : list) {
				System.out.println(key);
			}
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
