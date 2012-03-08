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

package eu.mosaic_cloud.connectors.core;

import java.util.UUID;

import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;

import com.google.common.base.Preconditions;

/**
 * Base class for connector proxys.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class BaseConnectorProxy implements SessionCallbacks,
        IConnector {

    private final IConfiguration configuration;
    protected MosaicLogger logger;
    protected final ResponseHandlerMap pendingRequests;
    protected final Channel channel;
    private final ConnectorEnvironment environment;
    private final String identifier;
    private Session session;

    /**
     * Creates a proxy for a resource.
     * 
     * @param configuration
     *            the configurations required to initialize the proxy
     * @param channel
     *            the channel on which to communicate with the driver
     */
    protected BaseConnectorProxy(final IConfiguration configuration,
            final ConnectorEnvironment environment) {
        super();
        Preconditions.checkNotNull(configuration);
        Preconditions.checkNotNull(environment);
        this.configuration = configuration;
        this.environment = environment;
        // FIXME: the channel acquisition should be made as part of the channel
        // endpoint resolution
        this.channel = this.environment.getCommunicationChannel();
        this.logger = MosaicLogger.createLogger(this);
        this.identifier = UUID.randomUUID().toString();
        this.pendingRequests = new ResponseHandlerMap();
    }

    protected CallbackCompletion<Void> connect(
            final SessionSpecification session, final Message initMessage) {
        final String driverEndpoint = ConfigUtils.resolveParameter(
                this.configuration,
                ConfigProperties.getString("GenericConnector.0"), String.class,
                null);
        final String driverIdentity = ConfigUtils.resolveParameter(
                this.configuration,
                ConfigProperties.getString("GenericConnector.1"), String.class,
                null);
        final String driverTarget = ConfigUtils.resolveParameter(
                this.configuration,
                ConfigProperties.getString("GenericConnector.2"), String.class,
                null);

        final CallbackCompletion<Void> result;
        this.channel.register(session);
        if ((driverEndpoint != null) && (driverIdentity != null)) {
            ((ZeroMqChannel) this.channel).connect(driverEndpoint);
            this.channel.connect(driverIdentity, session, initMessage, this);
            result = CallbackCompletion.createOutcome();
        } else {
            final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture
                    .create(Void.class);
            ResolverCallbacks resolverCallbacks=new ResolverCallbacks() {

                @Override
                public CallbackCompletion<Void> resolved(
                        ChannelResolver resolver, String target,
                        String peer, String endpoint) {
                    Preconditions.checkState(driverTarget
                            .equals(target));
                    Preconditions.checkState(peer != null);
                    Preconditions.checkState(endpoint != null);
                    // FIXME: The connection operation should be done by the channel resolver.
                    ((ZeroMqChannel) BaseConnectorProxy.this.channel)
                            .connect(endpoint);
                    BaseConnectorProxy.this.channel.connect(peer,
                            session, initMessage,
                            BaseConnectorProxy.this);
                    // FIXME: Calling `connect` is not enough; the connection is successfull only
                    // after the call of `created(Session)` was done.
                    future.trigger.triggerSucceeded(null);
                    return CallbackCompletion.createOutcome();
                }
            };
            this.environment.resolveChannel(driverTarget,resolverCallbacks);
            result = future.completion;
        }
        return result;
    }

    /**
     * Called after session was created.
     * 
     * @param session
     *            the session
     */
    @Override
    public CallbackCompletion<Void> created(final Session session) {
        Preconditions.checkState(this.session == null);
        this.session = session;
        return CallbackCompletion.createOutcome();
    }

    /**
     * Called after session was destroyed
     * 
     * @param session
     *            the session
     */
    @Override
    public CallbackCompletion<Void> destroyed(final Session session) {
        Preconditions.checkState(this.session == session);
        this.pendingRequests.cancelAll();
        return CallbackCompletion.createOutcome();
    }

    protected CallbackCompletion<Void> disconnect(final Message finalMessage) {
        // FIXME: The disconnection should push the termination also to the interoperability layer.
        // Currently the driver side has no ideea that the connector was disconnected except if
        // the `finalMessage` contains such information.
        // FIXME: The `finalMessage` should always exist and should always be an "terminal" one.
        if (finalMessage != null) {
            this.send(finalMessage);
        }
        return CallbackCompletion.createOutcome();
    }

    /**
     * Called when some operation on session failed.
     * 
     * @param session
     *            the session
     * @param exception
     *            the exception
     */
    @Override
    public CallbackCompletion<Void> failed(final Session session,
            final Throwable exception) {
        Preconditions.checkState(this.session == session);
        return CallbackCompletion.createOutcome();
    }

    protected CompletionToken generateToken() {
        final CompletionToken.Builder tokenBuilder = CompletionToken
                .newBuilder();
        tokenBuilder.setMessageId(UUID.randomUUID().toString());
        tokenBuilder.setClientId(this.identifier);
        return tokenBuilder.build();
    }

    /**
     * Returns the configuration of the connector's proxy.
     * 
     * @return the configuration of the connector's proxy
     */
    protected IConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Process a received response for a previous submitted request.
     * 
     * @param message
     *            the contents of the received message
     */
    protected abstract void processResponse(Message message);

    @Override
    public CallbackCompletion<Void> received(final Session session,
            final Message message) {
        this.logger.debug("ConnectorProxy - Received "
                + message.specification.toString() + "...");
        this.processResponse(message);
        return CallbackCompletion.createOutcome();
    }

    /**
     * Sends a request to the driver.
     * 
     * @param session
     *            the session to which the request belongs
     * @param request
     *            the request
     */
    protected void send(final Message request) {
        // FIXME: Currently this is a hack to avoid a race condition introduced by the `connect` code above.
        // For now we just busy-wait until the session object is available
        while (this.session == null) {
            Thread.yield();
        }
        this.session.send(request);
    }

    protected <O extends Object> CallbackCompletion<O> sendRequest(
            final Message message, final CompletionToken token,
            final Class<O> outcomeClass) {
        final CallbackCompletionDeferredFuture<O> future = CallbackCompletionDeferredFuture
                .create(outcomeClass);
        this.pendingRequests.register(token.getMessageId(), future);
        this.logger.debug("ConnectorProxy - Sending "
                + message.specification.toString() + " request ["
                + token.getMessageId() + "]...");
        this.send(message);
        return future.completion;
    }
}
