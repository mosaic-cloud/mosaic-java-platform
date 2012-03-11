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
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.utils.PojoDataEncoder;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class RedisKvStoreConnectorTest extends
        BaseKvStoreConnectorTest<GenericKvStoreConnector<String>> {

    private static final String MOSAIC_REDIS_HOST = "mosaic.tests.resources.redis.host";
    private static final String MOSAIC_REDIS_PORT = "mosaic.tests.resources.redis.port";

    private static Scenario scenario_;

    @BeforeClass
    public static void setUpBeforeClass() {
        final IConfiguration configuration = PropertyTypeConfiguration.create();

        // configuration.addParameter("interop.channel.address", "inproc://");
        configuration.addParameter("interop.channel.address", "tcp://127.0.0.1:31030");
        configuration.addParameter("interop.driver.identifier", "driver.redis.1");

        final String host = System.getProperty(RedisKvStoreConnectorTest.MOSAIC_REDIS_HOST,
                "127.0.0.1");
        configuration.addParameter("kvstore.host", host);
        final Integer port = Integer.valueOf(System.getProperty(
                RedisKvStoreConnectorTest.MOSAIC_REDIS_PORT, "6379"));
        configuration.addParameter("kvstore.port", port);
        configuration.addParameter("kvstore.driver_name", "REDIS");
        configuration.addParameter("kvstore.driver_threads", 1);
        configuration.addParameter("kvstore.bucket", "12");

        BaseConnectorTest.setUpScenario(RedisKvStoreConnectorTest.class);
        final Scenario scenario = new Scenario(RedisKvStoreConnectorTest.class, configuration);

        scenario.registerDriverRole(KeyValueSession.DRIVER);
        BaseConnectorTest.driverStub = KeyValueStub.createDetached(configuration,
                scenario.getThreading(), scenario.getDriverChannel());
        RedisKvStoreConnectorTest.scenario_ = scenario;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        BaseConnectorTest.tearDownScenario(RedisKvStoreConnectorTest.scenario_);
    }

    @Override
    public void setUp() {
        this.scenario = RedisKvStoreConnectorTest.scenario_;
        this.connector = GenericKvStoreConnector.create(this.scenario.getEnvironment(),
                new PojoDataEncoder<String>(String.class));
    }
}
