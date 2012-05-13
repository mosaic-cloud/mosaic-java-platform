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

package eu.mosaic_cloud.cloudlets.connectors.kvstore.memcache;


import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.BaseKvStoreConnector;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KvStoreCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;


/**
 * Cloudlet-level connector for memcached-based key value storages. Cloudlets
 * will use an object of this type to get access to a memcached storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <TContext>
 *            connector callback context type
 * @param <TValue>
 *            the type of the values exchanged with the key-value store using
 *            this connector
 * @param <TExtra>
 *            the type of the extra data; as an example, this data can be used
 *            correlation
 */
public class MemcacheKvStoreConnector<TContext, TValue, TExtra>
		extends BaseKvStoreConnector<eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue>, IMemcacheKvStoreConnectorCallback<TContext, TValue, TExtra>, TContext, TValue, TExtra>
		implements
			IMemcacheKvStoreConnector<TValue, TExtra>
{
	/**
	 * Creates a new connector.
	 * 
	 * @param config
	 *            configuration data required by the connector
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the connector
	 */
	public MemcacheKvStoreConnector (final ICloudletController<?> cloudlet, final eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector<TValue> connector, final IConfiguration config, final IMemcacheKvStoreConnectorCallback<TContext, TValue, TExtra> callback, final TContext context)
	{
		super (cloudlet, connector, config, callback, context);
	}
	
	@Override
	public CallbackCompletion<Boolean> add (final String key, final int exp, final TValue data)
	{
		return this.add (key, data, exp, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> add (final String key, final TValue value, final int exp, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.add (key, exp, value);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> aCompletion)
				{
					assert (aCompletion == completion);
					CallbackCompletion<Void> resultCompletion;
					if (completion.getException () == null) {
						resultCompletion = MemcacheKvStoreConnector.this.callback.addSucceeded (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
					} else {
						resultCompletion = MemcacheKvStoreConnector.this.callback.addFailed (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return resultCompletion;
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Boolean> append (final String key, final TValue data)
	{
		return this.append (key, data, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> append (final String key, final TValue value, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.append (key, value);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> aCompletion)
				{
					assert (aCompletion == completion);
					CallbackCompletion<Void> resultCompletion;
					if (completion.getException () == null) {
						resultCompletion = MemcacheKvStoreConnector.this.callback.appendSucceeded (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
					} else {
						resultCompletion = MemcacheKvStoreConnector.this.callback.appendFailed (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return resultCompletion;
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Boolean> cas (final String key, final TValue data)
	{
		return this.cas (key, data, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> cas (final String key, final TValue value, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.cas (key, value);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> aCompletion)
				{
					assert (aCompletion == completion);
					CallbackCompletion<Void> resultCompletion;
					if (completion.getException () == null) {
						resultCompletion = MemcacheKvStoreConnector.this.callback.casSucceeded (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
					} else {
						resultCompletion = MemcacheKvStoreConnector.this.callback.casFailed (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return resultCompletion;
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Map<String, TValue>> getBulk (final List<String> keys)
	{
		return this.getBulk (keys, null);
	}
	
	@Override
	public CallbackCompletion<Map<String, TValue>> getBulk (final List<String> keys, final TExtra extra)
	{
		final CallbackCompletion<Map<String, TValue>> completion = this.connector.getBulk (keys);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> aCompletion)
				{
					assert (aCompletion == completion);
					CallbackCompletion<Void> resultCompletion;
					if (completion.getException () == null) {
						resultCompletion = MemcacheKvStoreConnector.this.callback.getBulkSucceeded (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<Map<String, TValue>, TExtra> (MemcacheKvStoreConnector.this.cloudlet, keys, completion.getOutcome (), extra));
					} else {
						resultCompletion = MemcacheKvStoreConnector.this.callback.getBulkFailed (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<Map<String, TValue>, TExtra> (MemcacheKvStoreConnector.this.cloudlet, keys, completion.getException (), extra));
					}
					return resultCompletion;
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Boolean> prepend (final String key, final TValue data)
	{
		return this.prepend (key, data, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> prepend (final String key, final TValue value, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.prepend (key, value);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> aCompletion)
				{
					assert (aCompletion == completion);
					CallbackCompletion<Void> resultCompletion;
					if (completion.getException () == null) {
						resultCompletion = MemcacheKvStoreConnector.this.callback.prependSucceeded (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
					} else {
						resultCompletion = MemcacheKvStoreConnector.this.callback.prependFailed (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return resultCompletion;
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Boolean> replace (final String key, final int exp, final TValue data)
	{
		return this.replace (key, data, exp, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> replace (final String key, final TValue value, final int exp, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.replace (key, exp, value);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> aCompletion)
				{
					assert (aCompletion == completion);
					CallbackCompletion<Void> resultCompletion;
					if (completion.getException () == null) {
						resultCompletion = MemcacheKvStoreConnector.this.callback.replaceSucceeded (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
					} else {
						resultCompletion = MemcacheKvStoreConnector.this.callback.replaceFailed (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return resultCompletion;
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Boolean> set (final String key, final int exp, final TValue data)
	{
		return this.set (key, data, exp, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> set (final String key, final TValue value, final int exp, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.set (key, exp, value);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> aCompletion)
				{
					assert (aCompletion == completion);
					CallbackCompletion<Void> resultCompletion;
					if (completion.getException () == null) {
						resultCompletion = MemcacheKvStoreConnector.this.callback.setSucceeded (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, value, extra));
					} else {
						resultCompletion = MemcacheKvStoreConnector.this.callback.setFailed (MemcacheKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (MemcacheKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return resultCompletion;
				}
			});
		}
		return completion;
	}
}
