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
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
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

    	public BasicCallbackReactor callbacks;

    	public ChannelFactory channelFactory;

    	public ChannelResolver channelResolver;

        public IConfiguration configuration;

        public ZeroMqChannel connectorChannel;

        public String connectorIdentity;

        public ZeroMqChannel driverChannel;

        public String driverEndpoint;

        public String driverIdentity;

        public DriverStub driverStub;

        public ConnectorEnvironment environment;

        public TranscriptExceptionTracer exceptions;

        public QueueingExceptionTracer exceptionsQueue;

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
        scenario.configuration = PropertyTypeConfiguration.create(owner.getClassLoader(), configuration);
        scenario.logger = MosaicLogger.createLogger(owner);
        scenario.transcript = Transcript.create(owner);
        scenario.exceptionsQueue = QueueingExceptionTracer.create(NullExceptionTracer.defaultInstance);
        scenario.exceptions = TranscriptExceptionTracer.create(scenario.transcript,
                scenario.exceptionsQueue);
        scenario.threading = BasicThreadingContext.create(owner, scenario.exceptions, scenario.exceptions.catcher);
        scenario.threading.initialize();
        scenario.callbacks = BasicCallbackReactor.create(scenario.threading, scenario.exceptions);
        scenario.callbacks.initialize();
        scenario.connectorIdentity = UUID.randomUUID().toString();
        scenario.driverIdentity = ConfigUtils.resolveParameter(scenario.configuration,
                "interop.driver.identifier", String.class, "");
        scenario.driverEndpoint = ConfigUtils.resolveParameter(scenario.configuration,
                "interop.channel.address", String.class, "");
        scenario.connectorChannel = ZeroMqChannel.create(scenario.connectorIdentity, scenario.threading,
                scenario.exceptions);
        scenario.driverChannel = ZeroMqChannel.create(scenario.driverIdentity, scenario.threading,
                scenario.exceptions);
        scenario.driverChannel.accept(scenario.driverEndpoint);
        scenario.channelFactory = new ChannelFactory() {
        	@Override
			public final Channel create() {
        		return scenario.connectorChannel;
			}
		};
        scenario.channelResolver = new ChannelResolver() {
        	@Override
			public final void resolve(String target, ResolverCallbacks callbacks) {
				Assert.assertEquals(scenario.driverIdentity, target);
				callbacks.resolved(this, target, scenario.driverIdentity, scenario.driverEndpoint);
			}
		};
		scenario.environment = ConnectorEnvironment.create (
				scenario.callbacks, scenario.threading, scenario.exceptions,
				scenario.channelFactory, scenario.channelResolver);
    }

    protected static void tearDownScenario(final BaseScenario<?> scenario) {
        if (scenario.driverStub != null) {
            scenario.driverStub.destroy();
        }
        Assert.assertTrue(scenario.driverChannel.terminate(scenario.poolTimeout));
        Assert.assertTrue(scenario.connectorChannel.terminate(scenario.poolTimeout));
        Assert.assertTrue(scenario.callbacks.destroy(scenario.poolTimeout));
        Assert.assertTrue(scenario.threading.destroy(scenario.poolTimeout));
        Assert.assertNull(scenario.exceptionsQueue.queue.poll());
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
    	try {
	    	if (this.connector != null)
	            this.awaitSuccess(this.connector.destroy());
    	} finally {
	    	this.connector = null;
	        this.scenario = null;
    	}
    }

    @Test
    public abstract void test();

    protected void testConnector() {
        Assert.assertNotNull(this.connector);
    }
}
