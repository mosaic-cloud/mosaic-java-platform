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
package eu.mosaic_cloud.cloudlets.tools;

import eu.mosaic_cloud.cloudlets.connectors.kvstore.IMemcacheKvStoreConnectorCallback;
import eu.mosaic_cloud.cloudlets.connectors.kvstore.KeyValueCallbackArguments;

/**
 * Default memcached key-value storage calback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this callback
 */
public class DefaultMemcacheKvStoreConnectorCallback<C> extends
		DefaultKvStoreConnectorCallback<C> implements
		IMemcacheKvStoreConnectorCallback<C> {

	@Override
	public void addSucceeded(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Add Succeeded", true, false);

	}

	@Override
	public void addFailed(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Add Failed", false, false);

	}

	@Override
	public void appendSucceeded(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Append Succeeded", true, false);

	}

	@Override
	public void appendFailed(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Append Failed", false, false);

	}

	@Override
	public void prependSucceeded(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Prepend Succeeded", true,
				false);

	}

	@Override
	public void prependFailed(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Prepend Failed", false, false);

	}

	@Override
	public void replaceSucceeded(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Replace Succeeded", true,
				false);

	}

	@Override
	public void replaceFailed(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Replace Failed", false, false);

	}

	@Override
	public void getBulkSucceeded(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "GetBulk Succeeded", true,
				false);

	}

	@Override
	public void getBulkFailed(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "GetBulk Failed", false, false);

	}

	@Override
	public void casSucceeded(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Cas Succeeded", true, false);

	}

	@Override
	public void casFailed(C context, KeyValueCallbackArguments<C> arguments) {
		this.handleUnhandledCallback(arguments, "Cas Failed", false, false);

	}

}
