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


/**
 * Base cloudlet-level accessor for key value storages. Cloudlets will use an
 * object of this type to get access to a key-value storage.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using the accessor
 */
public class KvStoreConnector<CB extends eu.mosaic_cloud.connectors.kvstore.IKvStoreConnector<D>, CC extends IKvStoreConnectorCallback<C, D, E>, C, D, E>
		extends BaseConnector<CB, CC, C>
		implements
			IKvStoreConnector<C, D, E>
{
	/**
	 * Creates a new accessor.
	 * 
	 * @param config
	 *            configuration data required by the accessor
	 * @param cloudlet
	 *            the cloudlet controller of the cloudlet using the accessor
	 * @param encoder
	 *            encoder used for serializing data
	 */
	public KvStoreConnector (final ICloudletController<?> cloudlet, final CB connector, final IConfiguration config, final CC callback, final C context)
	{
		super (cloudlet, connector, config, callback, context);
	}
	
	@Override
	public CallbackCompletion<Boolean> delete (final String key)
	{
		return (this.delete (key, null));
	}
	
	@Override
	public CallbackCompletion<Boolean> delete (final String key, final E extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.delete (key);
		if (this.callback != null)
			completion.observe (new CallbackCompletionObserver () {
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					if (completion.getException () != null)
						return KvStoreConnector.this.callback.deleteFailed (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, D, E> (KvStoreConnector.this.cloudlet, key, (D) completion.getException (), extra));
					return KvStoreConnector.this.callback.deleteSucceeded (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, D, E> (KvStoreConnector.this.cloudlet, key, null, extra));
				}
			});
		return (completion);
	}
	
	@Override
	public CallbackCompletion<D> get (final String key)
	{
		return (this.get (key, null));
	}
	
	@Override
	public CallbackCompletion<D> get (final String key, final E extra)
	{
		final CallbackCompletion<D> completion = this.connector.get (key);
		if (this.callback != null)
			completion.observe (new CallbackCompletionObserver () {
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					if (completion.getException () != null)
						return KvStoreConnector.this.callback.getFailed (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, D, E> (KvStoreConnector.this.cloudlet, key, (D) completion.getException (), extra));
					return KvStoreConnector.this.callback.getSucceeded (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, D, E> (KvStoreConnector.this.cloudlet, key, completion.getOutcome (), extra));
				}
			});
		return (completion);
	}
	
	@Override
	public CallbackCompletion<List<String>> list ()
	{
		return (this.list (null));
	}
	
	@Override
	public CallbackCompletion<List<String>> list (final E extra)
	{
		final CallbackCompletion<List<String>> completion = this.connector.list ();
		if (this.callback != null)
			completion.observe (new CallbackCompletionObserver () {
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					if (completion.getException () != null)
						return KvStoreConnector.this.callback.listFailed (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, List<String>, E> (KvStoreConnector.this.cloudlet, null, (List<String>) completion.getException (), extra));
					return KvStoreConnector.this.callback.listSucceeded (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, List<String>, E> (KvStoreConnector.this.cloudlet, null, completion.getOutcome (), extra));
				}
			});
		return (completion);
	}
	
	@Override
	public CallbackCompletion<Boolean> set (final String key, final D value)
	{
		return (this.set (key, value, null));
	}
	
	@Override
	public CallbackCompletion<Boolean> set (final String key, final D value, final E extra)
	{
		final CallbackCompletion<Boolean> completion = this.connector.set (key, value);
		if (this.callback != null)
			completion.observe (new CallbackCompletionObserver () {
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					if (completion.getException () != null)
						return KvStoreConnector.this.callback.setFailed (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, D, E> (KvStoreConnector.this.cloudlet, key, (D) completion.getException (), extra));
					return KvStoreConnector.this.callback.setSucceeded (KvStoreConnector.this.context, new KvStoreCallbackCompletionArguments<C, D, E> (KvStoreConnector.this.cloudlet, key, value, extra));
				}
			});
		return (completion);
	}
}
