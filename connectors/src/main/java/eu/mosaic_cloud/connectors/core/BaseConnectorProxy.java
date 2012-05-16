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

import eu.mosaic_cloud.connectors.tools.ConnectorConfiguration;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


/**
 * Base class for connector proxys.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class BaseConnectorProxy
		implements
			SessionCallbacks,
			IConnector
{
	/**
	 * Creates a proxy for a resource.
	 * 
	 * @param channel
	 *            the channel on which to communicate with the driver
	 */
	protected BaseConnectorProxy (final ConnectorConfiguration configuration)
	{
		super ();
		Preconditions.checkNotNull (configuration);
		this.configuration = configuration;
		// FIXME: the channel acquisition should be made as part of the channel
		//-- endpoint resolution
		this.channel = this.configuration.getCommunicationChannel ();
		this.identifier = UUID.randomUUID ().toString ();
		this.transcript = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, FallbackExceptionTracer.defaultInstance);
		this.pendingRequests = new ResponseHandlerMap (this.transcript);
	}
	
	/**
	 * Called after session was created.
	 * 
	 * @param session
	 *            the session
	 */
	@Override
	public CallbackCompletion<Void> created (final Session session)
	{
		Preconditions.checkState (this.session == null);
		this.session = session;
		this.transcript.traceDebugging ("created the interoperability session `%{object:identity}`...", this.session);
		return (CallbackCompletion.createOutcome ());
	}
	
	/**
	 * Called after session was destroyed
	 * 
	 * @param session
	 *            the session
	 */
	@Override
	public CallbackCompletion<Void> destroyed (final Session session)
	{
		Preconditions.checkState (this.session == session);
		this.transcript.traceDebugging ("destroyed the interoperability session `%{object:identity}`...", this.session);
		this.pendingRequests.cancelAll ();
		return (CallbackCompletion.createOutcome ());
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
	public CallbackCompletion<Void> failed (final Session session, final Throwable exception)
	{
		Preconditions.checkState (this.session == session);
		this.transcript.traceWarning ("failed the interoperability session `%{object:identity}`...", this.session);
		return (CallbackCompletion.createOutcome ());
	}
	
	public String getIdentifier ()
	{
		return (this.identifier);
	}
	
	@Override
	public CallbackCompletion<Void> received (final Session session, final Message message)
	{
		Preconditions.checkState (this.session == session);
		this.transcript.traceDebugging ("received the interoperability message of type `%s`...", message.specification);
		this.processResponse (message);
		return (CallbackCompletion.createOutcome ());
	}
	
	protected CallbackCompletion<Void> connect (final SessionSpecification session, final Message initMessage)
	{
		Preconditions.checkNotNull (session);
		this.transcript.traceDebugging ("creating and connecting the interoperability session of type `%s`...", session);
		final String driverEndpoint = this.configuration.getConfigParameter (ConfigProperties.getString ("GenericConnector.0"), String.class, null);
		final String driverIdentity = this.configuration.getConfigParameter (ConfigProperties.getString ("GenericConnector.1"), String.class, null);
		final String driverTarget = this.configuration.getConfigParameter (ConfigProperties.getString ("GenericConnector.2"), String.class, this.getDefaultDriverGroup ());
		CallbackCompletion<Void> result;
		this.channel.register (session);
		if ((driverEndpoint != null) && (driverIdentity != null)) {
			this.transcript.traceDebugging ("using the driver endpoint `%s`...", driverEndpoint);
			this.transcript.traceDebugging ("using the driver identity `%s`...", driverIdentity);
			// FIXME: The connection operation should be done by the channel
			//-- resolver.
			((ZeroMqChannel) this.channel).connect (driverEndpoint);
			this.channel.connect (driverIdentity, session, initMessage, this);
			result = CallbackCompletion.createOutcome ();
		} else {
			this.transcript.traceDebugging ("resolving the driver endpoint and identity for target `%s`...", driverTarget);
			final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture.create (Void.class);
			final ResolverCallbacks resolverCallbacks = new ResolverCallbacks () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> resolved (final ChannelResolver resolver, final String target, final String peer, final String endpoint)
				{
					Preconditions.checkState (driverTarget.equals (target));
					Preconditions.checkState (peer != null);
					Preconditions.checkState (endpoint != null);
					BaseConnectorProxy.this.transcript.traceDebugging ("resolved the driver endpoint and identity for target `%s` successfully.", driverTarget);
					BaseConnectorProxy.this.transcript.traceDebugging ("using the driver endpoint `%s`...", endpoint);
					BaseConnectorProxy.this.transcript.traceDebugging ("using the driver identity `%s`...", peer);
					// FIXME: The connection operation should be done by the
					//-- channel resolver.
					BaseConnectorProxy.this.transcript.traceDebugging ("registering the interoperability endpoint...");
					((ZeroMqChannel) BaseConnectorProxy.this.channel).connect (endpoint);
					BaseConnectorProxy.this.transcript.traceDebugging ("creating the interoperability session...");
					BaseConnectorProxy.this.channel.connect (peer, session, initMessage, BaseConnectorProxy.this);
					// FIXME: Calling `connect` is not enough; the connection is
					//-- successful only after the call of `created(Session)` was
					//-- done.
					future.trigger.triggerSucceeded (null);
					return (CallbackCompletion.createOutcome ());
				}
			};
			this.configuration.resolveChannel (driverTarget, resolverCallbacks);
			result = future.completion;
		}
		return (result);
	}
	
	protected CallbackCompletion<Void> disconnect (final Message finalMessage)
	{
		// FIXME: The disconnection should push the termination also to the
		//-- interoperability layer.
		//-- Currently the driver side has no idea that the connector was
		//-- disconnected except if the `finalMessage` contains such information.
		// FIXME: The `finalMessage` should always exist and should always be an
		//-- "terminal" one.
		if (finalMessage != null) {
			this.sendMessage (finalMessage);
		}
		this.transcript.traceDebugging ("destroying the interoperability session...");
		return (CallbackCompletion.createOutcome ());
	}
	
	protected CompletionToken generateToken ()
	{
		final CompletionToken.Builder tokenBuilder = CompletionToken.newBuilder ();
		tokenBuilder.setMessageId (UUID.randomUUID ().toString ());
		tokenBuilder.setClientId (this.identifier);
		return (tokenBuilder.build ());
	}
	
	protected abstract String getDefaultDriverGroup ();
	
	/**
	 * Process a received response for a previous submitted request.
	 * 
	 * @param message
	 *            the contents of the received message
	 */
	protected abstract void processResponse (Message message);
	
	/**
	 * Sends a request to the driver.
	 * 
	 * @param session
	 *            the session to which the request belongs
	 * @param request
	 *            the request
	 */
	protected void sendMessage (final Message message)
	{
		// FIXME: Currently this is a hack to avoid a race condition introduced
		//-- by the `connect` code above.
		//-- For now we just busy-wait until the session object is available
		if (this.session == null) {
			this.transcript.traceDebugging ("waiting for the interoperability session...");
			while (this.session == null) {
				Thread.yield ();
			}
		}
		this.transcript.traceDebugging ("sending the interoperability message of type `%s`...", message.specification);
		this.session.send (message);
	}
	
	protected void sendRequest (final Message message)
	{
		this.transcript.traceDebugging ("sending the asynchronous request with specification `%s`...", message.specification);
		this.sendMessage (message);
	}
	
	protected <TOutcome extends Object> CallbackCompletion<TOutcome> sendRequest (final Message message, final CompletionToken token, final Class<TOutcome> outcomeClass)
	{
		this.transcript.traceDebugging ("registering and sending the pending request with specification `%s` and token `%s`...", message.specification, token.getMessageId ());
		final CallbackCompletionDeferredFuture<TOutcome> future = CallbackCompletionDeferredFuture.create (outcomeClass);
		this.pendingRequests.register (token.getMessageId (), future);
		this.sendMessage (message);
		return (future.completion);
	}
	
	protected final ConnectorConfiguration configuration;
	protected final TranscriptExceptionTracer exceptions;
	protected final ResponseHandlerMap pendingRequests;
	protected final Transcript transcript;
	private final Channel channel;
	private final String identifier;
	private Session session;
}
