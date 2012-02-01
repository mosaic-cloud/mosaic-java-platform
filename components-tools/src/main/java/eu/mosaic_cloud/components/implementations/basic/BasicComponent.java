/*
 * #%L
 * mosaic-components-tools
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

package eu.mosaic_cloud.components.implementations.basic;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBiMap;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelController;
import eu.mosaic_cloud.components.core.ChannelFlow;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;


public final class BasicComponent
		extends Object
{
	private BasicComponent (final CallbackReactor callbackReactor, final ExceptionTracer exceptions)
	{
		super ();
		this.backend = new Backend (this, callbackReactor, exceptions);
	}
	
	public final boolean await ()
	{
		return (this.await (-1));
	}
	
	public final boolean await (final long timeout)
	{
		return (this.backend.isolate.await (timeout));
	}
	
	public final CallbackReference bind (final ChannelController controller)
	{
		return (this.backend.componentInternalsProxy.bind (controller));
	}
	
	public void destroy ()
	{
		Preconditions.checkState (this.destroy (-1));
	}
	
	public final boolean destroy (final long timeout)
	{
		return (this.backend.componentInternalsProxy.destroy ().await (timeout));
	}
	
	public final ComponentController getController ()
	{
		return (this.backend.componentControllerProxy);
	}
	
	public final void initialize ()
	{
		Preconditions.checkState (this.initialize (-1));
	}
	
	public final boolean initialize (final long timeout)
	{
		return (this.backend.componentInternalsProxy.initialize ().await (timeout));
	}
	
	final Backend backend;
	
	public static final BasicComponent create (final CallbackReactor reactor, final ExceptionTracer exceptions)
	{
		return (new BasicComponent (reactor, exceptions));
	}
	
	public static interface ComponentInternals
			extends
				Callbacks
	{
		public abstract CallbackReference bind (final ChannelController channelController);
		
		public abstract CallbackReference destroy ();
		
		public abstract CallbackReference initialize ();
	}
	
	private static enum Action
	{
		Call (),
		CallReturn (),
		Cast (),
		RegisterReturn ();
	}
	
	private static final class Backend
			extends Object
			implements
				ComponentInternals,
				ComponentController,
				ChannelCallbacks,
				CallbackHandler
	{
		Backend (final BasicComponent facade, final CallbackReactor reactor, final ExceptionTracer exceptions)
		{
			super ();
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (reactor);
			this.facade = facade;
			this.transcript = Transcript.create (this.facade);
			this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
			this.reactor = reactor;
			this.isolate = this.reactor.createIsolate ();
			this.componentInternalsProxy = this.reactor.createProxy (ComponentInternals.class);
			this.componentControllerProxy = this.reactor.createProxy (ComponentController.class);
			this.componentCallbacksProxy = this.reactor.createProxy (ComponentCallbacks.class);
			this.channelControllerProxy = this.reactor.createProxy (ChannelController.class);
			this.channelCallbacksProxy = this.reactor.createProxy (ChannelCallbacks.class);
			this.channelController = null;
			this.inboundCalls = HashBiMap.create ();
			this.outboundCalls = HashBiMap.create ();
			this.registers = HashBiMap.create ();
			this.reactor.assignHandler (this.componentInternalsProxy, this, this.isolate);
		}
		
		@Override
		public final CallbackReference assign (final ComponentCallbacks delegate)
		{
			return (this.reactor.assignDelegate (this.componentCallbacksProxy, delegate));
		}
		
		@Override
		public final CallbackReference bind (final ChannelController channelController)
		{
			Preconditions.checkNotNull (channelController);
			Preconditions.checkState (this.channelController == null);
			this.channelController = channelController;
			return (channelController.assign (this.channelCallbacksProxy));
		}
		
		@Override
		public final CallbackReference call (final ComponentIdentifier identifier, final ComponentCallRequest request)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (request);
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
			return (this.channelController.send (message));
		}
		
		@Override
		public final CallbackReference callReturn (final ComponentCallReply reply)
		{
			Preconditions.checkNotNull (reply);
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
			return (this.channelController.send (message));
		}
		
		@Override
		public final CallbackReference cast (final ComponentIdentifier identifier, final ComponentCastRequest request)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (request);
			final HashMap<String, Object> metaData = new HashMap<String, Object> ();
			metaData.put (Token.Action.string, Token.Cast.string);
			metaData.put (Token.Component.string, identifier.string);
			metaData.put (Token.Operation.string, request.operation);
			metaData.put (Token.Inputs.string, request.inputs);
			final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, request.data);
			return (this.channelController.send (message));
		}
		
		@Override
		public final CallbackReference closed (final ChannelController channelController, final ChannelFlow flow)
		{
			Preconditions.checkState (channelController == this.channelController);
			this.channelControllerProxy.terminate ();
			return (null);
		}
		
		@Override
		public final CallbackReference destroy ()
		{
			return null;
		}
		
		@Override
		public final CallbackReference failed (final ChannelController channelController, final Throwable exception)
		{
			Preconditions.checkState (channelController == this.channelController);
			this.exceptions.traceDeferredException (exception);
			throw (new Error (exception));
		}
		
		@Override
		public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			this.exceptions.traceDeferredException (exception);
			throw (new Error (exception));
		}
		
		@Override
		public final CallbackReference initialize ()
		{
			this.reactor.assignHandler (this.componentControllerProxy, this, this.isolate);
			this.reactor.assignHandler (this.channelCallbacksProxy, this, this.isolate);
			this.componentCallbacksProxy.initialized (this.componentControllerProxy);
			return (null);
		}
		
		@Override
		public final CallbackReference initialized (final ChannelController channelController)
		{
			Preconditions.checkState (channelController == this.channelController);
			return (null);
		}
		
		@Override
		public final CallbackReference received (final ChannelController channelController, final ChannelMessage message)
		{
			Preconditions.checkState (channelController == this.channelController);
			Preconditions.checkNotNull (message);
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
							this.componentCallbacksProxy.called (this.componentControllerProxy, request);
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
							this.componentCallbacksProxy.callReturned (this.componentControllerProxy, reply);
						}
							break;
						case Cast : {
							final Object operationValue = message.metaData.get (Token.Operation.string);
							Preconditions.checkNotNull (operationValue, "missing operation attribute");
							Preconditions.checkArgument (operationValue instanceof String, "invalid operation attribute `%s`", operationValue);
							Preconditions.checkArgument (message.metaData.containsKey (Token.Inputs.string), "missing meta-data attribute");
							final Object inputsValue = message.metaData.get (Token.Inputs.string);
							final ComponentCastRequest request = ComponentCastRequest.create ((String) operationValue, inputsValue, message.data.asReadOnlyBuffer ());
							this.componentCallbacksProxy.casted (this.componentControllerProxy, request);
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
								this.componentCallbacksProxy.registerReturned (this.componentControllerProxy, reference, true);
							} else {
								Preconditions.checkArgument (this.registers.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
								final ComponentCallReference reference = this.registers.inverse ().remove (correlation);
								this.componentCallbacksProxy.registerReturned (this.componentControllerProxy, reference, false);
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
			return (null);
		}
		
		@Override
		public final CallbackReference register (final ComponentIdentifier identifier, final ComponentCallReference reference)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (reference);
			Preconditions.checkArgument (!this.registers.containsKey (reference));
			final String correlation = UUID.randomUUID ().toString ().replace ("-", "");
			final HashMap<String, Object> metaData = new HashMap<String, Object> ();
			metaData.put (Token.Action.string, Token.Register.string);
			metaData.put (Token.Group.string, identifier.string);
			metaData.put (Token.Correlation.string, correlation);
			final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, ByteBuffer.allocate (0));
			this.registers.put (reference, correlation);
			return (this.channelController.send (message));
		}
		
		@Override
		public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{}
		
		@Override
		public final CallbackReference terminate ()
		{
			this.componentCallbacksProxy.terminated (this.componentControllerProxy);
			this.reactor.destroyProxy (this.componentControllerProxy);
			this.reactor.destroyProxy (this.componentCallbacksProxy);
			this.reactor.destroyProxy (this.channelControllerProxy);
			this.reactor.destroyProxy (this.channelCallbacksProxy);
			this.reactor.destroyProxy (this.componentInternalsProxy);
			return (this.reactor.destroyIsolate (this.isolate));
		}
		
		@Override
		public final CallbackReference terminated (final ChannelController channelController)
		{
			Preconditions.checkState (channelController == this.channelController);
			this.channelControllerProxy.terminate ();
			return (null);
		}
		
		@Override
		public final void unregisteredCallbacks (final Callbacks proxy)
		{}
		
		final ChannelCallbacks channelCallbacksProxy;
		ChannelController channelController;
		final ChannelController channelControllerProxy;
		final ComponentCallbacks componentCallbacksProxy;
		final ComponentController componentControllerProxy;
		final ComponentInternals componentInternalsProxy;
		final TranscriptExceptionTracer exceptions;
		final BasicComponent facade;
		final HashBiMap<ComponentCallReference, String> inboundCalls;
		final CallbackIsolate isolate;
		final HashBiMap<ComponentCallReference, String> outboundCalls;
		final CallbackReactor reactor;
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
