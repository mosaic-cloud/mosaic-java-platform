package mosaic.connector.amqp.tests;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import mosaic.connector.queue.AmqpConnector;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.interop.AmqpStub;
import mosaic.driver.queue.AmqpExchangeType;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AmqpConnectorTest {
	private static IConfiguration configuration;
	private static AmqpConnector connector;
	private static AmqpStub driverStub;
	private static List<IOperationCompletionHandler<Boolean>> handlersBool;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		configuration = PropertyTypeConfiguration.create(new FileInputStream(
				"test/resources/amqp-test.prop"));
		connector = AmqpConnector.create(configuration);
		handlersBool = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlersBool.add(new TestLoggingHandler<Boolean>());
		driverStub = AmqpStub.create(configuration);
		Thread driverThread = new Thread(driverStub);
		driverThread.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// connector.destroy();
		// driverStub.destroy();
	}

	@Test
	public void testOpenConnection() throws InterruptedException,
			ExecutionException {
		Assert.assertNotNull(connector);
		IResult<Boolean> r = connector.openConnection(handlersBool, null);
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

		IResult<Boolean> r = connector.declareExchange(exchange,
				AmqpExchangeType.DIRECT, false, false, false, handlersBool,
				null);
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
		IResult<Boolean> r = connector.declareQueue(queue1, true, false, true,
				false, handlersBool, null);
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
		IResult<Boolean> r = connector.bindQueue(exchange, queue1, routingKey1,
				handlersBool, null);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testCloseConnection() throws InterruptedException,
			ExecutionException {
		IResult<Boolean> r = connector.closeConnection(handlersBool, null);
		Assert.assertTrue(r.getResult());
	}

}
