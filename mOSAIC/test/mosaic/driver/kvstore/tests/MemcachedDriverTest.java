package mosaic.driver.kvstore.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

public class MemcachedDriverTest {
	private static MemcachedDriver wrapper;
	private static String keyPrefix;
	@SuppressWarnings("rawtypes")
	private IOperationCompletionHandler handler;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		wrapper = MemcachedDriver.create(PropertyTypeConfiguration.create(
				MemcachedDriverTest.class.getClassLoader(),
				"memcached-test.prop"));
		keyPrefix = UUID.randomUUID().toString();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wrapper.destroy();
	}

	@SuppressWarnings("rawtypes")
	@Before
	public void setUp() {
		handler = new TestLoggingHandler();
	}

	@Test
	public void testConnection() {
		Assert.assertNotNull(wrapper);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSet() {
		String k1 = keyPrefix + "_key_fantastic";
		IResult<Boolean> r1 = wrapper.invokeSetOperation(k1, 30, "fantastic",
				handler);
		Assert.assertNotNull(r1);

		String k2 = keyPrefix + "_key_famous";
		IResult<Boolean> r2 = wrapper.invokeSetOperation(k2, 30, "famous",
				handler);
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

	@SuppressWarnings("unchecked")
	@Test
	public void testGet() {
		String k1 = keyPrefix + "_key_fantastic";
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

	@SuppressWarnings("unchecked")
	@Test
	public void testGetBulk() {
		String k1 = keyPrefix + "_key_fantastic";
		String k2 = keyPrefix + "_key_famous";
		List<String> keys = new ArrayList<String>();
		keys.add(k1);
		keys.add(k2);
		IResult<Map<String, Object>> r1 = wrapper.invokeGetBulkOperation(keys,
				handler);

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

	@SuppressWarnings("unchecked")
	@Test
	public void testAdd() {
		String k1 = keyPrefix + "_key_fantastic";
		String k2 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = wrapper.invokeAddOperation(k1, 30, "wrong",
				handler);
		IResult<Boolean> r2 = wrapper.invokeAddOperation(k2, 30, "fabulous",
				handler);

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

	@SuppressWarnings("unchecked")
	@Test
	public void testReplace() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = wrapper.invokeReplaceOperation(k1, 30,
				"fantabulous", handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = wrapper.invokeGetOperation(k1, handler);

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

	@SuppressWarnings("unchecked")
	@Test
	public void testAppend() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = wrapper.invokeAppendOperation(k1,
				" and miraculous", handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = wrapper.invokeGetOperation(k1, handler);

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

	@SuppressWarnings("unchecked")
	@Test
	public void testPrepend() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = wrapper.invokePrependOperation(k1, "it is ",
				handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = wrapper.invokeGetOperation(k1, handler);

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

	@SuppressWarnings("unchecked")
	@Test
	public void testCAS() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = wrapper.invokeCASOperation(k1,
				"replaced by dummy", handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = wrapper.invokeGetOperation(k1, handler);

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

	@SuppressWarnings("unchecked")
	@Test
	public void testDelete() {
		String k1 = keyPrefix + "_key_fabulous";

		IResult<Boolean> r1 = wrapper.invokeDeleteOperation(k1, handler);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		IResult<Object> r2 = wrapper.invokeGetOperation(k1, handler);

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
}
