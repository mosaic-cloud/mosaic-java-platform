/*
 * #%L
 * mosaic-connectors
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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
package eu.mosaic_cloud.connectors.components;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.connectors.components.ResourceComponentCallbacks.ResourceType;
import eu.mosaic_cloud.connectors.queue.amqp.AmqpConnector;
import eu.mosaic_cloud.drivers.queue.amqp.AmqpExchangeType;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.interop.idl.ChannelData;
import eu.mosaic_cloud.tools.threading.tools.Threading;

public class AmqpConnectorCompTest {

	private IConfiguration configuration;
	private AmqpConnector connector;

	public AmqpConnectorCompTest() throws Throwable {
		this.configuration = PropertyTypeConfiguration.create(
				AmqpConnectorCompTest.class.getClassLoader(),
				"amqp-conn-test.prop");

		MosaicLogger.getLogger().info(
				"AMQP Connector test: configuration loaded. "
						+ this.configuration);

		ResourceFinder.getResourceFinder().findResource(ResourceType.AMQP,
				new Callback());
	}

	public void destroy() throws Throwable {
		if (this.connector != null) {
			this.connector.destroy();
			MosaicLogger.getLogger().info(
					"AMQP Connector test: connector destroyed.");
		}
	}

	public void testConnector() throws InterruptedException, ExecutionException {
		Preconditions.checkNotNull(this.connector);
		MosaicLogger.getLogger()
				.info("AMQP Connector test: connector created.");
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
		String exchange = ConfigUtils.resolveParameter(this.configuration,
				"queue.publisher.amqp.exchange", String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("declare exchange");

		IResult<Boolean> r = this.connector.declareExchange(exchange,
				AmqpExchangeType.DIRECT, false, false, false, handlers, null);
		Preconditions.checkState(r.getResult(), "ERROR: exchange not declared");
		MosaicLogger.getLogger()
				.info("AMQP Connector test: exchange declared.");
	}

	public void testDeclareQueue() throws InterruptedException,
			ExecutionException {
		String queue = ConfigUtils.resolveParameter(this.configuration,
				"queue.consumer.amqp.queue", String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("declare queue");
		IResult<Boolean> r = this.connector.declareQueue(queue, true, false,
				true, false, handlers, null);
		Preconditions.checkState(r.getResult(), "ERROR: queue not declared");
		MosaicLogger.getLogger().info("AMQP Connector test: queue declared.");
	}

	public void testBindQueue() throws InterruptedException, ExecutionException {
		String exchange = ConfigUtils.resolveParameter(this.configuration,
				"queue.publisher.amqp.exchange", String.class, "");
		String routingKey = ConfigUtils.resolveParameter(this.configuration,
				"queue.publisher.amqp.routing_key", String.class, "");
		String queue = ConfigUtils.resolveParameter(this.configuration,
				"queue.consumer.amqp.queue", String.class, "");
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("bind queue");
		IResult<Boolean> r = this.connector.bindQueue(exchange, queue,
				routingKey, handlers, null);

		Preconditions.checkState(r.getResult(), "ERROR: queue not bounded");
		MosaicLogger.getLogger()
				.info("AMQP Connector test: connector bounded.");
	}

	public void testConn() throws InterruptedException, ExecutionException {
		testConnector();
		testDeclareExchange();
		testDeclareQueue();
		testBindQueue();
	}

	public static void test() throws Throwable {
		new AmqpConnectorCompTest();
	}

	class Callback implements IFinderCallback {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * eu.mosaic_cloud.connectors.temp.IFinderCallback#resourceFound(eu.
		 * mosaic_cloud.platform.interop .idl.ChannelData)
		 */
		@Override
		public void resourceFound(ChannelData channel) throws Throwable {
			AmqpConnectorCompTest.this.configuration
					.addParameter("interop.driver.identifier",
							channel.getChannelIdentifier());
			AmqpConnectorCompTest.this.configuration.addParameter(
					"interop.channel.address", channel.getChannelEndpoint());
			AmqpConnectorCompTest.this.connector = AmqpConnector.create(
					AmqpConnectorCompTest.this.configuration,
					Threading.sequezeThreadingContextOutOfDryRock());
			AmqpConnectorCompTest.this.testConn();
			AmqpConnectorCompTest.this.destroy();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * eu.mosaic_cloud.connectors.temp.IFinderCallback#resourceNotFound()
		 */
		@Override
		public void resourceNotFound() {
			MosaicLogger.getLogger().error("Callback - Resource not found");
		}
	}

}
