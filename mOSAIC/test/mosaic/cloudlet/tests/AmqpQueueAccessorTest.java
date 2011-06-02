package mosaic.cloudlet.tests;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import mosaic.cloudlet.resources.amqp.DefaultAmqpAccessorCallback;
import mosaic.connector.queue.AmqpConnector;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.queue.AmqpExchangeType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public final class AmqpQueueAccessorTest {
//	@Before
//	public void prepare() throws Throwable {
//		IConfiguration configuration = PropertyTypeConfiguration
//				.create(new FileInputStream(
//						"test/mosaic/driver/amqp/tests/amqp-test.prop"));
//
//		String exchange = configuration
//				.getParameter(
//						ConfigurationIdentifier
//								.resolveRelative("publisher_1/amqp.publisher.exchange"),
//						String.class).getValue("");
//		String queue1 = configuration
//				.getParameter(
//						ConfigurationIdentifier
//								.resolveRelative("consumer_1/amqp.consumer.queue"),
//						String.class).getValue("");
//		boolean autoAck1 = ConfigUtils.resolveParameter(configuration,
//				"consumer_1/amqp.consumer.auto_ack", Boolean.class, false);
//		boolean exclusive1=ConfigUtils.resolveParameter(configuration,
//				"consumer_1/amqp.consumer.exclusive", Boolean.class, false);
//		String consumer1=ConfigUtils.resolveParameter(configuration,
//				"consumer_1/amqp.consumer.consumer_id", String.class, "");
//		
//		String queue2 = configuration
//				.getParameter(
//						ConfigurationIdentifier
//								.resolveRelative("consumer_2/amqp.consumer.queue"),
//						String.class).getValue("");
//		boolean autoAck2 = ConfigUtils.resolveParameter(configuration,
//				"consumer_2/amqp.consumer.auto_ack", Boolean.class, false);
//		boolean exclusive2=ConfigUtils.resolveParameter(configuration,
//				"consumer_2/amqp.consumer.exclusive", Boolean.class, false);
//		String consumer2=ConfigUtils.resolveParameter(configuration,
//				"consumer_2/amqp.consumer.consumer_id", String.class, "");
//
//		String routingKey1 = configuration
//				.getParameter(
//						ConfigurationIdentifier
//								.resolveRelative("publisher_1/amqp.publisher.exchange"),
//						String.class).getValue("");
//		String routingKey2 = configuration
//				.getParameter(
//						ConfigurationIdentifier
//								.resolveRelative("publisher_2/amqp.publisher.exchange"),
//						String.class).getValue("");
//
//		boolean manadatory = ConfigUtils.resolveParameter(configuration,
//				"publisher_1/amqp.publisher.mandatory", Boolean.class, false);
//		boolean immediate=ConfigUtils.resolveParameter(configuration,
//				"publisher_1/amqp.publisher.immediate", Boolean.class, false);
//		boolean durable=ConfigUtils.resolveParameter(configuration,
//				"publisher_1/amqp.publisher.durable", Boolean.class, false);
//
//		AmqpConnector amqpConnector = AmqpConnector.create(configuration);
//
//		List<IOperationCompletionHandler<Boolean>> emptyHandlers = Collections
//				.emptyList();
//		IResult<Boolean> result1 = amqpConnector.openConnection(emptyHandlers,
//				null);
//		result1.getResult();
//
//		IResult<Boolean> result2=amqpConnector.declareExchange(exchange, AmqpExchangeType.DIRECT,
//				durable, false, false, emptyHandlers, null);
//		amqpConnector.declareQueue(queue1, exclusive1, false, false, false,emptyHandlers, null);
//		amqpConnector.declareQueue(queue2, exclusive2, false, false, false,emptyHandlers, null);
//		
//		result2.getResult();
//		
//		amqpConnector.bindQueue(exchange, queue1, routingKey1, emptyHandlers, null);
//		amqpConnector.bindQueue(exchange, queue2, routingKey2, emptyHandlers, null);
//		
//		amqpWrapper.executeConnectionClose();
//
//		amqpWrapper.destroy();
//
//		if (!amqpWrapperCallbacks.exceptions.isEmpty())
//			throw (amqpWrapperCallbacks.exceptions.remove());
//		for (final Throwable exception : ExceptionTracer.defaultInstance
//				.selectIgnoredExceptions())
//			throw (exception);
//	}
//
//	public final void testConsumer() throws Throwable {
//		ExceptionTracer.defaultInstance.resetIgnoredExceptions();
//
//		final Configuration configuration = BasicConfigurationCreator.defaultInstance
//				.create(Cloudlet2Test.class.getClassLoader(),
//						"amqp-queue-accessor-test.properties")
//				.spliceConfiguration(
//						ConfigurationIdentifier.resolveRelative("consumer_3"));
//
//		final ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<Throwable>();
//
//		final CallbackReactor callbackReactor = BasicCallbackReactorCreator.defaultInstance
//				.createCallbackReactor();
//
//		final CallbackTag consumerCallbackTag = WrappedCallbackTag
//				.wrap("consumer");
//		final Semaphore consumerDone = new Semaphore(0);
//
//		final CallbackHandler<Object> consumerCallbackHandler = new CallbackHandler<Object>() {
//
//			@Override
//			public final void callback(final Object state,
//					final Callback callback) {
//				final CallbackType callbackType = callback.getType();
//				final CallbackArguments callbackArguments = callback
//						.getArguments();
//				final CallbackTag callbackTag = callback.getTag();
//
//				if (callbackTag == consumerCallbackTag) {
//
//					Assert.assertTrue(callbackArguments instanceof QueueConsumerCallbackArguments);
//					final QueueConsumerCallbackType consumerCallbackType = (QueueConsumerCallbackType) callbackType;
//					final QueueConsumerCallbackArguments consumerCallbackArguments = (QueueConsumerCallbackArguments) callbackArguments;
//					final QueueConsumer consumer = consumerCallbackArguments.accessor;
//
//					switch (consumerCallbackType) {
//
//					case InitializeSucceeded:
//						consumer.register();
//						break;
//
//					case RegisterSucceeded:
//						break;
//
//					case Consume:
//						Assert.assertTrue(consumerCallbackArguments instanceof QueueConsumerCallbackConsumeArguments);
//						final QueueConsumerCallbackConsumeArguments consumeCallbackArguments = (QueueConsumerCallbackConsumeArguments) consumerCallbackArguments;
//						final QueueConsumerMessage consumeMessage = consumeCallbackArguments.message;
//						consumeMessage.acknowledge();
//						break;
//
//					case AcknowledgeSucceeded:
//						consumer.unregister();
//						consumerDone.release();
//						break;
//
//					case UnregisterSucceeded:
//						consumer.destroy();
//						break;
//
//					case DestroySucceeded:
//						consumerDone.release();
//						break;
//
//					case RegisterFailed:
//					case AcknowledgeFailed:
//					case UnregisterFailed:
//						exceptions.add(new Exception(String.format(
//								"failed consumer operation: %s %s",
//								callbackTag, consumerCallbackType)));
//						consumer.destroy();
//						break;
//
//					case InitializeFailed:
//					case DestroyFailed:
//						exceptions.add(new Exception(String.format(
//								"failed consumer operation: %s %s",
//								callbackTag, consumerCallbackType)));
//						consumerDone.release();
//						break;
//
//					default:
//						exceptions
//								.add(new UnsupportedOperationException(
//										String.format(
//												"unknown consumer callback type: %s %s",
//												callbackTag,
//												consumerCallbackType)));
//						break;
//					}
//
//				} else
//					exceptions.add(new UnsupportedOperationException(String
//							.format("unknown callback tag: %s", callbackTag)));
//			}
//		};
//
//		final CallbackTarget consumerCallbackTarget = callbackReactor
//				.createTarget(consumerCallbackHandler, null);
//
//		final QueueConsumer consumer = AmqpQueueAccessorCreator.defaultInstance
//				.createQueueConsumer(configuration, byte[].class);
//
//		consumer.initialize(consumerCallbackTarget, consumerCallbackTag);
//
//		if (!consumerDone.tryAcquire(1 * 1000, TimeUnit.MILLISECONDS))
//			throw (new InterruptedException());
//
//		callbackReactor.destroy();
//
//		if (!exceptions.isEmpty())
//			throw (exceptions.remove());
//		for (final Throwable exception : ExceptionTracer.defaultInstance
//				.selectIgnoredExceptions())
//			throw (exception);
//	}
//
//	public final void testPublisher() throws Throwable {
//		ExceptionTracer.defaultInstance.resetIgnoredExceptions();
//
//		final Configuration configuration = BasicConfigurationCreator.defaultInstance
//				.create(Cloudlet2Test.class.getClassLoader(),
//						"amqp-queue-accessor-test.properties")
//				.spliceConfiguration(
//						ConfigurationIdentifier.resolveRelative("publisher_3"));
//
//		final ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<Throwable>();
//
//		final CallbackReactor callbackReactor = BasicCallbackReactorCreator.defaultInstance
//				.createCallbackReactor();
//
//		final CallbackTag publisherCallbackTag = WrappedCallbackTag
//				.wrap("consumer");
//		final Semaphore publisherDone = new Semaphore(0);
//
//		final CallbackHandler<Object> publisherCallbackHandler = new CallbackHandler<Object>() {
//			@Override
//			public final void callback(final Object state,
//					final Callback callback) {
//
//				final CallbackType callbackType = callback.getType();
//				final CallbackArguments callbackArguments = callback
//						.getArguments();
//				final CallbackTag callbackTag = callback.getTag();
//
//				if (callbackTag == publisherCallbackTag) {
//
//					Assert.assertTrue(callbackArguments instanceof QueuePublisherCallbackArguments);
//					final QueuePublisherCallbackType publisherCallbackType = (QueuePublisherCallbackType) callbackType;
//					final QueuePublisherCallbackArguments publisherCallbackArguments = (QueuePublisherCallbackArguments) callbackArguments;
//					final QueuePublisher publisher = publisherCallbackArguments.accessor;
//
//					switch (publisherCallbackType) {
//
//					case InitializeSucceeded:
//						publisher.register();
//						break;
//
//					case RegisterSucceeded:
//						publisher
//								.publish("hello".getBytes(), UUID.randomUUID());
//						break;
//
//					case PublishSucceeded:
//						publisher.unregister();
//						break;
//
//					case UnregisterSucceeded:
//						publisher.destroy();
//						break;
//
//					case DestroySucceeded:
//						publisherDone.release();
//						break;
//
//					case RegisterFailed:
//					case PublishFailed:
//					case UnregisterFailed:
//						exceptions.add(new Exception(String.format(
//								"failed consumer operation: %s %s",
//								callbackTag, publisherCallbackType)));
//						publisher.destroy();
//						break;
//
//					case InitializeFailed:
//					case DestroyFailed:
//						exceptions.add(new Exception(String.format(
//								"failed consumer operation: %s %s",
//								callbackTag, publisherCallbackType)));
//						publisherDone.release();
//						break;
//
//					default:
//						exceptions
//								.add(new UnsupportedOperationException(
//										String.format(
//												"unknown consumer callback type: %s %s",
//												callbackTag,
//												publisherCallbackType)));
//						break;
//					}
//
//				} else
//					exceptions.add(new UnsupportedOperationException(String
//							.format("unknown callback tag: %s", callbackTag)));
//			}
//		};
//
//		final CallbackTarget publisherCallbackTarget = callbackReactor
//				.createTarget(publisherCallbackHandler, null);
//
//		final QueuePublisher publisher = AmqpQueueAccessorCreator.defaultInstance
//				.createQueuePublisher(configuration, byte[].class);
//
//		publisher.initialize(publisherCallbackTarget, publisherCallbackTag);
//
//		if (!publisherDone.tryAcquire(1 * 1000, TimeUnit.MILLISECONDS))
//			throw (new InterruptedException());
//
//		callbackReactor.destroy();
//
//		if (!exceptions.isEmpty())
//			throw (exceptions.remove());
//		for (final Throwable exception : ExceptionTracer.defaultInstance
//				.selectIgnoredExceptions())
//			throw (exception);
//	}
//
//	@Test
//	public final void testPublisherAndConsumer() throws Throwable {
//		this.testPublisher();
//		this.testConsumer();
//	}
//
//	@Test
//	public final void testPublisherConsumerBenchmark() throws Throwable {
//		this.testPublisherConsumerBenchmark(2, 1 * 1000);
//	}
//
//	public final void testPublisherConsumerBenchmark(final int consumers,
//			final int iterations) throws Throwable {
//		ExceptionTracer.defaultInstance.resetIgnoredExceptions();
//
//		final Configuration globalConfiguration = BasicConfigurationCreator.defaultInstance
//				.create(Cloudlet2Test.class.getClassLoader(),
//						"amqp-queue-accessor-test.properties");
//
//		final Configuration consumer1Configuration = globalConfiguration
//				.spliceConfiguration(ConfigurationIdentifier
//						.resolveRelative("consumer_1"));
//		final Configuration consumer2Configuration = globalConfiguration
//				.spliceConfiguration(ConfigurationIdentifier
//						.resolveRelative("consumer_2"));
//
//		final Configuration publisher1Configuration = globalConfiguration
//				.spliceConfiguration(ConfigurationIdentifier
//						.resolveRelative("publisher_1"));
//		final Configuration publisher2Configuration = globalConfiguration
//				.spliceConfiguration(ConfigurationIdentifier
//						.resolveRelative("publisher_2"));
//
//		final ConcurrentLinkedQueue<Throwable> exceptions = new ConcurrentLinkedQueue<Throwable>();
//
//		/*
//		 * The proposed connector API embraces a fully EDA
//		 * ("Event Driven Architecture") metaphor. The reactor is a class that
//		 * manages callback handlers, callback dispatching and execution
//		 * threads.
//		 */
//
//		final CallbackReactor callbackReactor = BasicCallbackReactorCreator.defaultInstance
//				.createCallbackReactor();
//
//		/*
//		 * In our EDA model each event is characterized by two things: an
//		 * callback tag and a callback arguments. The callback tag is chosen by
//		 * the handler itself (or a component that is tightly coupled with the
//		 * handler), and the arguments are given by the callback source. In this
//		 * example we choose to tag each callback by the name of the queue to
//		 * which the consumer or message belongs in case of consumers, and a
//		 * generic name in case of publishers.
//		 */
//
//		final CallbackTag queueConsumer1CallbackTag = WrappedCallbackTag
//				.wrap("consumer-1");
//		final CallbackTag queueConsumer2CallbackTag = WrappedCallbackTag
//				.wrap("consumer-2");
//		final CallbackTag queuePublisher1CallbackTag = WrappedCallbackTag
//				.wrap("consumer-1");
//		final CallbackTag queuePublisher2CallbackTag = WrappedCallbackTag
//				.wrap("consumer-2");
//
//		/*
//		 * The semaphore and chronometer are used to know when (and how much
//		 * time elapsed until) all the sent messages were consumed and
//		 * acknowledged.
//		 */
//
//		final Semaphore consumeSemaphore = new Semaphore(
//				0 - (iterations * 2) + 1);
//		final Semaphore publishSemaphore = new Semaphore(
//				0 - (iterations * 2) + 1);
//		final Chronometer consumeChronometer = new Chronometer();
//
//		final CallbackHandler<Object> callbackHandler = new CallbackHandler<Object>() {
//
//			@Override
//			public final void callback(final Object state,
//					final Callback callback) {
//				final CallbackTag callbackTag = callback.getTag();
//
//				/*
//				 * In case the callback tag is one of the two ones created early
//				 * we know that this callback comes from one of the
//				 * `QueueConsumer` or the `QueuePublisher` instances. (Otherwise
//				 * it must belong to some other callback source to which we've
//				 * registered.)
//				 */
//
//				if ((callbackTag == queueConsumer1CallbackTag)
//						|| (callbackTag == queueConsumer2CallbackTag)) {
//
//					/*
//					 * As this callback came from one of the the `QueueConsumer`
//					 * instances we know -- by convention -- that the callback
//					 * type and arguments must be of the
//					 * `QueueConsumerCallbackType`, respectively
//					 * `QueueConsumerCallbackArguments` classes. (Otherwise we
//					 * have a serious implementation bug in our library.)
//					 */
//
//					final QueueConsumerCallbackType callbackType = (QueueConsumerCallbackType) callback
//							.getType();
//					final QueueConsumerCallbackArguments callbackArguments = (QueueConsumerCallbackArguments) callback
//							.getArguments();
//
//					/*
//					 * The callback handler uses the `QueueConsumerCallbackType`
//					 * enumeration (accessed from the member `type` of the
//					 * received callback arguments) to determine the type of the
//					 * callback triggered by the `QueueConsumer` instance.
//					 */
//
//					switch (callbackType) {
//
//					case Consume:
//
//						/*
//						 * In the case of a `Consume` callback type, we know --
//						 * again by convention -- that the callback arguments,
//						 * although an instance of
//						 * `QueueConsumerCallbackArguments`, it is more exactly
//						 * an instance of the
//						 * `QueueConsumerConsumeCallbackArguments` class
//						 * (derived from the previous one), which also contains
//						 * in the `message` member, an instance of the
//						 * `QueueConsumerMessage` interface.
//						 */
//
//						final QueueConsumerMessage message = ((QueueConsumerCallbackConsumeArguments) callbackArguments).message;
//
//						/*
//						 * We must acknowledge the message. (We must decide what
//						 * means / happens with an unacknowledged message.)
//						 */
//
//						message.acknowledge();
//						break;
//
//					case AcknowledgeSucceeded:
//
//						/*
//						 * Because we have acknowledged the message previously,
//						 * a new callback was generated to mark the success.
//						 */
//
//						consumeSemaphore.release();
//						break;
//
//					/*
//					 * All the following callbacks are generated as consequence
//					 * of `initialize`, `register`, `unregister` or `destroy`
//					 * method calls. We are ignoring for now any failures. (The
//					 * types are as above but with the prefix `Failed`.)
//					 */
//
//					case InitializeSucceeded:
//					case RegisterSucceeded:
//					case UnregisterSucceeded:
//					case DestroySucceeded:
//						break;
//
//					case InitializeFailed:
//					case RegisterFailed:
//					case UnregisterFailed:
//					case DestroyFailed:
//						exceptions.add(new UnsupportedOperationException(String
//								.format("unhandeled consumer callback: %s %s",
//										callbackTag, callbackType)));
//						break;
//
//					default:
//						exceptions.add(new UnsupportedOperationException(String
//								.format("unknown consumer callback: %s %s",
//										callbackTag, callbackType)));
//						break;
//					}
//
//				} else if ((callbackTag == queuePublisher1CallbackTag)
//						|| (callbackTag == queuePublisher2CallbackTag)) {
//
//					final QueuePublisherCallbackType callbackType = (QueuePublisherCallbackType) callback
//							.getType();
//
//					switch (callbackType) {
//
//					case PublishSucceeded:
//						publishSemaphore.release();
//						break;
//
//					case InitializeSucceeded:
//					case RegisterSucceeded:
//					case UnregisterSucceeded:
//					case DestroySucceeded:
//						break;
//
//					case InitializeFailed:
//					case RegisterFailed:
//					case UnregisterFailed:
//					case DestroyFailed:
//						exceptions.add(new UnsupportedOperationException(String
//								.format("unhandeled consumer callback: %s %s",
//										callbackTag, callbackType)));
//						break;
//
//					default:
//						exceptions.add(new UnsupportedOperationException(String
//								.format("unknown consumer callback: %s %s",
//										callbackTag, callbackType)));
//						break;
//					}
//
//				} else
//					exceptions.add(new UnsupportedOperationException(String
//							.format("unknown callback tag: %s", callbackTag)));
//			}
//		};
//
//		/*
//		 * Our EDA model makes a clear separation between code and state. Thus
//		 * the instance of the `CallbackHandler` interfaces only contains the
//		 * code, and the state parameter (which in our case is `null`)
//		 * represents the data. (It's just like in the case of a normal Java
//		 * class, but from which we put all methods in one class, and all the
//		 * fields in another, and we replace the keyword `this` from the methods
//		 * class with the word `state`. (This pattern would allow us live
//		 * code-updates, because we can swap the code without touching the
//		 * data.) Furthermore in our model the callback is not sent to the
//		 * handler directly, but throgh the abstraction of an "callback target".
//		 */
//
//		final CallbackTarget callbackTarget = callbackReactor.createTarget(
//				callbackHandler, null);
//
//		/* We keep a list of accessors, to control them in a batch. */
//
//		final LinkedList<QueueAccessor> queueAccessors = new LinkedList<QueueAccessor>();
//
//		/*
//		 * We create multiple consumers for each queue, as many different (and
//		 * parallel consumers) as we have specified in the `consumers`
//		 * argument.) (We should notice that inside the handler we can't
//		 * disambiguate the consumers based solely on the callback tag (because
//		 * we give them the same callback tag), but we can obtain a reference to
//		 * the actual `QueueConsumer` instance through the
//		 * `QueueConsumerCallbackArguments.consumer`.)
//		 */
//
//		for (int consumer = 0; consumer < consumers; consumer++) {
//
//			/*
//			 * Initially the consumer instance is created and takes as a
//			 * creation argument the name of the resource. (In our case the name
//			 * of the resource identifies the name of the queue to consume from,
//			 * as in "queue!consumer" (consumer can be ""), but in the final
//			 * version of the connector API, this should be an identifier to a
//			 * connector / resource specified in the application descriptor.)
//			 */
//
//			final QueueConsumer queueConsumer1 = AmqpQueueAccessorCreator.defaultInstance
//					.createQueueConsumer(consumer1Configuration, byte[].class);
//
//			/*
//			 * The previous step just created a Java object, but nothing
//			 * happened on the network. The `initialize` method is the one that
//			 * binds an callback target and an callback tag to the consumer, and
//			 * opens the network connection.
//			 */
//
//			queueConsumer1
//					.initialize(callbackTarget, queueConsumer1CallbackTag);
//
//			/*
//			 * Only after the `register` method is the AMQP `basic.consume`
//			 * message sent, which prepares our consumer for actual message
//			 * deliveries.
//			 */
//
//			queueConsumer1.register();
//
//			/*
//			 * We must note that the previous 3 operations should have been done
//			 * from inside the callback handler. For example `register` should
//			 * have been called when we've received an `InitializeSucceeded`
//			 * callback.
//			 */
//
//			final QueueConsumer queueConsumer2 = AmqpQueueAccessorCreator.defaultInstance
//					.createQueueConsumer(consumer2Configuration, byte[].class);
//			queueConsumer2
//					.initialize(callbackTarget, queueConsumer2CallbackTag);
//			queueConsumer2.register();
//
//			queueAccessors.add(queueConsumer1);
//			queueAccessors.add(queueConsumer2);
//		}
//
//		/*
//		 * We also create only two publishers, one for each queue. (As in the
//		 * case of consumers, the resource identifies the exchange and routing
//		 * key to publish to as "exchange!routing-key".
//		 */
//		final QueuePublisher queuePublisher1 = AmqpQueueAccessorCreator.defaultInstance
//				.createQueuePublisher(publisher1Configuration, byte[].class);
//		queuePublisher1.initialize(callbackTarget, queuePublisher1CallbackTag);
//		queuePublisher1.register();
//
//		final QueuePublisher queuePublisher2 = AmqpQueueAccessorCreator.defaultInstance
//				.createQueuePublisher(publisher2Configuration, byte[].class);
//		queuePublisher2.initialize(callbackTarget, queuePublisher2CallbackTag);
//		queuePublisher2.register();
//
//		queueAccessors.add(queuePublisher1);
//		queueAccessors.add(queuePublisher2);
//
//		Thread.sleep(100);
//
//		consumeChronometer.start();
//
//		/*
//		 * We publish a number of messages to the two queues (the same amount to
//		 * each queue, thus we end-up with twice messages as the argument
//		 * `iterations` specifies.)
//		 */
//		for (int iteration = 0; iteration < iterations; iteration++) {
//			final byte[] data = String.valueOf(iteration).getBytes();
//			queuePublisher1.publish(data, iteration);
//			queuePublisher2.publish(data, iteration);
//		}
//
//		/*
//		 * During the publishing and during the waiting, the messages are
//		 * consumed asynchronously through the callback reactor by our callback
//		 * handler.
//		 */
//		if (!publishSemaphore.tryAcquire(10 * 1000, TimeUnit.MILLISECONDS))
//			throw (new InterruptedException());
//		if (!consumeSemaphore.tryAcquire(10 * 1000, TimeUnit.MILLISECONDS))
//			throw (new InterruptedException());
//		consumeChronometer.stop();
//		Transcript.traceDebugging("consumed: %f -> %f m/s",
//				consumeChronometer.getAccumulated(), (iterations * 2)
//						/ consumeChronometer.getAccumulated());
//
//		/*
//		 * Cleanup. (As in the case of `register`, the `destroy` method should
//		 * be called from within the handler when the message
//		 * `UnregisterSucceeded` was received.)
//		 */
//
//		Thread.sleep(100);
//
//		queuePublisher1.unregister();
//		for (final QueueAccessor accessor : queueAccessors)
//			accessor.unregister();
//
//		Thread.sleep(100);
//
//		queuePublisher1.unregister();
//		for (final QueueAccessor accessor : queueAccessors)
//			accessor.destroy();
//
//		Thread.sleep(100);
//
//		callbackReactor.destroy();
//
//		if (!exceptions.isEmpty())
//			throw (exceptions.remove());
//		for (final Throwable exception : ExceptionTracer.defaultInstance
//				.selectIgnoredExceptions())
//			throw (exception);
//	}
//
//	public static final void main(final String[] arguments) throws Throwable {
//		Assert.assertTrue(arguments.length == 0);
//		final AmqpQueueAccessorTest tests = new AmqpQueueAccessorTest();
//		tests.prepare();
//		tests.testPublisher();
//		tests.testConsumer();
//		tests.testPublisherConsumerBenchmark();
//	}
}
