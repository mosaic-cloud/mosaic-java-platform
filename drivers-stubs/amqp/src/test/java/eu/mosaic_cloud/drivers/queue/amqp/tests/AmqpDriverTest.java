/*
 * #%L
 * mosaic-drivers-stubs-amqp
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

package eu.mosaic_cloud.drivers.queue.amqp.tests;


import java.util.UUID;
import java.util.concurrent.ExecutionException;

import eu.mosaic_cloud.drivers.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.drivers.ops.IResult;
import eu.mosaic_cloud.drivers.ops.tests.TestLoggingHandler;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpDriver;
import eu.mosaic_cloud.drivers.queue.amqp.IAmqpConsumer;
import eu.mosaic_cloud.platform.implementations.v1.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.implementations.v1.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpInboundMessage;
import eu.mosaic_cloud.platform.interop.common.amqp.AmqpOutboundMessage;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder.EncodeOutcome;
import eu.mosaic_cloud.platform.v1.core.serialization.EncodingException;
import eu.mosaic_cloud.platform.v1.core.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class AmqpDriverTest
{
	@Before
	public void setUp () {
		final Transcript transcript = Transcript.create (this);
		final QueueingExceptionTracer exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		final TranscriptExceptionTracer exceptions = TranscriptExceptionTracer.create (transcript, exceptionsQueue);
		BasicThreadingSecurityManager.initialize ();
		this.threadingContext = BasicThreadingContext.create (this, exceptions, exceptions.catcher);
		this.threadingContext.initialize ();
		this.wrapper = AmqpDriver.create (AmqpDriverTest.configuration, this.threadingContext);
		this.encoder = PlainTextDataEncoder.DEFAULT_INSTANCE;
		this.sentMessage = "DriverTest" + this.clientId;
	}
	
	@After
	public void tearDown () {
		this.wrapper.destroy ();
		this.threadingContext.destroy ();
	}
	
	@Test
	public void testAll ()
				throws InterruptedException, ExecutionException, EncodingException {
		this.testDriver ();
		this.testDeclareExchange ();
		this.testDeclareQueue ();
		this.testBindQueue ();
		this.testConsume ();
		this.testPublish ();
		this.testConsumeCancel ();
	}
	
	public void testBindQueue ()
				throws InterruptedException, ExecutionException {
		final String exchange = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.exchange", String.class, "");
		final String routingKey = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.routing_key", String.class, "");
		final String queue = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "consumer.amqp.queue", String.class, "");
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("bind queue");
		final IResult<Boolean> r = this.wrapper.bindQueue (this.clientId, exchange, queue, routingKey, handler);
		Assert.assertTrue (r.getResult ());
	}
	
	public void testConsume ()
				throws InterruptedException, ExecutionException {
		final String queue = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "consumer.amqp.queue", String.class, "");
		final boolean autoAck = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "consumer.amqp.auto_ack", Boolean.class, true);
		final boolean exclusive = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "consumer.amqp.exclusive", Boolean.class, true);
		final IOperationCompletionHandler<String> handler = new TestLoggingHandler<String> ("consume");
		final IAmqpConsumer consumeCallback = new ConsumerHandler ();
		final IResult<String> r = this.wrapper.basicConsume (queue, this.clientId, exclusive, autoAck, consumeCallback, handler);
		Assert.assertTrue ("Register consumer", this.clientId.equals (r.getResult ()));
	}
	
	public void testConsumeCancel () {
		Threading.sleep (1000);
		Assert.assertNotNull (this.consumerTag);
		this.wrapper.basicCancel (this.consumerTag, null);
		Threading.sleep (1000);
	}
	
	public void testDeclareExchange ()
				throws InterruptedException, ExecutionException {
		final String exchange = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.exchange", String.class, "");
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("declare exchange");
		final IResult<Boolean> r = this.wrapper.declareExchange (this.clientId, exchange, AmqpExchangeType.DIRECT, false, true, false, handler);
		Assert.assertTrue (r.getResult ());
	}
	
	public void testDeclareQueue ()
				throws InterruptedException, ExecutionException {
		final String queue = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "consumer.amqp.queue", String.class, "");
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("declare queue");
		final IResult<Boolean> r = this.wrapper.declareQueue (this.clientId, queue, true, false, true, false, handler);
		Assert.assertTrue (r.getResult ());
	}
	
	public void testDriver ()
				throws InterruptedException, ExecutionException {
		Assert.assertNotNull (this.wrapper);
	}
	
	public void testPublish ()
				throws EncodingException, InterruptedException, ExecutionException {
		final String exchange = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.exchange", String.class, "");
		final String routingKey = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.routing_key", String.class, "");
		final boolean manadatory = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.manadatory", Boolean.class, true);
		final boolean immediate = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.immediate", Boolean.class, true);
		final boolean durable = ConfigUtils.resolveParameter (AmqpDriverTest.configuration, "publisher.amqp.durable", Boolean.class, false);
		final EncodeOutcome encode = this.encoder.encode (this.sentMessage, null);
		final AmqpOutboundMessage mssg = new AmqpOutboundMessage (exchange, routingKey, encode.data, manadatory, immediate, durable, null, encode.metadata.getContentEncoding (), encode.metadata.getContentType (), null, null);
		final IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean> ("publish message");
		final IResult<Boolean> r = this.wrapper.basicPublish (this.clientId, mssg, handler);
		Assert.assertTrue (r.getResult ());
	}
	
	private final String clientId = UUID.randomUUID ().toString ();
	private String consumerTag;
	private DataEncoder<String> encoder;
	private String sentMessage;
	private BasicThreadingContext threadingContext;
	private AmqpDriver wrapper;
	
	@BeforeClass
	public static void setUpBeforeClass () {
		final String host = System.getProperty (AmqpDriverTest.MOSAIC_AMQP_HOST, AmqpDriverTest.MOSAIC_AMQP_HOST_DEFAULT);
		final Integer port = Integer.valueOf (System.getProperty (AmqpDriverTest.MOSAIC_AMQP_PORT, AmqpDriverTest.MOSAIC_AMQP_PORT_DEFAULT));
		AmqpDriverTest.configuration = PropertyTypeConfiguration.create ();
		AmqpDriverTest.configuration.addParameter ("amqp.host", host);
		AmqpDriverTest.configuration.addParameter ("amqp.port", port);
		AmqpDriverTest.configuration.addParameter ("amqp.driver_threads", 1);
		AmqpDriverTest.configuration.addParameter ("consumer.amqp.queue", "tests.queue");
		AmqpDriverTest.configuration.addParameter ("consumer.amqp.auto_ack", true);
		AmqpDriverTest.configuration.addParameter ("consumer.amqp.exclusive", true);
		AmqpDriverTest.configuration.addParameter ("publisher.amqp.exchange", "tests.exchange");
		AmqpDriverTest.configuration.addParameter ("publisher.amqp.routing_key", "tests.queue");
		AmqpDriverTest.configuration.addParameter ("publisher.amqp.manadatory", true);
		AmqpDriverTest.configuration.addParameter ("publisher.amqp.immediate", true);
		AmqpDriverTest.configuration.addParameter ("publisher.amqp.durable", false);
	}
	
	private static Configuration configuration;
	private static final String MOSAIC_AMQP_HOST = "mosaic.tests.resources.amqp.host";
	private static final String MOSAIC_AMQP_HOST_DEFAULT = "127.0.0.1";
	private static final String MOSAIC_AMQP_PORT = "mosaic.tests.resources.amqp.port";
	private static final String MOSAIC_AMQP_PORT_DEFAULT = "21688";
	
	final class ConsumerHandler
				implements
					IAmqpConsumer
	{
		@Override
		public void handleCancel (final String consumerTag) {
			Assert.assertTrue ("Cancel - consumer tag compare", consumerTag.equals (AmqpDriverTest.this.consumerTag));
		}
		
		@Override
		public void handleCancelOk (final String consumerTag) {
			Assert.assertTrue ("CancelOk - consumer tag compare", consumerTag.equals (AmqpDriverTest.this.consumerTag));
		}
		
		@Override
		public void handleConsumeOk (final String consumerTag) {
			Assert.assertTrue ("ConsumeOk - consumer tag compare", AmqpDriverTest.this.consumerTag == null);
			AmqpDriverTest.this.consumerTag = consumerTag;
		}
		
		@Override
		public void handleDelivery (final AmqpInboundMessage message) {
			String recvMessage;
			final EncodingMetadata encoding = new EncodingMetadata (message.getContentType (), message.getContentEncoding ());
			try {
				recvMessage = AmqpDriverTest.this.encoder.decode (message.getData (), encoding);
				Assert.assertTrue ("Received message: " + recvMessage, AmqpDriverTest.this.sentMessage.equals (recvMessage));
			} catch (final EncodingException e) {
				Assert.fail ("Delivery exception " + e.getMessage ());
			}
		}
		
		@Override
		public void handleShutdown (final String consumerTag, final String signalMessage) {
			Assert.assertTrue ("Shutdown - consumer tag compare", consumerTag.equals (AmqpDriverTest.this.consumerTag));
		}
	}
}
