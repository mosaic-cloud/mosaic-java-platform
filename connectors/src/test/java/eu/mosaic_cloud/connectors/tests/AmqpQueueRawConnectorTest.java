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

package eu.mosaic_cloud.connectors.tests;


import java.util.UUID;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.connectors.implementations.v1.core.ConnectorConfiguration;
import eu.mosaic_cloud.connectors.implementations.v1.queue.amqp.AmqpQueueRawConnector;
import eu.mosaic_cloud.connectors.v1.queue.amqp.IAmqpQueueRawConsumerCallback;
import eu.mosaic_cloud.drivers.queue.amqp.interop.AmqpStub;
import eu.mosaic_cloud.platform.implementations.v1.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.implementations.v1.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.platform.v1.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v1.core.serialization.EncodingException;
import eu.mosaic_cloud.platform.v1.core.serialization.EncodingMetadata;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder.EncodeOutcome;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;


public class AmqpQueueRawConnectorTest
		extends BaseConnectorTest<AmqpQueueRawConnector, BaseScenario>
{
	@Override
	@Before
	public void setUp ()
	{
		this.scenario = AmqpQueueRawConnectorTest.scenario_;
		final ConnectorConfiguration configuration = ConnectorConfiguration.create (this.scenario.getConfiguration (), this.scenario.getEnvironment ());
		this.connector = AmqpQueueRawConnector.create (configuration);
		this.encoder = PlainTextDataEncoder.DEFAULT_INSTANCE;
		this.sentMessage = "ConnectorTest" + this.clientId;
	}
	
	@Override
	public void test ()
			throws InterruptedException,
				ExecutionException,
				EncodingException
	{
		this.testConnector ();
		this.testDeclareExchange ();
		this.testDeclareQueue ();
		this.testBindQueue ();
		this.testConsume ();
		this.testPublish ();
		this.testConsumeCancel ();
	}
	
	public void testConsume ()
			throws InterruptedException,
				ExecutionException
	{
		final IConfiguration configuration = this.scenario.getConfiguration ();
		final String queue = ConfigUtils.resolveParameter (configuration, "consumer.amqp.queue", String.class, "");
		final boolean autoAck = ConfigUtils.resolveParameter (configuration, "consumer.amqp.auto_ack", Boolean.class, true);
		final boolean exclusive = ConfigUtils.resolveParameter (configuration, "consumer.amqp.exclusive", Boolean.class, true);
		final IAmqpQueueRawConsumerCallback consumerCallback = new ConsumerHandler ();
		Assert.assertTrue (this.awaitSuccess (this.connector.consume (queue, this.clientId, exclusive, autoAck, consumerCallback)));
	}
	
	public void testConsumeCancel ()
	{
		Threading.sleep (1000);
		Assert.assertNotNull (this.consumerTag);
		Assert.assertTrue (this.awaitSuccess (this.connector.cancel (this.consumerTag)));
		Threading.sleep (1000);
	}
	
	public void testPublish ()
			throws EncodingException,
				InterruptedException,
				ExecutionException
	{
		final IConfiguration configuration = this.scenario.getConfiguration ();
		final String exchange = ConfigUtils.resolveParameter (configuration, "publisher.amqp.exchange", String.class, "");
		final String routingKey = ConfigUtils.resolveParameter (configuration, "publisher.amqp.routing_key", String.class, "");
		final boolean manadatory = ConfigUtils.resolveParameter (configuration, "publisher.amqp.manadatory", Boolean.class, true);
		final boolean immediate = ConfigUtils.resolveParameter (configuration, "publisher.amqp.immediate", Boolean.class, true);
		final boolean durable = ConfigUtils.resolveParameter (configuration, "publisher.amqp.durable", Boolean.class, false);
		final EncodeOutcome encode = this.encoder.encode (this.sentMessage, null);
		final AmqpOutboundMessage mssg = new AmqpOutboundMessage (exchange, routingKey, encode.data, manadatory, immediate, durable, null, encode.metadata.getContentEncoding (), encode.metadata.getContentType (), null, null);
		Assert.assertTrue (this.awaitSuccess (this.connector.publish (mssg)));
	}
	
	protected void testBindQueue ()
	{
		final IConfiguration configuration = this.scenario.getConfiguration ();
		final String exchange = ConfigUtils.resolveParameter (configuration, "publisher.amqp.exchange", String.class, "");
		final String routingKey = ConfigUtils.resolveParameter (configuration, "publisher.amqp.routing_key", String.class, "");
		final String queue = ConfigUtils.resolveParameter (configuration, "consumer.amqp.queue", String.class, "");
		Assert.assertTrue (this.awaitSuccess (this.connector.bindQueue (exchange, queue, routingKey)));
	}
	
	protected void testDeclareExchange ()
	{
		final String exchange = ConfigUtils.resolveParameter (this.scenario.getConfiguration (), "publisher.amqp.exchange", String.class, "");
		Assert.assertTrue (this.awaitSuccess (this.connector.declareExchange (exchange, AmqpExchangeType.DIRECT, false, false, false)));
	}
	
	protected void testDeclareQueue ()
	{
		final String queue = ConfigUtils.resolveParameter (this.scenario.getConfiguration (), "consumer.amqp.queue", String.class, "");
		Assert.assertTrue (this.awaitSuccess (this.connector.declareQueue (queue, true, false, true, false)));
	}
	
	@BeforeClass
	public static void setUpBeforeClass ()
	{
		final String host = System.getProperty (AmqpQueueRawConnectorTest.MOSAIC_AMQP_HOST, AmqpQueueRawConnectorTest.MOSAIC_AMQP_HOST_DEFAULT);
		final Integer port = Integer.valueOf (System.getProperty (AmqpQueueRawConnectorTest.MOSAIC_AMQP_PORT, AmqpQueueRawConnectorTest.MOSAIC_AMQP_PORT_DEFAULT));
		final IConfiguration configuration = PropertyTypeConfiguration.create ();
		configuration.addParameter ("interop.driver.endpoint", "inproc://f4c74dc5-b548-4ec4-a6a6-ef97c79bf55d");
		configuration.addParameter ("interop.driver.identity", "f4c74dc5-b548-4ec4-a6a6-ef97c79bf55d");
		configuration.addParameter ("amqp.host", host);
		configuration.addParameter ("amqp.port", port);
		configuration.addParameter ("amqp.driver_threads", 1);
		configuration.addParameter ("consumer.amqp.queue", "tests.queue");
		configuration.addParameter ("consumer.amqp.consumer_id", "tests.consumer");
		configuration.addParameter ("consumer.amqp.auto_ack", true);
		configuration.addParameter ("consumer.amqp.exclusive", true);
		configuration.addParameter ("publisher.amqp.exchange", "tests.exchange");
		configuration.addParameter ("publisher.amqp.routing_key", "tests.queue");
		configuration.addParameter ("publisher.amqp.manadatory", true);
		configuration.addParameter ("publisher.amqp.immediate", true);
		configuration.addParameter ("publisher.amqp.durable", false);
		final BaseScenario scenario = new BaseScenario (AmqpQueueRawConnectorTest.class, configuration);
		scenario.registerDriverRole (AmqpSession.DRIVER);
		BaseConnectorTest.driverStub = AmqpStub.createDetached (configuration, scenario.getDriverChannel (), scenario.getThreading ());
		AmqpQueueRawConnectorTest.scenario_ = scenario;
	}
	
	@AfterClass
	public static void tearDownAfterClass ()
	{
		BaseConnectorTest.tearDownScenario (AmqpQueueRawConnectorTest.scenario_);
	}
	
	private final String clientId = UUID.randomUUID ().toString ();
	private String consumerTag;
	private DataEncoder<String> encoder;
	private String sentMessage;
	private static final String MOSAIC_AMQP_HOST = "mosaic.tests.resources.amqp.host";
	private static final String MOSAIC_AMQP_HOST_DEFAULT = "127.0.0.1";
	private static final String MOSAIC_AMQP_PORT = "mosaic.tests.resources.amqp.port";
	private static final String MOSAIC_AMQP_PORT_DEFAULT = "21688";
	private static BaseScenario scenario_;
	
	final class ConsumerHandler
			implements
				IAmqpQueueRawConsumerCallback
	{
		@Override
		public CallbackCompletion<Void> handleCancelOk (final String consumerTag)
		{
			Assert.assertTrue ("CancelOk - consumer tag compare", consumerTag.equals (AmqpQueueRawConnectorTest.this.consumerTag));
			return CallbackCompletion.createOutcome ();
		}
		
		@Override
		public CallbackCompletion<Void> handleConsumeOk (final String consumerTag)
		{
			Assert.assertTrue ("ConsumeOk - consumer tag compare", AmqpQueueRawConnectorTest.this.consumerTag == null);
			AmqpQueueRawConnectorTest.this.consumerTag = consumerTag;
			return CallbackCompletion.createOutcome ();
		}
		
		@Override
		public CallbackCompletion<Void> handleDelivery (final AmqpInboundMessage message)
		{
			String recvMessage;
			final EncodingMetadata encoding = new EncodingMetadata (message.getContentType (), message.getContentEncoding ());
			try {
				recvMessage = AmqpQueueRawConnectorTest.this.encoder.decode (message.getData (), encoding);
				Assert.assertTrue ("Received message: " + recvMessage, AmqpQueueRawConnectorTest.this.sentMessage.equals (recvMessage));
			} catch (final EncodingException e) {
				Assert.fail ("Delivery exception " + e.getMessage ());
			}
			return CallbackCompletion.createOutcome ();
		}
		
		@Override
		public CallbackCompletion<Void> handleShutdownSignal (final String consumerTag, final String message)
		{
			Assert.assertTrue ("Shutdown - consumer tag compare", consumerTag.equals (AmqpQueueRawConnectorTest.this.consumerTag));
			return CallbackCompletion.createOutcome ();
		}
	}
}
