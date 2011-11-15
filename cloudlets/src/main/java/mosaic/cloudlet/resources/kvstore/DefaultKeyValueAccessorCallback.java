/*
 * #%L
 * mosaic-cloudlet
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.cloudlet.resources.kvstore;

import mosaic.cloudlet.resources.DefaultResourceAccessorCallback;

/**
 * Default key-value storage calback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet using this callback
 */
public class DefaultKeyValueAccessorCallback<S> extends
		DefaultResourceAccessorCallback<S> implements
		IKeyValueAccessorCallback<S> {

	@Override
	public void setSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Set Succeeded", true, false);

	}

	@Override
	public void setFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Set Failed", false, false);
	}

	@Override
	public void getSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Get Succeeded", true, false);

	}

	@Override
	public void getFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Get Failed", false, false);
	}

	@Override
	public void deleteSucceeded(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Delete Succeeded", true, false);

	}

	@Override
	public void deleteFailed(S state, KeyValueCallbackArguments<S> arguments) {
		this.handleUnhandledCallback(arguments, "Delete Failed", false, false);
	}

}
