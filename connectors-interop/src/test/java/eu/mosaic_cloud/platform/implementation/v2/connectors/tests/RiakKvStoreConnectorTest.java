/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.platform.implementation.v2.connectors.tests;


import eu.mosaic_cloud.drivers.kvstore.riak.interop.KeyValueStub;
import eu.mosaic_cloud.platform.implementation.v2.connectors.kvstore.generic.GenericKvStoreConnector;
import eu.mosaic_cloud.platform.implementation.v2.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.tools.configurations.implementations.basic.PropertiesBackedConfigurationSource;

import org.junit.AfterClass;
import org.junit.BeforeClass;


@SuppressWarnings ("boxing")
public class RiakKvStoreConnectorTest
			extends BaseKvStoreConnectorTest<GenericKvStoreConnector<String>>
{
	@Override
	public void setUp () {
		this.scenario = RiakKvStoreConnectorTest.scenario_;
		final ConnectorConfiguration configuration = ConnectorConfiguration.create (this.scenario.getConfiguration (), this.scenario.getEnvironment ());
		this.connector = GenericKvStoreConnector.create (configuration, PlainTextDataEncoder.DEFAULT_INSTANCE);
	}
	
	@BeforeClass
	public static void setUpBeforeClass () {
		final String host = System.getProperty (RiakKvStoreConnectorTest.MOSAIC_RIAK_HOST, RiakKvStoreConnectorTest.MOSAIC_RIAK_HOST_DEFAULT);
		final Integer port = Integer.valueOf (System.getProperty (RiakKvStoreConnectorTest.MOSAIC_RIAK_PORT, RiakKvStoreConnectorTest.MOSAIC_RIAK_PORT_DEFAULT));
		final PropertiesBackedConfigurationSource configuration = PropertiesBackedConfigurationSource.create ();
		configuration.overridePropertyValue ("interop.driver.endpoint", "inproc://fb012d6b-c238-4b31-b889-4121a318b2cb");
		configuration.overridePropertyValue ("interop.driver.identity", "fb012d6b-c238-4b31-b889-4121a318b2cb");
		configuration.overridePropertyValue ("kvstore.host", host);
		configuration.overridePropertyValue ("kvstore.port", port.toString ());
		configuration.overridePropertyValue ("kvstore.driver_name", "RIAKREST");
		configuration.overridePropertyValue ("kvstore.driver_threads", Integer.toString (1));
		configuration.overridePropertyValue ("kvstore.bucket", "tests");
		final Scenario scenario = new Scenario (RiakKvStoreConnectorTest.class, configuration);
		scenario.registerDriverRole (KeyValueSession.DRIVER);
		BaseConnectorTest.driverStub = KeyValueStub.createDetached (configuration, scenario.getThreading (), scenario.getDriverChannel ());
		RiakKvStoreConnectorTest.scenario_ = scenario;
	}
	
	@AfterClass
	public static void tearDownAfterClass () {
		BaseConnectorTest.tearDownScenario (RiakKvStoreConnectorTest.scenario_);
	}
	
	private static final String MOSAIC_RIAK_HOST = "mosaic.tests.resources.riak.host";
	private static final String MOSAIC_RIAK_HOST_DEFAULT = "127.0.0.1";
	private static final String MOSAIC_RIAK_PORT = "mosaic.tests.resources.riakrest.port";
	private static final String MOSAIC_RIAK_PORT_DEFAULT = "24637";
	private static Scenario scenario_;
}
