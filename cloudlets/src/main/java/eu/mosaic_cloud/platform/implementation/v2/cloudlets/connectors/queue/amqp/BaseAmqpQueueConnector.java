/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.platform.implementation.v2.cloudlets.connectors.core.BaseConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.amqp.AmqpQueueConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.amqp.AmqpQueueConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;


/**
 * Base connector class for AMQP queuing systems.
 * 
 * @author Georgiana Macariu
 * @param <TConnector>
 *            the type of the base connector used by this cloudlet-level connector
 * @param <TCallback>
 *            the type of the callback class
 * @param <TContext>
 *            the type of the context of the cloudlet using the connector
 */
public abstract class BaseAmqpQueueConnector<TConnector extends eu.mosaic_cloud.platform.v2.connectors.queue.amqp.AmqpQueueConnector, TCallback extends AmqpQueueConnectorCallback<TContext>, TContext>
			extends BaseConnector<TConnector, TCallback, TContext>
			implements
				AmqpQueueConnector
{
	protected BaseAmqpQueueConnector (final CloudletController<?> cloudlet, final TConnector connector, final Configuration configuration, final TCallback callback, final TContext context) {
		super (cloudlet, connector, configuration, callback, context);
	}
}
