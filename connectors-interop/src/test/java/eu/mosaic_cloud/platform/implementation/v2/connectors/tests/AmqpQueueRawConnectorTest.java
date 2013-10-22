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

package eu.mosaic_cloud.platform.implementation.v2.connectors.tests;


import java.util.UUID;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.drivers.queue.amqp.interop.AmqpStub;
import eu.mosaic_cloud.platform.implementation.v2.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.implementation.v2.connectors.interop.queue.amqp.AmqpQueueRawConnector;
import eu.mosaic_cloud.platform.implementation.v2.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.interop.specs.amqp.AmqpSession;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.queue.amqp.AmqpQueueRawConsumerCallback;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder.EncodeOutcome;
import eu.mosaic_cloud.platform.v2.serialization.EncodingException;
import eu.mosaic_cloud.platform.v2.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.implementations.basic.PropertiesBackedConfigurationSource;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;


@SuppressWarnings ("boxing")
public class AmqpQueueRawConnectorTest
			extends BaseConnectorTest<AmqpQueueRawConnector, BaseScenario>
{
	@Override
	@Before
	public void setUp () {
		this.scenario = AmqpQueueRawConnectorTest.scenario_;
		final ConnectorConfiguration configuration = ConnectorConfiguration.create (this.scenario.getConfiguration (), this.scenario.getEnvironment ());
		this.connector = AmqpQueueRawConnector.create (configuration);
		this.encoder = PlainTextDataEncoder.DEFAULT_INSTANCE;
		this.sentMessage = "ConnectorTest" + this.clientId;
	}
	
	@Override
	public void test ()
				throws InterruptedException, ExecutionException, EncodingException {
		this.testConnector ();
		this.testDeclareExchange ();
		this.testDeclareQueue ();
		this.testBindQueue ();
		this.testConsume ();
		this.testPublish ();
		this.testConsumeCancel ();
	}
	
	public void testConsume () {
		final ConfigurationSource configuration = this.scenario.getConfiguration ();
		final String queue = ConfigUtils.resolveParameter (configuration, "consumer.amqp.queue", String.class, "");
		final boolean autoAck = ConfigUtils.resolveParameter (configuration, "consumer.amqp.auto_ack", Boolean.class, true);
		final boolean exclusive = ConfigUtils.resolveParameter (configuration, "consumer.amqp.exclusive", Boolean.class, true);
		final AmqpQueueRawConsumerCallback consumerCallback = new ConsumerHandler ();
		Assert.assertTrue (this.awaitSuccess (this.connector.consume (queue, this.clientId, exclusive, autoAck, consumerCallback)));
	}
	
	public void testConsumeCancel () {
		Threading.sleep (1000);
		Assert.assertNotNull (this.consumerTag);
		Assert.assertTrue (this.awaitSuccess (this.connector.cancel (this.consumerTag)));
		Threading.sleep (1000);
	}
	
	public void testPublish ()
				throws EncodingException {
		final ConfigurationSource configuration = this.scenario.getConfiguration ();
		final String exchange = ConfigUtils.resolveParameter (configuration, "publisher.amqp.exchange", String.class, "");
		final String routingKey = ConfigUtils.resolveParameter (configuration, "publisher.amqp.routing_key", String.class, "");
		final boolean manadatory = ConfigUtils.resolveParameter (configuration, "publisher.amqp.manadatory", Boolean.class, true);
		final boolean immediate = ConfigUtils.resolveParameter (configuration, "publisher.amqp.immediate", Boolean.class, true);
		final boolean durable = ConfigUtils.resolveParameter (configuration, "publisher.amqp.durable", Boolean.class, false);
		final EncodeOutcome encode = this.encoder.encode (this.sentMessage, null);
		final AmqpOutboundMessage mssg = new AmqpOutboundMessage (exchange, routingKey, encode.data, manadatory, immediate, durable, null, encode.metadata.getContentEncoding (), encode.metadata.getContentType (), null, null);
		Assert.assertTrue (this.awaitSuccess (this.connector.publish (mssg)));
	}
	
	protected void testBindQueue () {
		final ConfigurationSource configuration = this.scenario.getConfiguration ();
		final String exchange = ConfigUtils.resolveParameter (configuration, "publisher.amqp.exchange", String.class, "");
		final String routingKey = ConfigUtils.resolveParameter (configuration, "publisher.amqp.routing_key", String.class, "");
		final String queue = ConfigUtils.resolveParameter (configuration, "consumer.amqp.queue", String.class, "");
		Assert.assertTrue (this.awaitSuccess (this.connector.bindQueue (exchange, queue, routingKey)));
	}
	
	protected void testDeclareExchange () {
		final String exchange = ConfigUtils.resolveParameter (this.scenario.getConfiguration (), "publisher.amqp.exchange", String.class, "");
		Assert.assertTrue (this.awaitSuccess (this.connector.declareExchange (exchange, AmqpExchangeType.DIRECT, false, false, false)));
	}
	
	protected void testDeclareQueue () {
		final String queue = ConfigUtils.resolveParameter (this.scenario.getConfiguration (), "consumer.amqp.queue", String.class, "");
		Assert.assertTrue (this.awaitSuccess (this.connector.declareQueue (queue, true, false, true, false)));
	}
	
	private final String clientId = UUID.randomUUID ().toString ();
	private String consumerTag;
	private DataEncoder<String> encoder;
	private String sentMessage;
	
	@BeforeClass
	public static void setUpBeforeClass () {
		final String host = System.getProperty (AmqpQueueRawConnectorTest.MOSAIC_AMQP_HOST, AmqpQueueRawConnectorTest.MOSAIC_AMQP_HOST_DEFAULT);
		final Integer port = Integer.valueOf (System.getProperty (AmqpQueueRawConnectorTest.MOSAIC_AMQP_PORT, AmqpQueueRawConnectorTest.MOSAIC_AMQP_PORT_DEFAULT));
		final PropertiesBackedConfigurationSource configuration = PropertiesBackedConfigurationSource.create ();
		configuration.overridePropertyValue ("interop.driver.endpoint", "inproc://f4c74dc5-b548-4ec4-a6a6-ef97c79bf55d");
		configuration.overridePropertyValue ("interop.driver.identity", "f4c74dc5-b548-4ec4-a6a6-ef97c79bf55d");
		configuration.overridePropertyValue ("amqp.host", host);
		configuration.overridePropertyValue ("amqp.port", port.toString ());
		configuration.overridePropertyValue ("amqp.driver_threads", Integer.toString (1));
		configuration.overridePropertyValue ("consumer.amqp.queue", "tests.queue");
		configuration.overridePropertyValue ("consumer.amqp.consumer_id", "tests.consumer");
		configuration.overridePropertyValue ("consumer.amqp.auto_ack", Boolean.toString (true));
		configuration.overridePropertyValue ("consumer.amqp.exclusive", Boolean.toString (true));
		configuration.overridePropertyValue ("publisher.amqp.exchange", "tests.exchange");
		configuration.overridePropertyValue ("publisher.amqp.routing_key", "tests.queue");
		configuration.overridePropertyValue ("publisher.amqp.manadatory", Boolean.toString (true));
		configuration.overridePropertyValue ("publisher.amqp.immediate", Boolean.toString (true));
		configuration.overridePropertyValue ("publisher.amqp.durable", Boolean.toString (false));
		final BaseScenario scenario = new BaseScenario (AmqpQueueRawConnectorTest.class, configuration);
		scenario.registerDriverRole (AmqpSession.DRIVER);
		BaseConnectorTest.driverStub = AmqpStub.createDetached (configuration, scenario.getDriverChannel (), scenario.getThreading ());
		AmqpQueueRawConnectorTest.scenario_ = scenario;
	}
	
	@AfterClass
	public static void tearDownAfterClass () {
		BaseConnectorTest.tearDownScenario (AmqpQueueRawConnectorTest.scenario_);
	}
	
	private static final String MOSAIC_AMQP_HOST = "mosaic.tests.resources.amqp.host";
	private static final String MOSAIC_AMQP_HOST_DEFAULT = "127.0.0.1";
	private static final String MOSAIC_AMQP_PORT = "mosaic.tests.resources.amqp.port";
	private static final String MOSAIC_AMQP_PORT_DEFAULT = "21688";
	private static BaseScenario scenario_;
	
	final class ConsumerHandler
				implements
					AmqpQueueRawConsumerCallback
	{
		@Override
		public CallbackCompletion<Void> handleCancelOk (final String consumerTag) {
			Assert.assertTrue ("CancelOk - consumer tag compare", consumerTag.equals (AmqpQueueRawConnectorTest.this.consumerTag));
			return CallbackCompletion.createOutcome ();
		}
		
		@Override
		public CallbackCompletion<Void> handleConsumeOk (final String consumerTag) {
			Assert.assertTrue ("ConsumeOk - consumer tag compare", AmqpQueueRawConnectorTest.this.consumerTag == null);
			AmqpQueueRawConnectorTest.this.consumerTag = consumerTag;
			return CallbackCompletion.createOutcome ();
		}
		
		@Override
		public CallbackCompletion<Void> handleDelivery (final AmqpInboundMessage message) {
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
		public CallbackCompletion<Void> handleShutdownSignal (final String consumerTag, final String message) {
			Assert.assertTrue ("Shutdown - consumer tag compare", consumerTag.equals (AmqpQueueRawConnectorTest.this.consumerTag));
			return CallbackCompletion.createOutcome ();
		}
	}
}
