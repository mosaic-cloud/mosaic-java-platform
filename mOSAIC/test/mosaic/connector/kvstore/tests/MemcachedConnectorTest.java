package mosaic.connector.kvstore.tests;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.connector.kvstore.MemcachedStoreConnector;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.interop.MemcachedStub;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MemcachedConnectorTest {
	private static MemcachedStoreConnector connector;
	private static String keyPrefix;
	private List<IOperationCompletionHandler<Boolean>> handlersBool;
	private List<IOperationCompletionHandler<Object>> handlersObject;
	private static MemcachedStub driverStub;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		IConfiguration config = PropertyTypeConfiguration
				.create(new FileInputStream(
						"test/mosaic/driver/kvstore/tests/memcached-test.prop"));
		connector = MemcachedStoreConnector.create(config);
		keyPrefix = UUID.randomUUID().toString();
		driverStub = MemcachedStub.create(config);
		Thread driverThread = new Thread(driverStub);
		driverThread.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
//		connector.destroy();
//		driverStub.destroy();
	}

	@Before
	public void setUp() {
		handlersBool = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlersBool.add(new TestLoggingHandler<Boolean>());
		handlersObject=new ArrayList<IOperationCompletionHandler<Object>>();
		handlersObject.add(new TestLoggingHandler<Object>());

	}

	@Test
	public void testConnection() {
		Assert.assertNotNull(connector);
	}

	@Test
	public void testSet() {
		String k1 = keyPrefix + "_key_fantastic";
		IResult<Boolean> r1 = connector.set(k1, 30, "fantastic", handlersBool);
		Assert.assertNotNull(r1);

		String k2 = keyPrefix + "_key_famous";
		IResult<Boolean> r2 = connector.set(k2, 30, "famous", handlersBool);
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
		IResult<Object> r1 = connector.get(k1, handlersObject);

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
	public void testGetBulk() {
		String k1 = keyPrefix + "_key_fantastic";
		String k2 = keyPrefix + "_key_famous";
		List<String> keys = new ArrayList<String>();
		keys.add(k1);
		keys.add(k2);
		List<IOperationCompletionHandler<Map<String,Object>>> handlersMap=new ArrayList<IOperationCompletionHandler<Map<String,Object>>>();
		handlersMap.add(new TestLoggingHandler<Map<String,Object>>());
		IResult<Map<String, Object>> r1 = connector.getBulk(keys, handlersMap);

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

	@Test
	public void testAdd() {
		String k1 = keyPrefix + "_key_fantastic";
		String k2 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = connector.add(k1, 30, "wrong", handlersBool);
		IResult<Boolean> r2 = connector.add(k2, 30, "fabulous", handlersBool);

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

	@Test
	public void testReplace() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = connector
				.replace(k1, 30, "fantabulous", handlersBool);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = connector.get(k1, handlersObject);

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

	@Test
	public void testAppend() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = connector.append(k1, " and miraculous", handlersBool);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = connector.get(k1, handlersObject);

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

	@Test
	public void testPrepend() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = connector.prepend(k1, "it is ", handlersBool);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = connector.get(k1, handlersObject);

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

	@Test
	public void testCas() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = connector.cas(k1, "replaced by dummy", handlersBool);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = connector.get(k1, handlersObject);

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

	@Test
	public void testDelete() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = connector.delete(k1, handlersBool);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = connector.get(k1, handlersObject);

		try {
			Assert.assertNull(r2.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
		System.out.println("done");
	}
	

}
