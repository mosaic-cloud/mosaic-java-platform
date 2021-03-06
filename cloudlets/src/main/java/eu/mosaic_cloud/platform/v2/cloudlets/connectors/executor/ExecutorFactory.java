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

package eu.mosaic_cloud.platform.v2.cloudlets.connectors.executor;


import eu.mosaic_cloud.platform.v2.cloudlets.connectors.core.ConnectorFactory;
import eu.mosaic_cloud.platform.v2.configuration.Configuration;


public interface ExecutorFactory
			extends
				ConnectorFactory<Executor<?, ?>>
{
	public abstract <TContext extends Object, TOutcome extends Object, TExtra extends Object> Executor<TOutcome, TExtra> create (final Configuration configuration, ExecutorCallback<TContext, TOutcome, TExtra> callback, TContext callbackContext);
}
