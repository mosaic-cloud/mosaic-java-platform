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

import java.util.UUID;

import eu.mosaic_cloud.connectors.core.IConnector;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class BaseConnectorTest<TConnector extends IConnector, TScenario extends BaseScenario> {

    protected static MosaicLogger logger;
    protected static AbstractDriverStub driverStub;
    protected static String connectorIdentity;
    protected TScenario scenario;
    protected TConnector connector;

    protected static <TScenario extends BaseScenario> void setUpScenario(
            final Class<? extends BaseConnectorTest<?, TScenario>> owner) {
        // create configuration
        BasicThreadingSecurityManager.initialize();
        BaseConnectorTest.connectorIdentity = UUID.randomUUID().toString();

        // initialize logging system
        BaseConnectorTest.logger = MosaicLogger.createLogger(owner);
    }

    protected static void tearDownScenario(final BaseScenario scenario) {
        if (BaseConnectorTest.driverStub != null) {
            BaseConnectorTest.driverStub.destroy();
        }
        scenario.destroy();
    }

    protected void await(final CallbackCompletion<?> completion) {
        Assert.assertTrue(completion.await(this.scenario.getPoolTimeout()));
    }

    protected boolean awaitBooleanOutcome(final CallbackCompletion<Boolean> completion) {
        this.await(completion);
        return this.getBooleanOutcome(completion);
    }

    protected <Outcome> Outcome awaitOutcome(final CallbackCompletion<Outcome> completion) {
        this.await(completion);
        return this.getOutcome(completion);
    }

    protected boolean awaitSuccess(final CallbackCompletion<?> completion) {
        this.await(completion);
        Assert.assertTrue(completion.isCompleted());
        Assert.assertEquals(null, completion.getException());
        return true;
    }

    protected boolean getBooleanOutcome(final CallbackCompletion<Boolean> completion) {
        final Boolean value = this.getOutcome(completion);
        Assert.assertNotNull(value);
        return value.booleanValue();
    }

    protected <Outcome> Outcome getOutcome(final CallbackCompletion<Outcome> completion) {
        Assert.assertTrue(completion.isCompleted());
        Assert.assertEquals(null, completion.getException());
        return completion.getOutcome();
    }

    @Before
    public abstract void setUp();

    @After
    public void tearDown() {
        try {
            if (this.connector != null) {
                this.awaitSuccess(this.connector.destroy());
            }
        } finally {
            this.connector = null;
            this.scenario = null;
        }
    }

    @Test
    public abstract void test();

    protected void testConnector() {
        Assert.assertNotNull(this.connector);
        Assert.assertTrue(this.awaitSuccess(this.connector.initialize()));
    }
}
