/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.cloudlets.connectors.core;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.cloudlets.core.CallbackArguments;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;

public abstract class BaseConnector<Connector extends eu.mosaic_cloud.connectors.core.IConnector, Callback extends IConnectorCallback<Context>, Context extends Object>
        implements IConnector<Context>, CallbackProxy {
    protected final Callback callback;
    protected final ICloudletController<?> cloudlet;
    protected final IConfiguration configuration;
    protected final Connector connector;
    protected final Context context;
    protected final MosaicLogger logger;
    private final CallbackCompletion<Void> initializeConnectorCompletion;
    
    protected BaseConnector(final ICloudletController<?> cloudlet, final Connector connector,
            final IConfiguration configuration, final Callback callback, final Context context) {
        super();
        Preconditions.checkNotNull(cloudlet);
        Preconditions.checkNotNull(connector);
        Preconditions.checkNotNull(configuration);
        this.cloudlet = cloudlet;
        this.connector = connector;
        this.configuration = configuration;
        this.callback = callback;
        this.context = context;
        this.logger = MosaicLogger.createLogger(this);
        this.initializeConnectorCompletion = this.initializeConnector();
    }

    @Override
    public CallbackCompletion<Void> destroy() {
        final CallbackCompletion<Void> completion = this.connector.destroy();
        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {
                @Override
                public CallbackCompletion<Void> completed(final CallbackCompletion<?> completion_) {
                    assert (completion_ == completion);
                    if (completion.getException() != null) {
                        return BaseConnector.this.callback.initializeFailed(
                                BaseConnector.this.context, new CallbackArguments<Context>(
                                        BaseConnector.this.cloudlet));
                    }
                    return BaseConnector.this.callback.initializeSucceeded(
                            BaseConnector.this.context, new CallbackArguments<Context>(
                                    BaseConnector.this.cloudlet));
                }
            });
        }
        return (completion);
    }

    @Override
    @Deprecated
    public CallbackCompletion<Void> initialize() {
        return (this.initializeConnectorCompletion);
    }

    private CallbackCompletion<Void> initializeConnector() {
        final CallbackCompletion<Void> completion = this.connector.initialize();
        if (this.callback != null) {
            completion.observe(new CallbackCompletionObserver() {
                @Override
                public CallbackCompletion<Void> completed(final CallbackCompletion<?> completion_) {
                    assert (completion_ == completion);
                    if (completion.getException() != null) {
                        return BaseConnector.this.callback.destroyFailed(
                                BaseConnector.this.context, new CallbackArguments<Context>(
                                        BaseConnector.this.cloudlet));
                    }
                    return BaseConnector.this.callback.destroySucceeded(BaseConnector.this.context,
                            new CallbackArguments<Context>(BaseConnector.this.cloudlet));
                }
            });
        }
        return (completion);
    }

    
}
