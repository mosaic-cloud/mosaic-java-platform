
package eu.mosaic_cloud.connectors.tests;


import eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector;
import eu.mosaic_cloud.drivers.interop.kvstore.KeyValueStub;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.platform.interop.kvstore.KeyValueSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;


@Ignore
public class RedisKvStoreConnectorTest
		extends BaseKvStoreConnectorTest<GenericKvStoreConnector<String>>
{
	@Override
	public void setUp ()
	{
		this.context = RedisKvStoreConnectorTest.context_;
		this.connector = GenericKvStoreConnector.create (this.context.configuration, new PojoDataEncoder<String> (String.class), this.context.threading);
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
	{
		final Context context = new Context ();
		BaseConnectorTest.setupUpContext (RedisKvStoreConnectorTest.class, context, "redis-kv-store-connector-test.prop");
		context.driverChannel.register (KeyValueSession.DRIVER);
		context.driverStub = KeyValueStub.create (context.configuration, context.threading, context.driverChannel);
		RedisKvStoreConnectorTest.context_ = context;
	}
	
	@AfterClass
	public static void tearDownAfterClass ()
	{
		BaseConnectorTest.tearDownContext (RedisKvStoreConnectorTest.context_);
	}
	
	private static Context context_;
}
