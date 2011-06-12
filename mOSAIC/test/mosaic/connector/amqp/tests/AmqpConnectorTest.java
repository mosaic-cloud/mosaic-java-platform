package mosaic.connector.amqp.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import mosaic.connector.queue.amqp.AmqpConnector;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.interop.queue.amqp.AmqpStub;
import mosaic.driver.queue.amqp.AmqpExchangeType;

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
	public static void setUpBeforeClass() throws Throwable {
		configuration = PropertyTypeConfiguration.create(
				AmqpConnectorTest.class.getClassLoader(), "amqp-test.prop");
		connector = AmqpConnector.create(configuration);
		handlersBool = new ArrayList<IOperationCompletionHandler<Boolean>>();
		handlersBool.add(new TestLoggingHandler<Boolean>());
		driverStub = AmqpStub.create(configuration);
		Thread driverThread = new Thread(driverStub);
		driverThread.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Throwable {
		connector.destroy();
		driverStub.destroy();
	}

	@Test
	public void testConnector() throws InterruptedException, ExecutionException {
		Assert.assertNotNull(connector);
	}

	@Test
	public void testDeclareExchange() throws InterruptedException,
			ExecutionException {
		String exchange = ConfigUtils.resolveParameter(configuration,
				"publisher.amqp.exchange", String.class, "");

		IResult<Boolean> r = connector.declareExchange(exchange,
				AmqpExchangeType.DIRECT, false, false, false, handlersBool,
				null);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testDeclareQueue() throws InterruptedException,
			ExecutionException {
		String queue = ConfigUtils.resolveParameter(configuration,
				"consumer.amqp.queue", String.class, "");
		IResult<Boolean> r = connector.declareQueue(queue, true, false, true,
				false, handlersBool, null);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testBindQueue() throws InterruptedException, ExecutionException {
		String exchange = ConfigUtils.resolveParameter(configuration,
				"publisher.amqp.exchange", String.class, "");
		String routingKey = ConfigUtils.resolveParameter(configuration,
				"publisher.amqp.routing_key", String.class, "");
		String queue = ConfigUtils.resolveParameter(configuration,
				"onsumer.amqp.queue", String.class, "");

		IResult<Boolean> r = connector.bindQueue(exchange, queue, routingKey,
				handlersBool, null);
		Assert.assertTrue(r.getResult());
	}

}
