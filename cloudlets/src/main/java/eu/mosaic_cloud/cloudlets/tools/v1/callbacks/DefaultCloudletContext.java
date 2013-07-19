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

package eu.mosaic_cloud.cloudlets.tools.v1.callbacks;


import eu.mosaic_cloud.cloudlets.v1.cloudlets.CloudletController;
import eu.mosaic_cloud.cloudlets.v1.connectors.components.ComponentConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.components.ComponentConnectorCallbacks;
import eu.mosaic_cloud.cloudlets.v1.connectors.components.ComponentConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.Connector;
import eu.mosaic_cloud.cloudlets.v1.connectors.core.ConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.Executor;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.ExecutorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.executors.ExecutorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.httpg.HttpgQueueConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.httpg.HttpgQueueConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.httpg.HttpgQueueConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.kvstore.KvStoreConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueueConsumerConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnector;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnectorCallback;
import eu.mosaic_cloud.cloudlets.v1.connectors.queue.amqp.AmqpQueuePublisherConnectorFactory;
import eu.mosaic_cloud.cloudlets.v1.core.Callback;
import eu.mosaic_cloud.platform.implementations.v1.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.v1.core.configuration.Configuration;
import eu.mosaic_cloud.platform.v1.core.configuration.ConfigurationIdentifier;
import eu.mosaic_cloud.platform.v1.core.serialization.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;

import com.google.common.base.Preconditions;


public class DefaultCloudletContext<TContext extends DefaultCloudletContext<TContext>>
			extends DefaultContext
{
	public DefaultCloudletContext (final CloudletController<TContext> cloudlet) {
		super (cloudlet);
	}
	
	public DefaultCloudletContext (final DefaultContext parent) {
		super (parent);
	}
	
	public CallbackCompletion<Void> chain (final CallbackCompletion<?> ... dependents) {
		return (CallbackCompletion.createChained (dependents));
	}
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createAmqpQueueConsumerConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (AmqpQueueConsumerConnectorFactory.class).create ((configuration != null) ? configuration : PropertyTypeConfiguration.createEmpty (), messageClass, messageEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createAmqpQueueConsumerConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createAmqpQueueConsumerConnector (configuration, messageClass, messageEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createAmqpQueueConsumerConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.createAmqpQueueConsumerConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createAmqpQueueConsumerConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueueConsumerConnector<TMessage, TExtra> createAmqpQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createAmqpQueueConsumerConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callbackClass, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createAmqpQueuePublisherConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (AmqpQueuePublisherConnectorFactory.class).create ((configuration != null) ? configuration : PropertyTypeConfiguration.createEmpty (), messageClass, messageEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createAmqpQueuePublisherConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final Configuration configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createAmqpQueuePublisherConnector (configuration, messageClass, messageEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createAmqpQueuePublisherConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.createAmqpQueuePublisherConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createAmqpQueuePublisherConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> AmqpQueuePublisherConnector<TMessage, TExtra> createAmqpQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends AmqpQueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createAmqpQueuePublisherConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callbackClass, callbackContext));
	}
	
	public <TCallback extends Callback<TContext>, TContext> TCallback createCallback (final Class<TCallback> callbackClass) {
		Preconditions.checkNotNull (callbackClass);
		final TCallback callback;
		try {
			callback = callbackClass.getConstructor ().newInstance ();
		} catch (final CaughtException.Wrapper wrapper) {
			throw (wrapper);
		} catch (final Throwable exception) {
			throw (CaughtException.create (ExceptionResolution.Deferred, exception, "failed creating callback instance; aborting!").wrap ());
		}
		return (callback);
	}
	
	@SuppressWarnings ("unchecked")
	public <TExtra> ComponentConnector<TExtra> createComponentConnector (final Class<? extends ComponentConnectorCallbacks<TContext, TExtra>> callbackClass) {
		return (this.createComponentConnector (callbackClass, (TContext) this));
	}
	
	public <TContext, TExtra> ComponentConnector<TExtra> createComponentConnector (final Class<? extends ComponentConnectorCallbacks<TContext, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createComponentConnector (this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TExtra> ComponentConnector<TExtra> createComponentConnector (final ComponentConnectorCallbacks<TContext, TExtra> callback) {
		return (this.createComponentConnector (callback, (TContext) this));
	}
	
	public <TContext, TExtra> ComponentConnector<TExtra> createComponentConnector (final ComponentConnectorCallbacks<TContext, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (ComponentConnectorFactory.class).create (callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final Class<? extends ExecutorCallback<TContext, TOutcome, TExtra>> callbackClass) {
		return (this.createExecutor ((Configuration) null, callbackClass, (TContext) this));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final Class<? extends ExecutorCallback<TContext, TOutcome, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createExecutor ((Configuration) null, callbackClass, callbackContext));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final Configuration configuration, final Class<? extends ExecutorCallback<TContext, TOutcome, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createExecutor (configuration, this.createCallback (callbackClass), callbackContext));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final Configuration configuration, final ExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (ExecutorFactory.class).create ((configuration != null) ? configuration : PropertyTypeConfiguration.createEmpty (), callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final ExecutorCallback<TContext, TOutcome, TExtra> callback) {
		return (this.createExecutor ((Configuration) null, callback, (TContext) this));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final ExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext) {
		return (this.createExecutor ((Configuration) null, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final String configuration, final Class<? extends ExecutorCallback<TContext, TOutcome, TExtra>> callbackClass) {
		return (this.createExecutor (configuration, callbackClass, (TContext) this));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final String configuration, final Class<? extends ExecutorCallback<TContext, TOutcome, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createExecutor ((configuration != null) ? this.spliceConfiguration (configuration) : null, callbackClass, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final String configuration, final ExecutorCallback<TContext, TOutcome, TExtra> callback) {
		return (this.createExecutor (configuration, callback, (TContext) this));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final String configuration, final ExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext) {
		return (this.createExecutor ((configuration != null) ? this.spliceConfiguration (configuration) : null, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final Configuration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final Class<? extends HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>> callbackClass) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final Configuration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final Class<? extends HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final Configuration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback, (TContext) this));
	}
	
	public <TContext, TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final Configuration configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (HttpgQueueConnectorFactory.class).create ((configuration != null) ? configuration : PropertyTypeConfiguration.createEmpty (), requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final String configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final Class<? extends HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>> callbackClass) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final String configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final Class<? extends HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createHttpgQueueConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callbackClass, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final String configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback, (TContext) this));
	}
	
	public <TContext, TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final String configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback, final TContext callbackContext) {
		return (this.createHttpgQueueConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final Configuration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final Class<? extends KvStoreConnectorCallback<TContext, TValue, TExtra>> callbackClass) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final Configuration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final Class<? extends KvStoreConnectorCallback<TContext, TValue, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final Configuration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, callback, (TContext) this));
	}
	
	public <TContext, TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final Configuration configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (KvStoreConnectorFactory.class).create ((configuration != null) ? configuration : PropertyTypeConfiguration.createEmpty (), valueClass, valueEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final String configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final Class<? extends KvStoreConnectorCallback<TContext, TValue, TExtra>> callbackClass) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final String configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final Class<? extends KvStoreConnectorCallback<TContext, TValue, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createKvStoreConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, valueClass, valueEncoder, callbackClass, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final String configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, callback, (TContext) this));
	}
	
	public <TContext, TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final String configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext callbackContext) {
		return (this.createKvStoreConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, valueClass, valueEncoder, callback, callbackContext));
	}
	
	public CallbackCompletion<Void> destroyConnectors (final Connector ... connectors) {
		Preconditions.checkNotNull (connectors);
		@SuppressWarnings ("unchecked") final CallbackCompletion<Void>[] completions = new CallbackCompletion[connectors.length];
		for (int index = 0; index < connectors.length; index++) {
			final Connector connector = connectors[index];
			Preconditions.checkNotNull (connector);
			completions[index] = connector.destroy ();
		}
		return (this.chain (completions));
	}
	
	public Configuration getConfiguration () {
		return (this.cloudlet.getConfiguration ());
	}
	
	public <Factory extends ConnectorFactory<?>> Factory getConnectorFactory (final Class<Factory> factory) {
		return (this.cloudlet.getConnectorFactory (factory));
	}
	
	public CallbackCompletion<Void> initializeConnectors (final Connector ... connectors) {
		Preconditions.checkNotNull (connectors);
		@SuppressWarnings ("unchecked") final CallbackCompletion<Void>[] completions = new CallbackCompletion[connectors.length];
		for (int index = 0; index < connectors.length; index++) {
			final Connector connector = connectors[index];
			Preconditions.checkNotNull (connector);
			completions[index] = connector.initialize ();
		}
		return (this.chain (completions));
	}
	
	public Configuration spliceConfiguration (final String relative) {
		return (this.getConfiguration ().spliceConfiguration (ConfigurationIdentifier.resolveRelative (relative)));
	}
}
