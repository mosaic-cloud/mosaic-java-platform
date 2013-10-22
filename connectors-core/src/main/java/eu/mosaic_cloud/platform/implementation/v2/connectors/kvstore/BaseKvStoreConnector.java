/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.platform.implementation.v2.connectors.kvstore;


import eu.mosaic_cloud.platform.implementation.v2.connectors.core.BaseConnector;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorConfiguration;
import eu.mosaic_cloud.platform.v2.connectors.kvstore.KvStoreConnector;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionTrigger;


/**
 * Basic key-value store connector. This connector implements only the operations common to most of the key-value store systems.
 * 
 * @author Georgiana Macariu
 * @param <TValue>
 *            type of stored data
 * @param <TProxy>
 *            type of connector proxy
 */
public abstract class BaseKvStoreConnector<TValue extends Object, TProxy extends BaseKvStoreConnectorProxy<TValue>>
			extends BaseConnector<TProxy>
			implements
				KvStoreConnector<TValue>
{
	protected BaseKvStoreConnector (final TProxy proxy, final ConnectorConfiguration configuration) {
		super (proxy, configuration);
	}
	
	@Override
	public CallbackCompletion<Void> delete (final String key) {
		return (this.enqueueOperation (new Operation<Void> () {
			@Override
			public final CallbackCompletion<Void> execute (final CallbackCompletionTrigger<Void> trigger) {
				return (BaseKvStoreConnector.this.proxy.delete (key, trigger));
			}
		}));
	}
	
	@Override
	public CallbackCompletion<TValue> get (final String key) {
		return (this.enqueueOperation (new Operation<TValue> () {
			@Override
			public final CallbackCompletion<TValue> execute (final CallbackCompletionTrigger<TValue> trigger) {
				return (BaseKvStoreConnector.this.proxy.get (key, trigger));
			}
		}));
	}
	
	@Override
	public CallbackCompletion<Void> set (final String key, final TValue value) {
		return (this.enqueueOperation (new Operation<Void> () {
			@Override
			public final CallbackCompletion<Void> execute (final CallbackCompletionTrigger<Void> trigger) {
				return (BaseKvStoreConnector.this.proxy.set (key, value, trigger));
			}
		}));
	}
}
