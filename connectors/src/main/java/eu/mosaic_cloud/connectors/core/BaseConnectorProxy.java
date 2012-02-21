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

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;

/**
 * Base class for connector proxys.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class BaseConnectorProxy implements SessionCallbacks {

    protected final IConfiguration configuration;
    protected MosaicLogger logger;
    protected final ResponseHandlerMap pendingRequests;
    private final Channel channel;
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
    protected BaseConnectorProxy(final IConfiguration configuration, final Channel channel) {
        super();
        Preconditions.checkNotNull(configuration);
        Preconditions.checkNotNull(channel);
        this.configuration = configuration;
        this.channel = channel;
        this.identifier = UUID.randomUUID().toString();
        this.pendingRequests = new ResponseHandlerMap();
        this.logger = MosaicLogger.createLogger(this);
    }

    /**
     * Returns the configuration of the connector's proxy.
     * 
     * @return the configuration of the connector's proxy
     */
    public IConfiguration getConfiguration() {
        return this.configuration;
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
     * Destroys the proxy, freeing up any allocated resources.
     */
    public CallbackCompletion<Void> destroy() {
        ((ZeroMqChannel) this.channel).terminate();
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

    /**
     * Called when some operation on session failed.
     * 
     * @param session
     *            the session
     * @param exception
     *            the exception
     */
    @Override
    public CallbackCompletion<Void> failed(final Session session, final Throwable exception) {
        Preconditions.checkState(this.session == session);
        return CallbackCompletion.createOutcome();
    }

    @Override
    public CallbackCompletion<Void> received(final Session session, final Message message) {
        this.logger.debug("ConnectorProxy - Received " + message.specification.toString() + "...");
        this.processResponse(message);
        return CallbackCompletion.createOutcome();
    }

    protected void connect(final String driverIdentifier, final SessionSpecification session,
            final Message initMessage) {
        this.channel.connect(driverIdentifier, session, initMessage, this);
    }

    protected CompletionToken generateToken() {
        final CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder();
        tokenBuilder.setMessageId(UUID.randomUUID().toString());
        tokenBuilder.setClientId(this.identifier);
        return tokenBuilder.build();
    }

    /**
     * Process a received response for a previous submitted request.
     * 
     * @param message
     *            the contents of the received message
     */
    protected abstract void processResponse(Message message);

    /**
     * Sends a request to the driver.
     * 
     * @param session
     *            the session to which the request belongs
     * @param request
     *            the request
     */
    protected void send(final Message request) {
        this.session.send(request);
    }

    protected <O extends Object> CallbackCompletion<O> sendRequest(final Message message,
            final CompletionToken token, final Class<O> outcomeClass) {
        final CallbackCompletionDeferredFuture<O> future = CallbackCompletionDeferredFuture
                .create(outcomeClass);
        this.pendingRequests.register(token.getMessageId(), future);
        this.logger.debug("ConnectorProxy - Sending " + message.specification.toString()
                + " request [" + token.getMessageId() + "]...");
        this.send(message);
        return future.completion;
    }

}
