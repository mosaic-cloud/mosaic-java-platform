/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
package eu.mosaic_cloud.connector.amqp.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.mosaic_cloud.connector.queue.amqp.AmqpConnector;
import eu.mosaic_cloud.core.Serial;
import eu.mosaic_cloud.core.SerialJunitRunner;
import eu.mosaic_cloud.core.TestLoggingHandler;
import eu.mosaic_cloud.core.configuration.ConfigUtils;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.ops.IResult;
import eu.mosaic_cloud.driver.interop.queue.amqp.AmqpStub;
import eu.mosaic_cloud.driver.queue.amqp.AmqpExchangeType;
import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.interop.amqp.AmqpSession;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

@RunWith(SerialJunitRunner.class)
@Serial
public class AmqpConnectorTest {
	private static IConfiguration configuration;
	private static AmqpConnector connector;
	private static AmqpStub driverStub;

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		AmqpConnectorTest.configuration = PropertyTypeConfiguration.create(
				AmqpConnectorTest.class.getClassLoader(), "amqp-test.prop");

		ZeroMqChannel driverChannel = new ZeroMqChannel(
				ConfigUtils.resolveParameter(AmqpConnectorTest.configuration,
						"interop.driver.identifier", String.class, ""),
				AbortingExceptionTracer.defaultInstance);
		driverChannel.register(AmqpSession.DRIVER);
		driverChannel.accept(ConfigUtils.resolveParameter(
				AmqpConnectorTest.configuration, "interop.channel.address",
				String.class, ""));

		AmqpConnectorTest.driverStub = AmqpStub.create(
				AmqpConnectorTest.configuration, driverChannel);
		AmqpConnectorTest.connector = AmqpConnector
				.create(AmqpConnectorTest.configuration);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		AmqpConnectorTest.connector.destroy();
		AmqpConnectorTest.driverStub.destroy();
	}

	public void testConnector() throws InterruptedException, ExecutionException {
		Assert.assertNotNull(AmqpConnectorTest.connector);
	}

	private List<IOperationCompletionHandler<Boolean>> getHandlers(
			String testName) {
		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				testName);
		List<IOperationCompletionHandler<Boolean>> list = new ArrayList<IOperationCompletionHandler<Boolean>>();
		list.add(handler);
		return list;
	}

	public void testDeclareExchange() throws InterruptedException,
			ExecutionException {
		String exchange = ConfigUtils.resolveParameter(
				AmqpConnectorTest.configuration, "publisher.amqp.exchange",
				String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("declare exchange");

		IResult<Boolean> r = AmqpConnectorTest.connector.declareExchange(
				exchange, AmqpExchangeType.DIRECT, false, false, false,
				handlers, null);
		Assert.assertTrue(r.getResult());
	}

	public void testDeclareQueue() throws InterruptedException,
			ExecutionException {
		String queue = ConfigUtils.resolveParameter(
				AmqpConnectorTest.configuration, "consumer.amqp.queue",
				String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("declare queue");
		IResult<Boolean> r = AmqpConnectorTest.connector.declareQueue(queue,
				true, false, true, false, handlers, null);
		Assert.assertTrue(r.getResult());
	}

	public void testBindQueue() throws InterruptedException, ExecutionException {
		String exchange = ConfigUtils.resolveParameter(
				AmqpConnectorTest.configuration, "publisher.amqp.exchange",
				String.class, "");
		String routingKey = ConfigUtils.resolveParameter(
				AmqpConnectorTest.configuration, "publisher.amqp.routing_key",
				String.class, "");
		String queue = ConfigUtils.resolveParameter(
				AmqpConnectorTest.configuration, "consumer.amqp.queue",
				String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("bind queue");
		IResult<Boolean> r = AmqpConnectorTest.connector.bindQueue(exchange,
				queue, routingKey, handlers, null);

		Assert.assertTrue(r.getResult());
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {
		testConnector();
		testDeclareExchange();
		testDeclareQueue();
		testBindQueue();
	}

}
