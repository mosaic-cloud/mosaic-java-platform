/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.connectors.queue.amqp;


import eu.mosaic_cloud.cloudlets.connectors.core.BaseConnector;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;


/**
 * Base accessor class for AMQP queuing systems.
 * 
 * @author Georgiana Macariu
 * 
 * @param <Context>
 *            the type of the context of the cloudlet using the accessor
 * @param <Message>
 *            the type of messages processed by the accessor
 */
public abstract class BaseAmqpQueueConnector<Connector extends eu.mosaic_cloud.connectors.queue.amqp.IAmqpQueueConnector, Callback extends IAmqpQueueConnectorCallback<Context>, Context>
		extends BaseConnector<Connector, Callback, Context>
		implements
			IAmqpQueueConnector<Context>
{
	protected BaseAmqpQueueConnector (final ICloudletController<?> cloudlet, final Connector connector, final IConfiguration configuration, final Callback callback, final Context context)
	{
		super (cloudlet, connector, configuration, callback, context);
	}
}
