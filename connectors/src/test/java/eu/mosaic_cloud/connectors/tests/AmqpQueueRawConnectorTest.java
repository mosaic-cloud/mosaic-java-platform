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

import eu.mosaic_cloud.connectors.queue.amqp.AmqpQueueRawConnector;
import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.drivers.queue.amqp.interop.AmqpStub;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

public class AmqpQueueRawConnectorTest extends
        BaseConnectorTest<AmqpQueueRawConnector, BaseScenario> {

    private static final String MOSAIC_AMQP_HOST = "mosaic.tests.resources.amqp.host";
    private static final String MOSAIC_AMQP_HOST_DEFAULT = "127.0.0.1";
    private static final String MOSAIC_AMQP_PORT = "mosaic.tests.resources.amqp.port";
    private static final String MOSAIC_AMQP_PORT_DEFAULT = "21688";
    private static BaseScenario scenario_;

    @BeforeClass
    public static void setUpBeforeClass() {

        final String host = System.getProperty(
                AmqpQueueRawConnectorTest.MOSAIC_AMQP_HOST,
                AmqpQueueRawConnectorTest.MOSAIC_AMQP_HOST_DEFAULT);
        final Integer port = Integer.valueOf(System.getProperty(
                AmqpQueueRawConnectorTest.MOSAIC_AMQP_PORT,
                AmqpQueueRawConnectorTest.MOSAIC_AMQP_PORT_DEFAULT));

        final IConfiguration configuration = PropertyTypeConfiguration.create();
        configuration.addParameter("interop.channel.address",
                "inproc://f4c74dc5-b548-4ec4-a6a6-ef97c79bf55d");
        configuration.addParameter("interop.driver.identifier",
                "f4c74dc5-b548-4ec4-a6a6-ef97c79bf55d");
        configuration.addParameter("amqp.host", host);
        configuration.addParameter("amqp.port", port);
        configuration.addParameter("amqp.driver_threads", 1);
        configuration.addParameter("consumer.amqp.queue", "tests.queue");
        configuration.addParameter("consumer.amqp.consumer_id",
                "tests.consumer");
        configuration.addParameter("consumer.amqp.auto_ack", true);
        configuration.addParameter("consumer.amqp.exclusive", true);
        configuration.addParameter("publisher.amqp.exchange", "tests.exchange");
        configuration.addParameter("publisher.amqp.routing_key",
                "tests.routing-key");
        configuration.addParameter("publisher.amqp.manadatory", true);
        configuration.addParameter("publisher.amqp.immediate", true);
        configuration.addParameter("publisher.amqp.durable", false);

        final BaseScenario scenario = new BaseScenario(
                AmqpQueueRawConnectorTest.class, configuration);

        scenario.registerDriverRole(AmqpSession.DRIVER);
        BaseConnectorTest.driverStub = AmqpStub.createDetached(configuration,
                scenario.getDriverChannel(), scenario.getThreading());

        AmqpQueueRawConnectorTest.scenario_ = scenario;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        BaseConnectorTest.tearDownScenario(AmqpQueueRawConnectorTest.scenario_);
    }

    @Override
    @Before
    public void setUp() {
        this.scenario = AmqpQueueRawConnectorTest.scenario_;
        final ConnectorConfiguration configuration = ConnectorConfiguration
                .create(this.scenario.getConfiguration(),
                        this.scenario.getEnvironment());
        this.connector = AmqpQueueRawConnector.create(configuration);
    }

    @Override
    public void test() {
        this.testConnector();
        this.testDeclareExchange();
        this.testDeclareQueue();
        this.testBindQueue();
    }

    protected void testBindQueue() {
        final IConfiguration configuration = this.scenario.getConfiguration();
        final String exchange = ConfigUtils.resolveParameter(configuration,
                "publisher.amqp.exchange", String.class, "");
        final String routingKey = ConfigUtils.resolveParameter(configuration,
                "publisher.amqp.routing_key", String.class, "");
        final String queue = ConfigUtils.resolveParameter(configuration,
                "consumer.amqp.queue", String.class, "");
        Assert.assertTrue(this.awaitSuccess(this.connector.bindQueue(exchange,
                queue, routingKey)));
    }

    protected void testDeclareExchange() {
        final String exchange = ConfigUtils.resolveParameter(
                this.scenario.getConfiguration(), "publisher.amqp.exchange",
                String.class, "");
        Assert.assertTrue(this.awaitSuccess(this.connector.declareExchange(
                exchange, AmqpExchangeType.DIRECT, false, false, false)));
    }

    protected void testDeclareQueue() {
        final String queue = ConfigUtils.resolveParameter(
                this.scenario.getConfiguration(), "consumer.amqp.queue",
                String.class, "");
        Assert.assertTrue(this.awaitSuccess(this.connector.declareQueue(queue,
                true, false, true, false)));
    }
}
