/*
 * #%L
 * mosaic-examples-simple-cloudlets
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

package eu.mosaic_cloud.examples.cloudlets.simple;


import java.util.UUID;

import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudlet;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletCallback;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultCloudletContext;
import eu.mosaic_cloud.cloudlets.tools.v1.callbacks.DefaultKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreConnector;
import eu.mosaic_cloud.platform.implementations.v1.serialization.PlainTextDataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public class KvStoreCloudlet
			extends DefaultCloudlet
{
	public static CallbackCompletion<Void> maybeContinue (final Context context) {
		if (context.count < context.limit) {
			final String key = UUID.randomUUID ().toString ();
			final String data = String.format ("Test value %d! (%s)", Integer.valueOf (context.count), key);
			context.logger.info ("KvStoreCloudlet setting value `{}` -> `{}`.", key, data);
			context.connector.set (key, data, key);
			context.count += 1;
		} else
			context.cloudlet.destroy ();
		return (DefaultCallback.Succeeded);
	}
	
	public static class CloudletCallback
				extends DefaultCloudletCallback<Context>
	{
		@Override
		protected CallbackCompletion<Void> destroy (final Context context) {
			context.logger.info ("PublisherCloudlet destroying...");
			return (context.connector.destroy ());
		}
		
		@Override
		protected CallbackCompletion<Void> destroySucceeded (final Context context) {
			context.logger.info ("PublisherCloudlet destroyed successfully.");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		protected CallbackCompletion<Void> initialize (final Context context) {
			context.logger.info ("PublisherCloudlet initializing...");
			context.connector = context.createKvStoreConnector ("store", String.class, PlainTextDataEncoder.DEFAULT_INSTANCE, ConnectorCallback.class);
			return (context.connector.initialize ());
		}
		
		@Override
		protected CallbackCompletion<Void> initializeSucceeded (final Context context) {
			context.logger.info ("PublisherCloudlet initialized successfully.");
			return (KvStoreCloudlet.maybeContinue (context));
		}
	}
	
	public static class ConnectorCallback
				extends DefaultKvStoreConnectorCallback<Context, String, String>
	{
		@Override
		protected CallbackCompletion<Void> destroySucceeded (final Context context) {
			context.logger.info ("PublisherCloudlet connector destroyed successfully.");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		protected CallbackCompletion<Void> getSucceeded (final Context context, final String value, final String extra) {
			final String key = extra;
			context.logger.info ("KvStoreCloudlet got value `{}` -> `{}`.", key, value);
			return (KvStoreCloudlet.maybeContinue (context));
		}
		
		@Override
		protected CallbackCompletion<Void> initializeSucceeded (final Context context) {
			context.logger.info ("PublisherCloudlet connector initialized successfully.");
			return (DefaultCallback.Succeeded);
		}
		
		@Override
		protected CallbackCompletion<Void> setSucceeded (final Context context, final String extra) {
			final String key = extra;
			context.logger.info ("KvStoreCloudlet getting value `{}`.", key);
			context.connector.get (key, extra);
			return (DefaultCallback.Succeeded);
		}
	}
	
	public static class Context
				extends DefaultCloudletContext<Context>
	{
		public Context (final CloudletController<Context> cloudlet) {
			super (cloudlet);
		}
		
		KvStoreConnector<String, String> connector;
		int count = 0;
		final int delay = 100;
		final int limit = 10;
	}
}
