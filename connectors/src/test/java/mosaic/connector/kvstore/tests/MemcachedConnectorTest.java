package mosaic.connector.kvstore.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.connector.kvstore.memcached.MemcachedStoreConnector;
import mosaic.core.Serial;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.PojoDataEncoder;
import mosaic.driver.interop.kvstore.memcached.MemcachedStub;
import mosaic.interop.kvstore.KeyValueSession;
import mosaic.interop.kvstore.MemcachedSession;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

@RunWith(SerialJunitRunner.class)
@Serial
public class MemcachedConnectorTest {
	private static MemcachedStoreConnector connector;
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
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testGet() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Object>> handlers = getHandlers("get");
		IResult<Object> r1 = MemcachedConnectorTest.connector.get(k1, handlers,
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

	public void testGetBulk() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fantastic";
		String k2 = MemcachedConnectorTest.keyPrefix + "_key_famous";
		List<String> keys = new ArrayList<String>();
		keys.add(k1);
		keys.add(k2);
		List<IOperationCompletionHandler<Map<String, Object>>> handlersMap = new ArrayList<IOperationCompletionHandler<Map<String, Object>>>();
		handlersMap
				.add(new TestLoggingHandler<Map<String, Object>>("get bulk"));
		IResult<Map<String, Object>> r1 = MemcachedConnectorTest.connector
				.getBulk(keys, handlersMap, null);

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
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
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
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List<IOperationCompletionHandler<Object>> handlers1 = getHandlers("get after replace");
		IResult<Object> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

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

	public void testAppend() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("append");

		IResult<Boolean> r1 = MemcachedConnectorTest.connector.append(k1,
				" and miraculous", handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List<IOperationCompletionHandler<Object>> handlers1 = getHandlers("get after append");
		IResult<Object> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

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

	public void testPrepend() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("prepend");

		IResult<Boolean> r1 = MemcachedConnectorTest.connector.prepend(k1,
				"it is ", handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List<IOperationCompletionHandler<Object>> handlers1 = getHandlers("get after prepend");
		IResult<Object> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

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

	public void testCas() throws IOException, ClassNotFoundException {
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("cas");
		IResult<Boolean> r1 = MemcachedConnectorTest.connector.cas(k1,
				"replaced by dummy", handlers, null);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		List<IOperationCompletionHandler<Object>> handlers1 = getHandlers("get after cas");
		IResult<Object> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

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
		String k1 = MemcachedConnectorTest.keyPrefix + "_key_fabulous";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("delete");
		IResult<Boolean> r1 = MemcachedConnectorTest.connector.delete(k1,
				handlers, null);
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
		IResult<Object> r2 = MemcachedConnectorTest.connector.get(k1,
				handlers1, null);

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
		IResult<List<String>> r1 = MemcachedConnectorTest.connector.list(
				handlers, null);
		try {
			Assert.assertNull(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testConnector() throws IOException, ClassNotFoundException {
		testConnection();
		testSet();
		testGet();
		testGetBulk();
		testAdd();
		testReplace();
		testAppend();
		testPrepend();
		testCas();
		testList();
		testDelete();
	}

	public static void main(String... args) throws Throwable {
		IConfiguration config = PropertyTypeConfiguration.create(
				MemcachedConnectorTest.class.getClassLoader(),
				"memcached-test.prop");
		MemcachedStoreConnector connector = MemcachedStoreConnector.create(
				config, new PojoDataEncoder<String>(String.class));
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
		System.out.println("Set 1 result=" + result);

		String k2 = keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = connector.set(k2, 30, "famous", handlers2, null);
		result = r2.getResult();
		System.out.println("Set 2 result=" + result);

		List<IOperationCompletionHandler<Object>> handlers = getHandlers("get");
		IResult<Object> r3 = connector.get(k1, handlers, null);
		System.out.println(r3.getResult().toString());

		// String k2 = keyPrefix + "_key_famous";
		// List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers(
		// "set 2");
		// IResult<Boolean> r2 = connector.set(k2, 30, "famous", handlers2,
		// null);
		connector.destroy();
		driverStub.destroy();

	}
}