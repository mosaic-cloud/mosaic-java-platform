
package mosaic.connector.kvstore.tests;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.Serial;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.PojoDataEncoder;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith (SerialJunitRunner.class)
@Serial
public class KeyValueConnectorOnlyTest
{
	public void testConnection ()
	{
		Assert.assertNotNull (KeyValueConnectorOnlyTest.connector);
	}
	
	@Test
	public void testConnector ()
			throws IOException,
				ClassNotFoundException
	{
		this.testConnection ();
		this.testSet ();
		this.testGet ();
		this.testDelete ();
	}
	
	public void testDelete ()
	{
		final String k1 = KeyValueConnectorOnlyTest.keyPrefix + "_key_fantastic";
		final List<IOperationCompletionHandler<Boolean>> handlers = KeyValueConnectorOnlyTest.getHandlers ("delete");
		final IResult<Boolean> r1 = KeyValueConnectorOnlyTest.connector.delete (k1, handlers, null);
		try {
			Assert.assertTrue (r1.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
		} catch (final Exception e) {
			e.printStackTrace ();
			Assert.fail ();
		}
		final List<IOperationCompletionHandler<String>> handlers1 = KeyValueConnectorOnlyTest.getHandlers ("get after delete");
		final IResult<String> r2 = KeyValueConnectorOnlyTest.connector.get (k1, handlers1, null);
		try {
			Assert.assertNull (r2.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
		} catch (final Exception e) {
			e.printStackTrace ();
			Assert.fail ();
		}
	}
	
	public void testGet ()
			throws IOException,
				ClassNotFoundException
	{
		final String k1 = KeyValueConnectorOnlyTest.keyPrefix + "_key_fantastic";
		final List<IOperationCompletionHandler<String>> handlers = KeyValueConnectorOnlyTest.getHandlers ("get");
		final IResult<String> r1 = KeyValueConnectorOnlyTest.connector.get (k1, handlers, null);
		try {
			Assert.assertEquals ("fantastic", r1.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS).toString ());
		} catch (final Exception e) {
			e.printStackTrace ();
			Assert.fail ();
		}
	}
	
	public void testSet ()
			throws IOException
	{
		final String k1 = KeyValueConnectorOnlyTest.keyPrefix + "_key_fantastic";
		final List<IOperationCompletionHandler<Boolean>> handlers1 = KeyValueConnectorOnlyTest.getHandlers ("set 1");
		final IResult<Boolean> r1 = KeyValueConnectorOnlyTest.connector.set (k1, "fantastic", handlers1, null);
		Assert.assertNotNull (r1);
		final String k2 = KeyValueConnectorOnlyTest.keyPrefix + "_key_famous";
		final List<IOperationCompletionHandler<Boolean>> handlers2 = KeyValueConnectorOnlyTest.getHandlers ("set 2");
		final IResult<Boolean> r2 = KeyValueConnectorOnlyTest.connector.set (k2, "famous", handlers2, null);
		Assert.assertNotNull (r2);
		try {
			Assert.assertTrue (r1.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
			Assert.assertTrue (r2.getResult (KeyValueConnectorOnlyTest.timeout, TimeUnit.MILLISECONDS));
		} catch (final Exception e) {
			e.printStackTrace ();
			Assert.fail ();
		}
	}
	
	public static void main (final String[] arguments)
			throws Throwable
	{
		try {
			Preconditions.checkArgument ((arguments != null) && (arguments.length == 0));
			KeyValueConnectorOnlyTest.setUpBeforeClass ();
			new KeyValueConnectorOnlyTest ().testConnector ();
			KeyValueConnectorOnlyTest.tearDownAfterClass ();
		} catch (final Throwable exception) {
			exception.printStackTrace ();
			System.exit (1);
		}
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
			throws Throwable
	{
		final IConfiguration config = PropertyTypeConfiguration.create (KeyValueConnectorOnlyTest.class.getClassLoader (), "kv-test.prop");
		KeyValueConnectorOnlyTest.connector = KeyValueStoreConnector.create (config, new PojoDataEncoder<String> (String.class));
		KeyValueConnectorOnlyTest.keyPrefix = UUID.randomUUID ().toString ();
	}
	
	@AfterClass
	public static void tearDownAfterClass ()
			throws Throwable
	{
		KeyValueConnectorOnlyTest.connector.destroy ();
	}
	
	private static <T> List<IOperationCompletionHandler<T>> getHandlers (final String testName)
	{
		final IOperationCompletionHandler<T> handler = new TestLoggingHandler<T> (testName);
		final List<IOperationCompletionHandler<T>> list = new ArrayList<IOperationCompletionHandler<T>> ();
		list.add (handler);
		return list;
	}
	
	private static KeyValueStoreConnector<String> connector;
	private static String keyPrefix;
	private static String storeType;
	private static final long timeout = 1000 * 1000;
}
