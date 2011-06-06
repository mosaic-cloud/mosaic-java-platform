package mosaic.driver.amqp.tests;

import java.util.concurrent.ExecutionException;

import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.queue.AmqpDriver;
import mosaic.driver.queue.AmqpExchangeType;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AmqpDriverTest {
	private static IConfiguration configuration;
	private static AmqpDriver wrapper;
	private static IOperationCompletionHandler<Boolean> handler;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		configuration = PropertyTypeConfiguration.create(
				AmqpDriverTest.class.getClassLoader(), "amqp-test.prop");
		wrapper = AmqpDriver.create(configuration);
		handler = new mosaic.core.TestLoggingHandler<Boolean>();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		wrapper.destroy();
	}

	@Test
	public void testOpenConnection() throws InterruptedException,
			ExecutionException {
		Assert.assertNotNull(wrapper);
		IResult<Boolean> r = wrapper.openConnection(handler);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testDeclareExchange() throws InterruptedException,
			ExecutionException {
		String exchange = configuration
				.getParameter(
						ConfigurationIdentifier
								.resolveRelative("publisher_1/amqp.publisher.exchange"),
						String.class).getValue("");

		IResult<Boolean> r = wrapper.declareExchange(exchange,
				AmqpExchangeType.DIRECT, false, false, false, handler);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testDeclareQueue() throws InterruptedException,
			ExecutionException {
		String queue1 = configuration
				.getParameter(
						ConfigurationIdentifier
								.resolveRelative("consumer_1/amqp.consumer.queue"),
						String.class).getValue("");
		IResult<Boolean> r = wrapper.declareQueue(queue1, true, false, true,
				false, handler);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testBindQueue() throws InterruptedException, ExecutionException {
		String exchange = configuration
				.getParameter(
						ConfigurationIdentifier
								.resolveRelative("publisher_1/amqp.publisher.exchange"),
						String.class).getValue("");
		String routingKey1 = configuration
				.getParameter(
						ConfigurationIdentifier
								.resolveRelative("publisher_1/amqp.publisher.exchange"),
						String.class).getValue("");
		String queue1 = configuration
				.getParameter(
						ConfigurationIdentifier
								.resolveRelative("consumer_1/amqp.consumer.queue"),
						String.class).getValue("");
		IResult<Boolean> r = wrapper.bindQueue(exchange, queue1, routingKey1,
				handler);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testCloseConnection() throws InterruptedException,
			ExecutionException {
		IResult<Boolean> r = wrapper.closeConnection(handler);
		Assert.assertTrue(r.getResult());
	}
}
