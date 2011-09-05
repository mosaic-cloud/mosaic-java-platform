package mosaic.connector.amqp.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import mosaic.connector.queue.amqp.AmqpConnector;
import mosaic.core.Serial;
import mosaic.core.SerialJunitRunner;
import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.interop.queue.amqp.AmqpStub;
import mosaic.driver.queue.amqp.AmqpExchangeType;
import mosaic.interop.amqp.AmqpSession;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.mosaic_cloud.exceptions.tools.AbortingExceptionTracer;
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
