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

import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;

import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueConnector;
import eu.mosaic_cloud.drivers.interop.queue.amqp.AmqpStub;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;

public class AmqpQueueConnectorTest extends
        BaseConnectorTest<AmqpQueueConnector, BaseConnectorTest.Context<AmqpStub>> {
    @Override
    @Before
    public void setUp() {
        this.context = AmqpQueueConnectorTest.context_;
        this.connector = AmqpQueueConnector.create(this.context.configuration,
                this.context.threading);
    }

    @Override
    public void test() {
        this.testConnector();
        this.testDeclareExchange();
        this.testDeclareQueue();
        this.testBindQueue();
    }

    protected void testBindQueue() {
        final String exchange = ConfigUtils.resolveParameter(this.context.configuration,
                "publisher.amqp.exchange", String.class, "");
        final String routingKey = ConfigUtils.resolveParameter(this.context.configuration,
                "publisher.amqp.routing_key", String.class, "");
        final String queue = ConfigUtils.resolveParameter(this.context.configuration,
                "consumer.amqp.queue", String.class, "");
        Assert.assertTrue(this.awaitBooleanOutcome(this.connector.bindQueue(exchange, queue,
                routingKey)));
    }

    protected void testDeclareExchange() {
        final String exchange = ConfigUtils.resolveParameter(this.context.configuration,
                "publisher.amqp.exchange", String.class, "");
        Assert.assertTrue(this.awaitBooleanOutcome(this.connector.declareExchange(exchange,
                AmqpExchangeType.DIRECT, false, false, false)));
    }

    protected void testDeclareQueue() {
        final String queue = ConfigUtils.resolveParameter(this.context.configuration,
                "consumer.amqp.queue", String.class, "");
        Assert.assertTrue(this.awaitBooleanOutcome(this.connector.declareQueue(queue, true, false,
                true, false)));
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        final Context<AmqpStub> context = new Context<AmqpStub>();
        BaseConnectorTest.setupUpContext(AmqpQueueConnectorTest.class, context,
                "amqp-queue-connector-test.prop");
        context.driverChannel.register(AmqpSession.DRIVER);
        context.driverStub = AmqpStub.create(context.configuration, context.driverChannel,
                context.threading);
        AmqpQueueConnectorTest.context_ = context;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        BaseConnectorTest.tearDownContext(AmqpQueueConnectorTest.context_);
    }

    private static Context<AmqpStub> context_;
}
