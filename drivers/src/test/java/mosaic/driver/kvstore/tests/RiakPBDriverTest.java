package mosaic.driver.kvstore.tests;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.kvstore.RiakPBDriver;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

//@RunWith(SerialJunitRunner.class)
//@Serial
public class RiakPBDriverTest {
	private static RiakPBDriver wrapper;
	private static String keyPrefix;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		RiakPBDriverTest.wrapper = RiakPBDriver
				.create(PropertyTypeConfiguration.create(
						RiakPBDriverTest.class.getClassLoader(),
						"riakpb-test.prop"));
		RiakPBDriverTest.keyPrefix = UUID.randomUUID().toString();
		RiakPBDriverTest.wrapper.registerClient(RiakPBDriverTest.keyPrefix,
				"test");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		RiakPBDriverTest.wrapper.unregisterClient(RiakPBDriverTest.keyPrefix);
		RiakPBDriverTest.wrapper.destroy();
	}

	public void testConnection() {
		Assert.assertNotNull(RiakPBDriverTest.wrapper);
	}

	public void testSet() throws IOException {
		String k1 = RiakPBDriverTest.keyPrefix + "_key_fantastic";
		byte[] b1 = SerDesUtils.pojoToBytes("fantastic");
		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"set 1");
		IResult<Boolean> r1 = RiakPBDriverTest.wrapper.invokeSetOperation(
				RiakPBDriverTest.keyPrefix, k1, b1, handler1);
		Assert.assertNotNull(r1);

		String k2 = RiakPBDriverTest.keyPrefix + "_key_famous";
		byte[] b2 = SerDesUtils.pojoToBytes("famous");
		IOperationCompletionHandler<Boolean> handler2 = new TestLoggingHandler<Boolean>(
				"set 2");
		IResult<Boolean> r2 = RiakPBDriverTest.wrapper.invokeSetOperation(
				RiakPBDriverTest.keyPrefix, k2, b2, handler2);
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
		String k1 = RiakPBDriverTest.keyPrefix + "_key_famous";
		IOperationCompletionHandler<byte[]> handler = new TestLoggingHandler<byte[]>(
				"get");
		IResult<byte[]> r1 = RiakPBDriverTest.wrapper.invokeGetOperation(
				RiakPBDriverTest.keyPrefix, k1, handler);

		try {
			Assert.assertEquals("famous", SerDesUtils.toObject(r1.getResult())
					.toString());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testList() {
		String k1 = RiakPBDriverTest.keyPrefix + "_key_fantastic";
		String k2 = RiakPBDriverTest.keyPrefix + "_key_famous";
		// List<String> keys = new ArrayList<String>();
		// keys.add(k1);
		// keys.add(k2);
		IOperationCompletionHandler<List<String>> handler = new TestLoggingHandler<List<String>>(
				"list");
		IResult<List<String>> r1 = RiakPBDriverTest.wrapper
				.invokeListOperation(RiakPBDriverTest.keyPrefix, handler);

		try {
			List<String> lresult = r1.getResult();
			Assert.assertNotNull(lresult);
			// Assert.assertEquals(keys.size(), lresult.size());
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
		String k1 = RiakPBDriverTest.keyPrefix + "_key_fantastic";

		IOperationCompletionHandler<Boolean> handler1 = new TestLoggingHandler<Boolean>(
				"delete 1");
		IResult<Boolean> r1 = RiakPBDriverTest.wrapper.invokeDeleteOperation(
				RiakPBDriverTest.keyPrefix, k1, handler1);
		try {
			Assert.assertTrue(r1.getResult());
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Assert.fail();
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		IOperationCompletionHandler<byte[]> handler3 = new TestLoggingHandler<byte[]>(
				"check deleted");
		IResult<byte[]> r3 = RiakPBDriverTest.wrapper.invokeGetOperation(
				RiakPBDriverTest.keyPrefix, k1, handler3);

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
	public void testDriver() throws IOException, ClassNotFoundException {
		testConnection();
		testSet();
		testGet();
		testList();
		testDelete();
	}

	// public static void main(String[] args) {
	// JUnitCore.main("mosaic.driver.kvstore.tests.RiakPBDriverTest");
	// }
}
