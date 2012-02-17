/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
