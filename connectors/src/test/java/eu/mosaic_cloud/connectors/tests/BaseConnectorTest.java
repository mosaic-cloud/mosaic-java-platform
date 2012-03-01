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

import eu.mosaic_cloud.connectors.core.IConnector;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class BaseConnectorTest<Connector extends IConnector, Scenario extends BaseConnectorTest.BaseScenario<?>> {

    public static class BaseScenario<DriverStub extends AbstractDriverStub> {

        public IConfiguration configuration;

        public ZeroMqChannel driverChannel;

        public DriverStub driverStub;

        public TranscriptExceptionTracer exceptions;

        public QueueingExceptionTracer exceptions_;

        public MosaicLogger logger;

        public long poolTimeout = 1000;

        public BasicThreadingContext threading;

        public Transcript transcript;
    }

    protected Connector connector;

    protected Scenario scenario;

    protected static <Scenario extends BaseScenario<?>> void setUpScenario(
            final Class<? extends BaseConnectorTest<?, Scenario>> owner, final Scenario scenario,
            final String configuration) {
        BasicThreadingSecurityManager.initialize();
        scenario.logger = MosaicLogger.createLogger(owner);
        scenario.transcript = Transcript.create(owner);
        scenario.exceptions_ = QueueingExceptionTracer.create(NullExceptionTracer.defaultInstance);
        scenario.exceptions = TranscriptExceptionTracer.create(scenario.transcript,
                scenario.exceptions_);
        if (configuration != null) {
            scenario.configuration = PropertyTypeConfiguration.create(owner.getClassLoader(),
                    configuration);
        } else {
            scenario.configuration = PropertyTypeConfiguration.create();
        }
        scenario.threading = BasicThreadingContext.create(owner, scenario.exceptions.catcher);
        scenario.threading.initialize();
        final String driverIdentity = ConfigUtils.resolveParameter(scenario.configuration,
                "interop.driver.identifier", String.class, "");
        final String driverEndpoint = ConfigUtils.resolveParameter(scenario.configuration,
                "interop.channel.address", String.class, "");
        scenario.driverChannel = ZeroMqChannel.create(driverIdentity, scenario.threading,
                AbortingExceptionTracer.defaultInstance);
        scenario.driverChannel.accept(driverEndpoint);
    }

    protected static void tearDownScenario(final BaseScenario<?> scenario) {
        if (scenario.driverStub != null) {
            scenario.driverStub.destroy();
        }
        Assert.assertTrue(scenario.driverChannel.terminate(scenario.poolTimeout));
        Assert.assertTrue(scenario.threading.destroy(scenario.poolTimeout));
    }

    protected void await(final CallbackCompletion<?> completion) {
        Assert.assertTrue(completion.await(this.scenario.poolTimeout));
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
        this.awaitSuccess(this.connector.destroy());
        this.scenario = null;
    }

    @Test
    public abstract void test();

    protected void testConnector() {
        Assert.assertNotNull(this.connector);
    }
}
