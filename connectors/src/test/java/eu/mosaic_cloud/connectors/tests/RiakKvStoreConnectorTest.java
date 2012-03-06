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
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class RiakKvStoreConnectorTest extends
        BaseKvStoreConnectorTest<GenericKvStoreConnector<String>> {

    private static Scenario scenario_;

    @BeforeClass
    public static void setUpBeforeClass() {
        final Scenario scenario = new Scenario();
        BaseConnectorTest.setUpScenario(RiakKvStoreConnectorTest.class, scenario,
                "riak-http-kv-store-driver-test.properties");
        scenario.driverChannel.register(KeyValueSession.DRIVER);
        scenario.driverStub = KeyValueStub.createDetached(scenario.configuration, scenario.threading,
                scenario.driverChannel);
        RiakKvStoreConnectorTest.scenario_ = scenario;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        BaseConnectorTest.tearDownScenario(RiakKvStoreConnectorTest.scenario_);
    }

    @Override
    public void setUp() {
        this.scenario = RiakKvStoreConnectorTest.scenario_;
        this.connector = GenericKvStoreConnector.create(this.scenario.configuration,
                this.scenario.environment, new PojoDataEncoder<String>(String.class));
    }
}
