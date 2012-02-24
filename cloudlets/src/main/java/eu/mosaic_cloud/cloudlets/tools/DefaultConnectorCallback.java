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

import eu.mosaic_cloud.cloudlets.connectors.core.IConnectorCallback;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

/**
 * Default resource accessor callback.
 * 
 * @author Georgiana Macariu
 * 
 * @param <C>
 *            the type of the context of the cloudlet using this callback
 */
public class DefaultConnectorCallback<C> extends DefaultCallback<C> implements
        IConnectorCallback<C> {

    @Override
    public CallbackCompletion<Void> destroyFailed(final C context,
            final CallbackArguments<C> arguments) {
        return this.handleUnhandledCallback(arguments, "Resource Destroy Failed", false, false);
    }

    @Override
    public CallbackCompletion<Void> destroySucceeded(final C context,
            final CallbackArguments<C> arguments) {
        return this.handleUnhandledCallback(arguments, "Resource Destroy Succeeded", true, false);
    }

    @Override
    public CallbackCompletion<Void> initializeFailed(final C context,
            final CallbackArguments<C> arguments) {
        return this.handleUnhandledCallback(arguments, "Resource Initialize Failed", false, true);
    }

    @Override
    public CallbackCompletion<Void> initializeSucceeded(final C context,
            final CallbackArguments<C> arguments) {
        return this.handleUnhandledCallback(arguments, "Resource Initialize Succeeded", true, true);
    }
}
