
package eu.mosaic_cloud.connectors.tests;


import eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector;
import eu.mosaic_cloud.drivers.interop.kvstore.KeyValueStub;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.platform.interop.kvstore.KeyValueSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;


public class RiakKvStoreConnectorTest
		extends BaseKvStoreConnectorTest<GenericKvStoreConnector<String>>
{
	@Override
	public void setUp ()
	{
		this.context = RiakKvStoreConnectorTest.context_;
		this.connector = GenericKvStoreConnector.create (this.context.configuration, new PojoDataEncoder<String> (String.class), this.context.threading);
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
	{
		final Context context = new Context ();
		BaseConnectorTest.setupUpContext (RiakKvStoreConnectorTest.class, context, "riak-kv-store-connector-test.prop");
		context.driverChannel.register (KeyValueSession.DRIVER);
		context.driverStub = KeyValueStub.create (context.configuration, context.threading, context.driverChannel);
		RiakKvStoreConnectorTest.context_ = context;
	}
	
	@AfterClass
	public static void tearDownAfterClass ()
	{
		BaseConnectorTest.tearDownContext (RiakKvStoreConnectorTest.context_);
	}
	
	private static Context context_;
}
