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


import java.util.Arrays;
import java.util.Map;

import eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.drivers.kvstore.interop.MemcachedStub;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.platform.interop.specs.kvstore.MemcachedSession;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


@Ignore
public class MemcacheKvStoreConnectorTest
		extends BaseKvStoreConnectorTest<MemcacheKvStoreConnector<String>>
{
	@Override
	public void setUp ()
	{
		this.scenario = MemcacheKvStoreConnectorTest.scenario_;
		final ConnectorConfiguration configuration = ConnectorConfiguration.create (this.scenario.getConfiguration (), this.scenario.getEnvironment ());
		this.connector = MemcacheKvStoreConnector.create (configuration, PojoDataEncoder.create (String.class));
	}
	
	@Override
	@Test
	public void test ()
	{
		this.testConnector ();
		this.testSet ();
		this.testGet ();
		this.testGetBulk ();
		this.testAdd ();
		this.testReplace ();
		this.testCas ();
		this.testList ();
		this.testDelete ();
	}
	
	protected void testAdd ()
	{
		final String k1 = this.scenario.keyPrefix + "_key_fantastic";
		final String k2 = this.scenario.keyPrefix + "_key_fabulous";
		Assert.assertFalse (this.awaitBooleanOutcome (this.connector.add (k1, 30, "wrong")));
		Assert.assertTrue (this.awaitBooleanOutcome (this.connector.add (k2, 30, "fabulous")));
	}
	
	protected void testAppend ()
	{
		final String k1 = this.scenario.keyPrefix + "_key_fabulous";
		Assert.assertTrue (this.awaitBooleanOutcome (this.connector.append (k1, " and miraculous")));
		Assert.assertEquals ("fantabulous and miraculous", this.awaitOutcome (this.connector.get (k1)));
	}
	
	protected void testCas ()
	{
		final String k1 = this.scenario.keyPrefix + "_key_fabulous";
		Assert.assertTrue (this.awaitBooleanOutcome (this.connector.cas (k1, "replaced by dummy")));
		Assert.assertEquals ("replaced by dummy", this.awaitOutcome (this.connector.get (k1)));
	}
	
	protected void testGetBulk ()
	{
		final String k1 = this.scenario.keyPrefix + "_key_fantastic";
		final String k2 = this.scenario.keyPrefix + "_key_famous";
		final Map<String, String> values = this.awaitOutcome (this.connector.getBulk (Arrays.asList (k1, k2)));
		Assert.assertNotNull (values);
		Assert.assertEquals ("fantastic", values.get (k1));
		Assert.assertEquals ("famous", values.get (k2));
	}
	
	@Override
	protected void testList ()
	{
		Assert.assertNull (this.awaitOutcome (this.connector.list ()));
	}
	
	protected void testPrepend ()
	{
		final String k1 = this.scenario.keyPrefix + "_key_fabulous";
		Assert.assertTrue (this.awaitBooleanOutcome (this.connector.prepend (k1, "it is ")));
		Assert.assertEquals ("it is fantabulous and miraculous", this.awaitOutcome (this.connector.get (k1)));
	}
	
	protected void testReplace ()
	{
		final String k1 = this.scenario.keyPrefix + "_key_fabulous";
		Assert.assertTrue (this.awaitBooleanOutcome (this.connector.replace (k1, 30, "fantabulous")));
		Assert.assertEquals ("fantabulous", this.awaitOutcome (this.connector.get (k1)));
	}
	
	@Override
	protected void testSet ()
	{
		final String k1 = this.scenario.keyPrefix + "_key_fantastic";
		final String k2 = this.scenario.keyPrefix + "_key_famous";
		Assert.assertTrue (this.awaitBooleanOutcome (this.connector.set (k1, 30, "fantastic")));
		Assert.assertTrue (this.awaitBooleanOutcome (this.connector.set (k2, 30, "famous")));
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
	{
		final String host = System.getProperty (MemcacheKvStoreConnectorTest.MOSAIC_MEMCACHED_HOST, MemcacheKvStoreConnectorTest.MOSAIC_MEMCACHED_HOST_DEFAULT);
		final Integer port = Integer.valueOf (System.getProperty (MemcacheKvStoreConnectorTest.MOSAIC_MEMCACHED_PORT, MemcacheKvStoreConnectorTest.MOSAIC_MEMCACHED_PORT_DEFAULT));
		final IConfiguration configuration = PropertyTypeConfiguration.create ();
		configuration.addParameter ("interop.channel.address", "inproc://f0bfd2cc-07ab-4df1-935c-22e80779bc87");
		configuration.addParameter ("interop.driver.identifier", "f0bfd2cc-07ab-4df1-935c-22e80779bc87");
		configuration.addParameter ("memcached.host_1", host);
		configuration.addParameter ("memcached.port_1", port);
		configuration.addParameter ("kvstore.driver_name", "MEMCACHED");
		configuration.addParameter ("kvstore.driver_threads", 1);
		configuration.addParameter ("kvstore.bucket", "test");
		configuration.addParameter ("kvstore.user", "test");
		configuration.addParameter ("kvstore.passwd", "test");
		final Scenario scenario = new Scenario (MemcacheKvStoreConnectorTest.class, configuration);
		scenario.registerDriverRole (KeyValueSession.DRIVER);
		scenario.registerDriverRole (MemcachedSession.DRIVER);
		BaseConnectorTest.driverStub = MemcachedStub.createDetached (configuration, scenario.getDriverChannel (), scenario.getThreading ());
		MemcacheKvStoreConnectorTest.scenario_ = scenario;
	}
	
	@AfterClass
	public static void tearDownAfterClass ()
	{
		BaseConnectorTest.tearDownScenario (MemcacheKvStoreConnectorTest.scenario_);
	}
	
	private static final String MOSAIC_MEMCACHED_HOST = "mosaic.tests.resources.memcached.host";
	private static final String MOSAIC_MEMCACHED_HOST_DEFAULT = "127.0.0.1";
	private static final String MOSAIC_MEMCACHED_PORT = "mosaic.tests.resources.memcached.port";
	private static final String MOSAIC_MEMCACHED_PORT_DEFAULT = "8091";
	private static Scenario scenario_;
}
