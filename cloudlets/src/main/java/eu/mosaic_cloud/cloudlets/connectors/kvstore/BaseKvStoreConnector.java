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

package eu.mosaic_cloud.cloudlets.connectors.kvstore;


import java.util.List;

import eu.mosaic_cloud.cloudlets.connectors.core.BaseConnector;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;


public abstract class BaseKvStoreConnector<TConnector extends eu.mosaic_cloud.connectors.kvstore.IKvStoreConnector<TValue>, TCallback extends IKvStoreConnectorCallback<TContext, TValue, TExtra>, TContext, TValue, TExtra>
		// NOPMD
		extends BaseConnector<TConnector, TCallback, TContext>
		implements
			IKvStoreConnector<TValue, TExtra>
{
	protected BaseKvStoreConnector (final ICloudletController<?> cloudlet, final TConnector connector, final IConfiguration config, final TCallback callback, final TContext context)
	{
		super (cloudlet, connector, config, callback, context);
	}
	
	@Override
	public CallbackCompletion<Boolean> delete (final String key)
	{
		return this.delete (key, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> delete (final String key, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.delete (key);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				// NOPMD
						@Override
						public
						CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion); // NOPMD
					if (completion.getException () != null) {
						return BaseKvStoreConnector.this.callback.deleteFailed (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (BaseKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return BaseKvStoreConnector.this.callback.deleteSucceeded (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (BaseKvStoreConnector.this.cloudlet, key, (TValue) null, extra));
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<TValue> get (final String key)
	{
		return this.get (key, null);
	}
	
	@Override
	public CallbackCompletion<TValue> get (final String key, final TExtra extra)
	{
		final CallbackCompletion<TValue> completion = this.connector.get (key);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion); // NOPMD
					if (completion.getException () != null) {
						return BaseKvStoreConnector.this.callback.getFailed (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (BaseKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return BaseKvStoreConnector.this.callback.getSucceeded (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (BaseKvStoreConnector.this.cloudlet, key, completion.getOutcome (), extra));
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<List<String>> list ()
	{
		return this.list (null);
	}
	
	@Override
	public CallbackCompletion<List<String>> list (final TExtra extra)
	{
		final CallbackCompletion<List<String>> completion = this.connector.list ();
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion); // NOPMD
					if (completion.getException () != null) {
						return BaseKvStoreConnector.this.callback.listFailed (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<List<String>, TExtra> (BaseKvStoreConnector.this.cloudlet, (String) null, completion.getException (), extra));
					}
					return BaseKvStoreConnector.this.callback.listSucceeded (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<List<String>, TExtra> (BaseKvStoreConnector.this.cloudlet, (String) null, completion.getOutcome (), extra));
				}
			});
		}
		return completion;
	}
	
	@Override
	public CallbackCompletion<Boolean> set (final String key, final TValue value)
	{
		return this.set (key, value, null);
	}
	
	@Override
	public CallbackCompletion<Boolean> set (final String key, final TValue value, final TExtra extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.set (key, value);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion); // NOPMD
					if (completion.getException () != null) {
						return BaseKvStoreConnector.this.callback.setFailed (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (BaseKvStoreConnector.this.cloudlet, key, completion.getException (), extra));
					}
					return BaseKvStoreConnector.this.callback.setSucceeded (BaseKvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<TValue, TExtra> (BaseKvStoreConnector.this.cloudlet, key, value, extra));
				}
			});
		}
		return completion;
	}
}
