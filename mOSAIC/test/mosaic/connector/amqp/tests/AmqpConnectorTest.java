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

	@BeforeClass
	public static void setUpBeforeClass() throws Throwable {
		configuration = PropertyTypeConfiguration.create(
				AmqpConnectorTest.class.getClassLoader(), "amqp-test.prop");
		connector = AmqpConnector.create(configuration);
		driverStub = AmqpStub.create(configuration);
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

	private List<IOperationCompletionHandler<Boolean>> getHandlers(
			String testName) {
		IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>(
				testName);
		List<IOperationCompletionHandler<Boolean>> list = new ArrayList<IOperationCompletionHandler<Boolean>>();
		list.add(handler);
		return list;
	}

	@Test
	public void testDeclareExchange() throws InterruptedException,
			ExecutionException {
		String exchange = ConfigUtils.resolveParameter(configuration,
				"publisher.amqp.exchange", String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("declare exchange");

		IResult<Boolean> r = connector.declareExchange(exchange,
				AmqpExchangeType.DIRECT, false, false, false, handlers, null);
		Assert.assertTrue(r.getResult());
	}

	@Test
	public void testDeclareQueue() throws InterruptedException,
			ExecutionException {
		String queue = ConfigUtils.resolveParameter(configuration,
				"consumer.amqp.queue", String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("declare queue");
		IResult<Boolean> r = connector.declareQueue(queue, true, false, true,
				false, handlers, null);
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
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("bind queue");
		IResult<Boolean> r = connector.bindQueue(exchange, queue, routingKey,
				handlers, null);

		Assert.assertTrue(r.getResult());
	}

}
