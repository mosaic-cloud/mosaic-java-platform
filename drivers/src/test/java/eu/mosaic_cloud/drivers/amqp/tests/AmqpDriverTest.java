/*
 * #%L
 * mosaic-drivers
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

package eu.mosaic_cloud.drivers.amqp.tests;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.drivers.queue.amqp.AmqpDriver;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.tests.TestLoggingHandler;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AmqpDriverTest {

    private static final String MOSAIC_AMQP_HOST = "mosaic.tests.resources.amqp.host";
    private static final String MOSAIC_AMQP_HOST_DEFAULT = "127.0.0.1";
    private static final String MOSAIC_AMQP_PORT = "mosaic.tests.resources.amqp.port";
    private static final String MOSAIC_AMQP_PORT_DEFAULT = "21688";
    private static IConfiguration configuration;
    private AmqpDriver wrapper;

    private final String clientId = UUID.randomUUID().toString();
    private BasicThreadingContext threadingContext;

    @BeforeClass
    public static void setUpBeforeClass() {

        final String host = System.getProperty(AmqpDriverTest.MOSAIC_AMQP_HOST,
                AmqpDriverTest.MOSAIC_AMQP_HOST_DEFAULT);
        final Integer port = Integer.valueOf(System.getProperty(
                AmqpDriverTest.MOSAIC_AMQP_PORT,
                AmqpDriverTest.MOSAIC_AMQP_PORT_DEFAULT));

        AmqpDriverTest.configuration = PropertyTypeConfiguration.create();
        AmqpDriverTest.configuration.addParameter("amqp.host", host);
        AmqpDriverTest.configuration.addParameter("amqp.port", port);
        AmqpDriverTest.configuration.addParameter("amqp.driver_threads", 1);
        AmqpDriverTest.configuration.addParameter("consumer.amqp.queue",
                "tests.queue");
        AmqpDriverTest.configuration.addParameter("consumer.amqp.consumer_id",
                "tests.consumer");
        AmqpDriverTest.configuration.addParameter("consumer.amqp.auto_ack",
                true);
        AmqpDriverTest.configuration.addParameter("consumer.amqp.exclusive",
                true);
        AmqpDriverTest.configuration.addParameter("publisher.amqp.exchange",
                "tests.exchange");
        AmqpDriverTest.configuration.addParameter("publisher.amqp.routing_key",
                "tests.routing-key");
        AmqpDriverTest.configuration.addParameter("publisher.amqp.manadatory",
                true);
        AmqpDriverTest.configuration.addParameter("publisher.amqp.immediate",
                true);
        AmqpDriverTest.configuration.addParameter("publisher.amqp.durable",
                false);
    }

    @Before
    public void setUp() {
        final Transcript transcript = Transcript.create(this);
        final QueueingExceptionTracer exceptionsQueue = QueueingExceptionTracer
                .create(NullExceptionTracer.defaultInstance);
        final TranscriptExceptionTracer exceptions = TranscriptExceptionTracer
                .create(transcript, exceptionsQueue);
        BasicThreadingSecurityManager.initialize();
        this.threadingContext = BasicThreadingContext.create(this, exceptions,
                exceptions.catcher);
        this.threadingContext.initialize();
        this.wrapper = AmqpDriver.create(AmqpDriverTest.configuration,
                this.threadingContext);
    }

    @After
    public void tearDown() {
        this.wrapper.destroy();
        this.threadingContext.destroy();
    }

    @Test
    public void testAll() throws InterruptedException, ExecutionException {
        testDriver();
        testDeclareExchange();
        testDeclareQueue();
        testBindQueue();
    }

    public void testBindQueue() throws InterruptedException, ExecutionException {
        final String exchange = ConfigUtils.resolveParameter(
                AmqpDriverTest.configuration, "publisher.amqp.exchange",
                String.class, "");
        final String routingKey = ConfigUtils.resolveParameter(
                AmqpDriverTest.configuration, "publisher.amqp.routing_key",
                String.class, "");
        final String queue = ConfigUtils.resolveParameter(
                AmqpDriverTest.configuration, "consumer.amqp.queue",
                String.class, "");
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
                "bind queue");
        final IResult<Boolean> r = this.wrapper.bindQueue(this.clientId,
                exchange, queue, routingKey, handler);
        Assert.assertTrue(r.getResult());
    }

    public void testDeclareExchange() throws InterruptedException,
            ExecutionException {
        final String exchange = ConfigUtils.resolveParameter(
                AmqpDriverTest.configuration, "publisher.amqp.exchange",
                String.class, "");
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
                "declare exchange");
        final IResult<Boolean> r = this.wrapper
                .declareExchange(this.clientId, exchange,
                        AmqpExchangeType.DIRECT, false, false, false, handler);
        Assert.assertTrue(r.getResult());
    }

    public void testDeclareQueue() throws InterruptedException,
            ExecutionException {
        final String queue = ConfigUtils.resolveParameter(
                AmqpDriverTest.configuration, "consumer.amqp.queue",
                String.class, "");
        final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
                "declare queue");
        final IResult<Boolean> r = this.wrapper.declareQueue(this.clientId,
                queue, true, false, true, false, handler);
        Assert.assertTrue(r.getResult());
    }

    public void testDriver() throws InterruptedException, ExecutionException {
        Assert.assertNotNull(this.wrapper);
    }
}
