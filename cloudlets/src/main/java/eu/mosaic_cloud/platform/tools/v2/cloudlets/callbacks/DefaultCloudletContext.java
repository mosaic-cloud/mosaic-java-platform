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

package eu.mosaic_cloud.platform.tools.v2.cloudlets.callbacks;


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.component.ComponentConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.component.ComponentConnectorCallbacks;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.component.ComponentConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.Connector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.executor.Executor;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.executor.ExecutorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.executor.ExecutorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.httpg.HttpgQueueConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.kvstore.KvStoreConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.kvstore.KvStoreConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.kvstore.KvStoreConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueueConsumerConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueuePublisherConnector;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueuePublisherConnectorCallback;
import eu.mosaic_cloud.platform.v2.cloudlets.connectors.queue.QueuePublisherConnectorFactory;
import eu.mosaic_cloud.platform.v2.cloudlets.core.Callback;
import eu.mosaic_cloud.platform.v2.cloudlets.core.CloudletController;
import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationIdentifier;
import eu.mosaic_cloud.tools.configurations.core.ConfigurationSource;
import eu.mosaic_cloud.tools.configurations.implementations.basic.EmptyConfigurationSource;
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
		return (this.createExecutor ((ConfigurationSource) null, callbackClass, (TContext) this));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final Class<? extends ExecutorCallback<TContext, TOutcome, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createExecutor ((ConfigurationSource) null, callbackClass, callbackContext));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final ConfigurationSource configuration, final Class<? extends ExecutorCallback<TContext, TOutcome, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createExecutor (configuration, this.createCallback (callbackClass), callbackContext));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final ConfigurationSource configuration, final ExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (ExecutorFactory.class).create ((configuration != null) ? configuration : EmptyConfigurationSource.defaultInstance, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final ExecutorCallback<TContext, TOutcome, TExtra> callback) {
		return (this.createExecutor ((ConfigurationSource) null, callback, (TContext) this));
	}
	
	public <TContext, TOutcome, TExtra> Executor<TOutcome, TExtra> createExecutor (final ExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext) {
		return (this.createExecutor ((ConfigurationSource) null, callback, callbackContext));
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
	public <TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final ConfigurationSource configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final Class<? extends HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>> callbackClass) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final ConfigurationSource configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final Class<? extends HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final ConfigurationSource configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback) {
		return (this.createHttpgQueueConnector (configuration, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback, (TContext) this));
	}
	
	public <TContext, TRequestBody, TResponseBody, TExtra> HttpgQueueConnector<TRequestBody, TResponseBody, TExtra> createHttpgQueueConnector (final ConfigurationSource configuration, final Class<TRequestBody> requestBodyClass, final DataEncoder<TRequestBody> requestBodyEncoder, final Class<TResponseBody> responseBodyClass, final DataEncoder<TResponseBody> responseBodyEncoder, final HttpgQueueConnectorCallback<TContext, TRequestBody, TResponseBody, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (HttpgQueueConnectorFactory.class).create ((configuration != null) ? configuration : EmptyConfigurationSource.defaultInstance, requestBodyClass, requestBodyEncoder, responseBodyClass, responseBodyEncoder, callback, callbackContext));
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
	public <TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final ConfigurationSource configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final Class<? extends KvStoreConnectorCallback<TContext, TValue, TExtra>> callbackClass) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final ConfigurationSource configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final Class<? extends KvStoreConnectorCallback<TContext, TValue, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final ConfigurationSource configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback) {
		return (this.createKvStoreConnector (configuration, valueClass, valueEncoder, callback, (TContext) this));
	}
	
	public <TContext, TValue, TExtra> KvStoreConnector<TValue, TExtra> createKvStoreConnector (final ConfigurationSource configuration, final Class<TValue> valueClass, final DataEncoder<TValue> valueEncoder, final KvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (KvStoreConnectorFactory.class).create ((configuration != null) ? configuration : EmptyConfigurationSource.defaultInstance, valueClass, valueEncoder, callback, callbackContext));
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
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createQueueConsumerConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createQueueConsumerConnector (configuration, messageClass, messageEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createQueueConsumerConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (QueueConsumerConnectorFactory.class).create ((configuration != null) ? configuration : EmptyConfigurationSource.defaultInstance, messageClass, messageEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createQueueConsumerConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueueConsumerConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createQueueConsumerConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callbackClass, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createQueueConsumerConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueueConsumerConnector<TMessage, TExtra> createQueueConsumerConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueueConsumerConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.createQueueConsumerConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createQueuePublisherConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createQueuePublisherConnector (configuration, messageClass, messageEncoder, this.createCallback (callbackClass), callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createQueuePublisherConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final ConfigurationSource configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.getConnectorFactory (QueuePublisherConnectorFactory.class).create ((configuration != null) ? configuration : EmptyConfigurationSource.defaultInstance, messageClass, messageEncoder, callback, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass) {
		return (this.createQueuePublisherConnector (configuration, messageClass, messageEncoder, callbackClass, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final Class<? extends QueuePublisherConnectorCallback<TContext, TMessage, TExtra>> callbackClass, final TContext callbackContext) {
		return (this.createQueuePublisherConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callbackClass, callbackContext));
	}
	
	@SuppressWarnings ("unchecked")
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback) {
		return (this.createQueuePublisherConnector (configuration, messageClass, messageEncoder, callback, (TContext) this));
	}
	
	public <TContext, TMessage, TExtra> QueuePublisherConnector<TMessage, TExtra> createQueuePublisherConnector (final String configuration, final Class<TMessage> messageClass, final DataEncoder<TMessage> messageEncoder, final QueuePublisherConnectorCallback<TContext, TMessage, TExtra> callback, final TContext callbackContext) {
		return (this.createQueuePublisherConnector ((configuration != null) ? this.spliceConfiguration (configuration) : null, messageClass, messageEncoder, callback, callbackContext));
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
	
	public ConfigurationSource getConfiguration () {
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
	
	public ConfigurationSource spliceConfiguration (final String relative) {
		return (this.getConfiguration ().splice (ConfigurationIdentifier.resolveRelative (relative)));
	}
}
