/*
 * #%L
 * components-tools
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

package eu.mosaic_cloud.components.implementations.basic;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.AbstractService;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Channel;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelFlow;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.transcript.core.Transcript;
import eu.mosaic_cloud.transcript.tools.TranscriptExceptionTracer;


public final class BasicComponent
		extends Object
		implements
			eu.mosaic_cloud.components.core.Component
{
	private BasicComponent (final Channel channel, final CallbackReactor callbackReactor, final ComponentCallbacks callbacks, final ExceptionTracer exceptions)
	{
		super ();
		this.delegate = new Component (this, channel, callbackReactor, callbacks, exceptions);
	}
	
	@Override
	public final void assign (final ComponentCallbacks callbacks)
	{
		this.delegate.assign (callbacks);
	}
	
	@Override
	public final void call (final ComponentIdentifier component, final ComponentCallRequest request)
	{
		this.delegate.call (component, request);
	}
	
	@Override
	public final void cast (final ComponentIdentifier component, final ComponentCastRequest request)
	{
		this.delegate.cast (component, request);
	}
	
	public final void initialize ()
	{
		this.delegate.startAndWait ();
	}
	
	public final boolean isActive ()
	{
		return (this.delegate.isRunning ());
	}
	
	@Override
	public final void register (final ComponentIdentifier group, final ComponentCallReference reference)
	{
		this.delegate.register (group, reference);
	}
	
	@Override
	public final void reply (final ComponentCallReply reply)
	{
		this.delegate.reply (reply);
	}
	
	@Override
	public final void terminate ()
	{
		this.delegate.stop ();
	}
	
	private final Component delegate;
	
	public static final BasicComponent create (final Channel channel, final CallbackReactor reactor, final ComponentCallbacks callbacks, final ExceptionTracer exceptions)
	{
		return (new BasicComponent (channel, reactor, callbacks, exceptions));
	}
	
	public static final BasicComponent create (final Channel channel, final CallbackReactor reactor, final ExceptionTracer exceptions)
	{
		return (new BasicComponent (channel, reactor, null, exceptions));
	}
	
	private static enum Action
	{
		Call (),
		CallReturn (),
		Cast (),
		RegisterReturn ();
	}
	
	private static final class Component
			extends AbstractService
			implements
				ChannelCallbacks,
				CallbackHandler<ChannelCallbacks>
	{
		Component (final BasicComponent facade, final Channel channel, final CallbackReactor callbackReactor, final ComponentCallbacks callbacks, final ExceptionTracer exceptions)
		{
			super ();
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (channel);
			Preconditions.checkNotNull (callbackReactor);
			this.facade = facade;
			this.monitor = Monitor.create (this.facade);
			synchronized (this.monitor) {
				this.transcript = Transcript.create (this.facade);
				this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
				this.channel = channel;
				this.channel.assign (this);
				this.callbackReactor = callbackReactor;
				this.callbackTrigger = this.callbackReactor.register (ComponentCallbacks.class, callbacks);
				this.inboundCalls = HashBiMap.create ();
				this.outboundCalls = HashBiMap.create ();
				this.registers = HashBiMap.create ();
			}
		}
		
		@Override
		public final CallbackReference closed (final Channel channel, final ChannelFlow flow)
		{
			Preconditions.checkState (channel == this.channel);
			synchronized (this.monitor) {
				if (flow == ChannelFlow.Inbound) {
					this.callbackTrigger.terminated (this.facade);
					this.stop ();
				}
			}
			return (null);
		}
		
		@Override
		public final void deassigned (final ChannelCallbacks trigger, final ChannelCallbacks newCallbacks)
		{
			Preconditions.checkState (false);
		}
		
		@Override
		public final CallbackReference failed (final Channel channel, final Throwable exception)
		{
			Preconditions.checkState (channel == this.channel);
			synchronized (this.monitor) {
				this.callbackTrigger.failed (this.facade, exception);
				this.stop ();
			}
			return (null);
		}
		
		@Override
		public final CallbackReference initialized (final Channel channel)
		{
			Preconditions.checkState (channel == this.channel);
			synchronized (this.monitor) {
				this.callbackTrigger.initialized (this.facade);
			}
			return (null);
		}
		
		@Override
		public final void reassigned (final ChannelCallbacks trigger, final ChannelCallbacks oldCallbacks)
		{
			Preconditions.checkState (false);
		}
		
		@Override
		public final CallbackReference received (final Channel channel, final ChannelMessage message)
		{
			Preconditions.checkState (channel == this.channel);
			Preconditions.checkNotNull (message);
			synchronized (this.monitor) {
				switch (message.type) {
					case Exchange : {
						final Object actionValue = message.metaData.get (Token.Action.string);
						Preconditions.checkNotNull (actionValue, "missing action attribute");
						Preconditions.checkArgument (actionValue instanceof String, "invalid action attribute `%s`", actionValue);
						final Action action;
						if (Token.Call.string.equals (actionValue))
							action = Action.Call;
						else if (Token.CallReturn.string.equals (actionValue))
							action = Action.CallReturn;
						else if (Token.Cast.string.equals (actionValue))
							action = Action.Cast;
						else if (Token.RegisterReturn.string.equals (actionValue))
							action = Action.RegisterReturn;
						else
							action = null;
						Preconditions.checkNotNull (action, "invalid action attribute `%s`", actionValue);
						switch (action) {
							case Call : {
								final Object operationValue = message.metaData.get (Token.Operation.string);
								Preconditions.checkNotNull (operationValue, "missing operation attribute");
								Preconditions.checkArgument (operationValue instanceof String, "invalid operation attribute `%s`", operationValue);
								final Object correlationValue = message.metaData.get (Token.Correlation.string);
								Preconditions.checkNotNull (correlationValue, "missing correlation attribute");
								Preconditions.checkArgument (correlationValue instanceof String, "invalid correlation attribute `%s`", correlationValue);
								final String correlation = (String) correlationValue;
								Preconditions.checkArgument (message.metaData.containsKey (Token.Inputs.string), "missing meta-data attribute");
								final Object inputsValue = message.metaData.get (Token.Inputs.string);
								Preconditions.checkArgument (!this.inboundCalls.inverse ().containsKey (correlation), "coliding correlation attribute `%s`", correlation);
								final ComponentCallRequest request = ComponentCallRequest.create ((String) operationValue, inputsValue, message.data.asReadOnlyBuffer (), ComponentCallReference.create ());
								this.inboundCalls.put (request.reference, correlation);
								this.callbackTrigger.called (this.facade, request);
							}
								break;
							case CallReturn : {
								final Object correlationValue = message.metaData.get (Token.Correlation.string);
								Preconditions.checkNotNull (correlationValue, "missing correlation attribute");
								Preconditions.checkArgument (correlationValue instanceof String, "invalid correlation attribute `%s`", correlationValue);
								final String correlation = (String) correlationValue;
								final Object okValue = message.metaData.get (Token.Ok.string);
								Preconditions.checkNotNull (okValue, "missing ok attribute");
								Preconditions.checkArgument (okValue instanceof Boolean, "invalid ok attribute `%s`", okValue);
								final ComponentCallReply reply;
								if (Boolean.TRUE.equals (okValue)) {
									final Object outputsValue = message.metaData.get (Token.Outputs.string);
									Preconditions.checkArgument (message.metaData.containsKey (Token.Outputs.string), "missing outputs attribute");
									Preconditions.checkArgument (this.outboundCalls.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
									final ComponentCallReference reference = this.outboundCalls.inverse ().remove (correlation);
									reply = ComponentCallReply.create (true, outputsValue, message.data.asReadOnlyBuffer (), reference);
								} else {
									final Object errorValue = message.metaData.get (Token.Error.string);
									Preconditions.checkArgument (message.metaData.containsKey (Token.Error.string), "missing error attribute");
									Preconditions.checkArgument (this.outboundCalls.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
									final ComponentCallReference reference = this.outboundCalls.inverse ().remove (correlation);
									reply = ComponentCallReply.create (false, errorValue, message.data.asReadOnlyBuffer (), reference);
								}
								this.callbackTrigger.callReturned (this.facade, reply);
							}
								break;
							case Cast : {
								final Object operationValue = message.metaData.get (Token.Operation.string);
								Preconditions.checkNotNull (operationValue, "missing operation attribute");
								Preconditions.checkArgument (operationValue instanceof String, "invalid operation attribute `%s`", operationValue);
								Preconditions.checkArgument (message.metaData.containsKey (Token.Inputs.string), "missing meta-data attribute");
								final Object inputsValue = message.metaData.get (Token.Inputs.string);
								final ComponentCastRequest request = ComponentCastRequest.create ((String) operationValue, inputsValue, message.data.asReadOnlyBuffer ());
								this.callbackTrigger.casted (this.facade, request);
							}
								break;
							case RegisterReturn : {
								final Object correlationValue = message.metaData.get (Token.Correlation.string);
								Preconditions.checkNotNull (correlationValue, "missing correlation attribute");
								Preconditions.checkArgument (correlationValue instanceof String, "invalid correlation attribute `%s`", correlationValue);
								final String correlation = (String) correlationValue;
								final Object okValue = message.metaData.get (Token.Ok.string);
								Preconditions.checkNotNull (okValue, "missing ok attribute");
								Preconditions.checkArgument (okValue instanceof Boolean, "invalid ok attribute `%s`", okValue);
								if (Boolean.TRUE.equals (okValue)) {
									Preconditions.checkArgument (this.registers.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
									final ComponentCallReference reference = this.registers.inverse ().remove (correlation);
									this.callbackTrigger.registerReturn (this.facade, reference, true);
								} else {
									Preconditions.checkArgument (this.registers.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
									final ComponentCallReference reference = this.registers.inverse ().remove (correlation);
									this.callbackTrigger.registerReturn (this.facade, reference, false);
								}
							}
								break;
							default:
								Preconditions.checkState (false);
								break;
						}
					}
						break;
					default:
						Preconditions.checkState (false);
						break;
				}
			}
			return (null);
		}
		
		@Override
		public final void registered (final ChannelCallbacks trigger)
		{}
		
		@Override
		public final CallbackReference terminated (final Channel channel)
		{
			Preconditions.checkState (channel == this.channel);
			synchronized (this.monitor) {
				this.callbackTrigger.terminated (this.facade);
				this.stop ();
			}
			return (null);
		}
		
		@Override
		public final void unregistered (final ChannelCallbacks trigger)
		{
			this.callbackReactor.unregister (this.callbackTrigger);
		}
		
		@Override
		protected final void doStart ()
		{
			synchronized (this.monitor) {
				this.notifyStarted ();
			}
		}
		
		@Override
		protected final void doStop ()
		{
			synchronized (this.monitor) {
				this.channel.close (ChannelFlow.Inbound);
				this.channel.close (ChannelFlow.Outbound);
				this.channel.terminate ();
				this.notifyStopped ();
				System.out.println("BasicComponent.Component.doStop()");
				System.exit (1);
			}
		}
		
		final void assign (final ComponentCallbacks callbacks)
		{
			this.callbackReactor.assign (this.callbackTrigger, callbacks);
		}
		
		final void call (final ComponentIdentifier identifier, final ComponentCallRequest request)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (request);
			synchronized (this.monitor) {
				Preconditions.checkArgument (!this.outboundCalls.containsKey (request.reference));
				final String correlation = UUID.randomUUID ().toString ().replace ("-", "");
				final HashMap<String, Object> metaData = new HashMap<String, Object> ();
				metaData.put (Token.Action.string, Token.Call.string);
				metaData.put (Token.Component.string, identifier.string);
				metaData.put (Token.Operation.string, request.operation);
				metaData.put (Token.Correlation.string, correlation);
				metaData.put (Token.Inputs.string, request.inputs);
				final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, request.data);
				this.outboundCalls.put (request.reference, correlation);
				this.channel.send (message);
			}
		}
		
		final void cast (final ComponentIdentifier identifier, final ComponentCastRequest request)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (request);
			synchronized (this.monitor) {
				final HashMap<String, Object> metaData = new HashMap<String, Object> ();
				metaData.put (Token.Action.string, Token.Cast.string);
				metaData.put (Token.Component.string, identifier.string);
				metaData.put (Token.Operation.string, request.operation);
				metaData.put (Token.Inputs.string, request.inputs);
				final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, request.data);
				this.channel.send (message);
			}
		}
		
		final void register (final ComponentIdentifier identifier, final ComponentCallReference reference)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (reference);
			synchronized (this.monitor) {
				Preconditions.checkArgument (!this.registers.containsKey (reference));
				final String correlation = UUID.randomUUID ().toString ().replace ("-", "");
				final HashMap<String, Object> metaData = new HashMap<String, Object> ();
				metaData.put (Token.Action.string, Token.Register.string);
				metaData.put (Token.Group.string, identifier.string);
				metaData.put (Token.Correlation.string, correlation);
				final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, ByteBuffer.allocate (0));
				this.registers.put (reference, correlation);
				this.channel.send (message);
			}
		}
		
		final void reply (final ComponentCallReply reply)
		{
			Preconditions.checkNotNull (reply);
			synchronized (this.monitor) {
				Preconditions.checkArgument (this.inboundCalls.containsKey (reply.reference));
				final String correlation = this.inboundCalls.remove (reply.reference);
				final HashMap<String, Object> metaData = new HashMap<String, Object> ();
				metaData.put (Token.Action.string, Token.CallReturn.string);
				metaData.put (Token.Correlation.string, correlation);
				metaData.put (Token.Ok.string, Boolean.valueOf (reply.ok));
				if (reply.ok)
					metaData.put (Token.Outputs.string, reply.outputsOrError);
				else
					metaData.put (Token.Error.string, reply.outputsOrError);
				final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, reply.data.asReadOnlyBuffer ());
				this.channel.send (message);
			}
		}
		
		final CallbackReactor callbackReactor;
		final ComponentCallbacks callbackTrigger;
		final Channel channel;
		final TranscriptExceptionTracer exceptions;
		final BasicComponent facade;
		final HashBiMap<ComponentCallReference, String> inboundCalls;
		final Monitor monitor;
		final HashBiMap<ComponentCallReference, String> outboundCalls;
		final HashBiMap<ComponentCallReference, String> registers;
		final Transcript transcript;
	}
	
	private static enum Token
	{
		Action ("action"),
		Call ("call"),
		CallReturn ("call-return"),
		Cast ("cast"),
		Component ("component"),
		Correlation ("correlation"),
		Error ("error"),
		Group ("group"),
		Inputs ("inputs"),
		Ok ("ok"),
		Operation ("operation"),
		Outputs ("outputs"),
		Register ("register"),
		RegisterReturn ("register-return"),
		Request ("request");
		Token (final String string)
		{
			Preconditions.checkNotNull (string);
			this.string = string;
		}
		
		final String string;
	}
}
