/*
 * #%L
 * mosaic-components-tools
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

package eu.mosaic_cloud.components.implementations.basic;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelController;
import eu.mosaic_cloud.components.core.ChannelFlow;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.components.core.ComponentAcquireReply;
import eu.mosaic_cloud.components.core.ComponentAcquireRequest;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.components.core.ComponentTcpSocketResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentTcpSocketResourceSpecification;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBiMap;


public final class BasicComponent
			extends Object
{
	BasicComponent (final CallbackReactor callbackReactor, final ExceptionTracer exceptions) {
		super ();
		final Transcript transcript = Transcript.create (this, true);
		this.backend = new Backend (this, callbackReactor, transcript, exceptions);
	}
	
	public final boolean await () {
		return (this.await (-1));
	}
	
	public final boolean await (final long timeout) {
		return (this.backend.await (timeout));
	}
	
	public void destroy () {
		Preconditions.checkState (this.destroy (-1));
	}
	
	public final boolean destroy (final long timeout) {
		return (this.backend.destroy (timeout));
	}
	
	public final ComponentController getController () {
		return (this.backend.componentControllerProxy);
	}
	
	public final void initialize () {
		Preconditions.checkState (this.initialize (-1));
	}
	
	public final boolean initialize (final long timeout) {
		return (this.backend.initialize (timeout));
	}
	
	final Backend backend;
	
	public static final BasicComponent create (final CallbackReactor reactor, final ExceptionTracer exceptions) {
		return (new BasicComponent (reactor, exceptions));
	}
	
	public interface ComponentInternals
				extends
					Callbacks
	{
		public abstract CallbackCompletion<Void> destroy ();
		
		public abstract CallbackCompletion<Void> initialize ();
	}
	
	static enum Action
	{
		AcquireReturn (),
		Call (),
		CallReturn (),
		Cast (),
		RegisterReturn ();
	}
	
	static final class Backend
				extends StateMachine<Backend.State, Backend.Transition>
				implements
					ComponentInternals,
					ComponentController,
					ChannelCallbacks,
					CallbackHandler
	{
		Backend (final BasicComponent facade, final CallbackReactor reactor, final Transcript transcript, final ExceptionTracer exceptions) {
			super (State.class, Transition.class, transcript, exceptions);
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (reactor);
			this.facade = facade;
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
			this.acquires = HashBiMap.create ();
			this.bootstrap ();
		}
		
		@Override
		public final CallbackCompletion<Void> acquire (final ComponentAcquireRequest request) {
			Preconditions.checkNotNull (request);
			final ComponentResourceSpecification specification = request.specification;
			final ComponentCallReference reference = request.reference;
			this.execute (Transition.Executing, State.Ready, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkNotNull (specification);
					Preconditions.checkNotNull (reference);
					Preconditions.checkArgument (!Backend.this.acquires.containsKey (reference));
					final HashMap<String, Object> metaData = new HashMap<String, Object> ();
					metaData.put (Token.Action.string, Token.Acquire.string);
					final HashMap<String, String> specifications = new HashMap<String, String> ();
					if (specification instanceof ComponentTcpSocketResourceSpecification)
						specifications.put (specification.identifier, Token.SocketIpv4Tcp.string);
					else
						throw (new IllegalArgumentException ());
					final String correlation = UUID.randomUUID ().toString ().replace ("-", "");
					metaData.put (Token.Specifications.string, specifications);
					metaData.put (Token.Correlation.string, correlation);
					final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Resources, metaData, ByteBuffer.allocate (0));
					Backend.this.acquires.put (reference, correlation);
					Backend.this.channelController.send (message);
				}
			});
			return (null);
		}
		
		public final boolean await (final long timeout) {
			if (this.hasState (State.Terminated))
				return (true);
			return (this.isolate.await (timeout));
		}
		
		@Override
		public final CallbackCompletion<Void> bind (final ComponentCallbacks componentCallbacks, final ChannelController channelController) {
			this.execute (Transition.Bind, State.Binding, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkNotNull (componentCallbacks);
					Preconditions.checkNotNull (channelController);
					Backend.this.componentCallbacks = componentCallbacks;
					Backend.this.channelController = channelController;
					Backend.this.reactor.assignDelegate (Backend.this.componentCallbacksProxy, Backend.this.componentCallbacks);
					Backend.this.reactor.assignDelegate (Backend.this.channelControllerProxy, Backend.this.channelController);
					Backend.this.channelControllerProxy.bind (Backend.this.channelCallbacksProxy);
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> call (final ComponentIdentifier identifier, final ComponentCallRequest request) {
			this.execute (Transition.Executing, State.Ready, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkNotNull (identifier);
					Preconditions.checkNotNull (request);
					Preconditions.checkArgument (!Backend.this.outboundCalls.containsKey (request.reference));
					final String correlation = UUID.randomUUID ().toString ().replace ("-", "");
					final HashMap<String, Object> metaData = new HashMap<String, Object> ();
					metaData.put (Token.Action.string, Token.Call.string);
					metaData.put (Token.Component.string, identifier.string);
					metaData.put (Token.Operation.string, request.operation);
					metaData.put (Token.Correlation.string, correlation);
					metaData.put (Token.Inputs.string, request.inputs);
					final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, request.data);
					Backend.this.outboundCalls.put (request.reference, correlation);
					Backend.this.channelController.send (message);
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> callReturn (final ComponentCallReply reply) {
			this.execute (Transition.Executing, State.Ready, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkNotNull (reply);
					Preconditions.checkArgument (Backend.this.inboundCalls.containsKey (reply.reference));
					final String correlation = Backend.this.inboundCalls.remove (reply.reference);
					final HashMap<String, Object> metaData = new HashMap<String, Object> ();
					metaData.put (Token.Action.string, Token.CallReturn.string);
					metaData.put (Token.Correlation.string, correlation);
					metaData.put (Token.Ok.string, Boolean.valueOf (reply.ok));
					if (reply.ok)
						metaData.put (Token.Outputs.string, reply.outputsOrError);
					else
						metaData.put (Token.Error.string, reply.outputsOrError);
					final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, reply.data.asReadOnlyBuffer ());
					Backend.this.channelController.send (message);
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> cast (final ComponentIdentifier identifier, final ComponentCastRequest request) {
			this.execute (Transition.Executing, State.Ready, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkNotNull (identifier);
					Preconditions.checkNotNull (request);
					final HashMap<String, Object> metaData = new HashMap<String, Object> ();
					metaData.put (Token.Action.string, Token.Cast.string);
					metaData.put (Token.Component.string, identifier.string);
					metaData.put (Token.Operation.string, request.operation);
					metaData.put (Token.Inputs.string, request.inputs);
					final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, request.data);
					Backend.this.channelController.send (message);
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> closed (final ChannelController channelController, final ChannelFlow flow) {
			this.execute (new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkState (channelController == Backend.this.channelController);
					Backend.this.componentControllerProxy.terminate ();
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> destroy () {
			this.execute (new Runnable () {
				@Override
				public final void run () {
					Backend.this.componentControllerProxy.terminate ();
				}
			});
			return null;
		}
		
		public final boolean destroy (final long timeout) {
			if (this.hasState (State.Terminated))
				return (true);
			return (this.componentInternalsProxy.destroy ().await (timeout));
		}
		
		@Override
		public final CallbackCompletion<Void> failed (final ChannelController channelController, final Throwable exception) {
			this.exceptions.traceIgnoredException (exception);
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final void failedCallbacks (final Callbacks proxy, final Throwable exception) {
			this.exceptions.traceIgnoredException (exception);
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final CallbackCompletion<Void> initialize () {
			this.execute (Transition.Initialize, State.Initialized, new Runnable () {
				@Override
				public final void run () {
					Backend.this.reactor.assignHandler (Backend.this.componentControllerProxy, Backend.this, Backend.this.isolate);
					Backend.this.reactor.assignHandler (Backend.this.channelCallbacksProxy, Backend.this, Backend.this.isolate);
				}
			});
			return (null);
		}
		
		public final boolean initialize (final long timeout) {
			if (this.hasState (State.Terminated))
				return (false);
			return (this.componentInternalsProxy.initialize ().await (timeout));
		}
		
		@Override
		public final CallbackCompletion<Void> initialized (final ChannelController channelController) {
			this.execute (Transition.Binding, State.Ready, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkState (channelController == Backend.this.channelController);
					Backend.this.componentCallbacksProxy.initialized (Backend.this.componentControllerProxy);
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> received (final ChannelController channelController, final ChannelMessage message) {
			this.execute (Transition.Executing, State.Ready, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkState (channelController == Backend.this.channelController);
					Preconditions.checkNotNull (message);
					switch (message.type) {
						case Exchange :
							Backend.this.receivedExchange (message);
							break;
						default :
							Preconditions.checkState (false);
							break;
					}
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> register (final ComponentIdentifier identifier, final ComponentCallReference reference) {
			this.execute (Transition.Executing, State.Ready, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkNotNull (identifier);
					Preconditions.checkNotNull (reference);
					Preconditions.checkArgument (!Backend.this.registers.containsKey (reference));
					final String correlation = UUID.randomUUID ().toString ().replace ("-", "");
					final HashMap<String, Object> metaData = new HashMap<String, Object> ();
					metaData.put (Token.Action.string, Token.Register.string);
					metaData.put (Token.Group.string, identifier.string);
					metaData.put (Token.Correlation.string, correlation);
					final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, ByteBuffer.allocate (0));
					Backend.this.registers.put (reference, correlation);
					Backend.this.channelController.send (message);
				}
			});
			return (null);
		}
		
		@Override
		public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate) {
			Preconditions.checkNotNull (proxy);
			Preconditions.checkNotNull (isolate);
			if (proxy == this.componentInternalsProxy) {
				// NOTE: intentional
			} else if (proxy == this.componentControllerProxy) {
				// NOTE: intentional
			} else if (proxy == this.componentCallbacksProxy) {
				// NOTE: intentional
			} else if (proxy == this.channelCallbacksProxy) {
				// NOTE: intentional
			} else if (proxy == this.channelControllerProxy) {
				// NOTE: intentional
			} else {
				throw (new IllegalArgumentException ());
			}
			if (isolate == this.isolate) {
				// NOTE: intentional
			} else {
				throw (new IllegalArgumentException ());
			}
		}
		
		@Override
		public final CallbackCompletion<Void> terminate () {
			this.execute (Transition.Terminate, State.Terminating, new Runnable () {
				@Override
				public final void run () {
					if (Backend.this.hasState (State.Ready))
						Backend.this.channelControllerProxy.terminate ();
				}
			});
			return (null);
		}
		
		@Override
		public final CallbackCompletion<Void> terminated (final ChannelController channelController) {
			this.execute (Transition.Terminating, State.Terminated, new Runnable () {
				@Override
				public final void run () {
					Preconditions.checkState (channelController == Backend.this.channelController);
					Backend.this.componentCallbacksProxy.terminated (Backend.this.componentControllerProxy);
					Backend.this.reactor.destroyProxy (Backend.this.componentControllerProxy);
					Backend.this.reactor.destroyProxy (Backend.this.componentCallbacksProxy);
					Backend.this.reactor.destroyProxy (Backend.this.channelControllerProxy);
					Backend.this.reactor.destroyProxy (Backend.this.channelCallbacksProxy);
					Backend.this.reactor.destroyProxy (Backend.this.componentInternalsProxy);
					Backend.this.reactor.destroyIsolate (Backend.this.isolate);
					Backend.this.isolate.destroy ();
				}
			});
			return (null);
		}
		
		@Override
		public final void unregisteredCallbacks (final Callbacks proxy) {}
		
		final void bootstrap () {
			this.defineStates (State.class);
			this.defineTransition (Transition.Bootstrap, State.New, State.Bootstrapped);
			this.defineTransition (Transition.Initialize, State.Bootstrapped, State.Initialized);
			this.defineTransition (Transition.Bind, State.Initialized, State.Binding);
			this.defineTransition (Transition.Binding, State.Binding, State.Ready);
			this.defineTransition (Transition.Executing, State.Ready, State.Ready);
			this.defineTransition (Transition.Terminate, new State[] {State.Ready, State.Terminating, State.Terminated}, new State[] {State.Terminating});
			this.defineTransition (Transition.Terminating, State.Terminating, State.Terminated);
			this.initialize (State.New);
			this.execute (Transition.Bootstrap, State.Bootstrapped, new Runnable () {
				@Override
				public final void run () {
					Backend.this.reactor.assignHandler (Backend.this.componentInternalsProxy, Backend.this, Backend.this.isolate);
				}
			});
		}
		
		final void receivedAcquireReturn (final ChannelMessage message) {
			final Object correlationValue = message.metaData.get (Token.Correlation.string);
			Preconditions.checkNotNull (correlationValue, "missing correlation attribute");
			Preconditions.checkArgument (correlationValue instanceof String, "invalid correlation attribute `%s`", correlationValue);
			final String correlation = (String) correlationValue;
			final Object okValue = message.metaData.get (Token.Ok.string);
			Preconditions.checkNotNull (okValue, "missing ok attribute");
			Preconditions.checkArgument (okValue instanceof Boolean, "invalid ok attribute `%s`", okValue);
			final ComponentAcquireReply reply;
			if (Boolean.TRUE.equals (okValue)) {
				// FIXME: Someone should refactor this code... (at least...)
				final Object descriptorsValue = message.metaData.get (Token.Descriptors.string);
				Preconditions.checkArgument (message.metaData.containsKey (Token.Descriptors.string), "missing descriptors attribute");
				Preconditions.checkArgument (descriptorsValue instanceof Map, "mismatched descriptors attribute `%s`", descriptorsValue);
				final Map<?, ?> descriptorsMap = (Map<?, ?>) descriptorsValue;
				Preconditions.checkArgument (descriptorsMap.size () == 1, "mismatched descriptors attribute `%s`", descriptorsValue);
				final Map.Entry<?, ?> descriptorPair = descriptorsMap.entrySet ().iterator ().next ();
				final Object descriptorIdentifierValue = descriptorPair.getKey ();
				final Object descriptorValue = descriptorPair.getValue ();
				Preconditions.checkArgument (descriptorIdentifierValue instanceof String, "missmatched descriptors attribute `%s`", descriptorsValue);
				Preconditions.checkArgument (descriptorValue instanceof Map, "missmatched descriptors attribute `%s`", descriptorsValue);
				final String identifier = (String) descriptorIdentifierValue;
				final Map<?, ?> descriptorMap = (Map<?, ?>) descriptorValue;
				final Object typeValue = descriptorMap.get (Token.Type.string);
				Preconditions.checkArgument (typeValue instanceof String, "mismatched correlation attribute `%s`", correlation);
				final ComponentResourceDescriptor descriptor;
				if (Token.SocketIpv4Tcp.string.equals (typeValue)) {
					final String ip = (String) descriptorMap.get (Token.Ip.string);
					final int port = ((Number) descriptorMap.get (Token.Port.string)).intValue ();
					final String fqdn = (String) descriptorMap.get (Token.Fqdn.string);
					descriptor = ComponentTcpSocketResourceDescriptor.create (identifier, ip, port, fqdn);
				} else
					throw (new IllegalArgumentException (String.format ("mismatched correlation attribute `%s`", correlation)));
				Preconditions.checkArgument (this.acquires.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
				final ComponentCallReference reference = this.acquires.inverse ().remove (correlation);
				reply = ComponentAcquireReply.create (descriptor, reference);
			} else {
				final Object errorValue = message.metaData.get (Token.Error.string);
				Preconditions.checkArgument (message.metaData.containsKey (Token.Error.string), "missing error attribute");
				Preconditions.checkArgument (this.acquires.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
				final ComponentCallReference reference = this.acquires.inverse ().remove (correlation);
				reply = ComponentAcquireReply.create (errorValue, reference);
			}
			this.componentCallbacksProxy.acquireReturned (this.componentControllerProxy, reply);
		}
		
		final void receivedCall (final ChannelMessage message) {
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
		
		final void receivedCallReturn (final ChannelMessage message) {
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
		
		final void receivedCast (final ChannelMessage message) {
			final Object operationValue = message.metaData.get (Token.Operation.string);
			Preconditions.checkNotNull (operationValue, "missing operation attribute");
			Preconditions.checkArgument (operationValue instanceof String, "invalid operation attribute `%s`", operationValue);
			Preconditions.checkArgument (message.metaData.containsKey (Token.Inputs.string), "missing meta-data attribute");
			final Object inputsValue = message.metaData.get (Token.Inputs.string);
			final ComponentCastRequest request = ComponentCastRequest.create ((String) operationValue, inputsValue, message.data.asReadOnlyBuffer ());
			this.componentCallbacksProxy.casted (this.componentControllerProxy, request);
		}
		
		final void receivedExchange (final ChannelMessage message) {
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
			else if (Token.AcquireReturn.string.equals (actionValue))
				action = Action.AcquireReturn;
			else
				action = null;
			Preconditions.checkNotNull (action, "invalid action attribute `%s`", actionValue);
			switch (action) {
				case Call :
					this.receivedCall (message);
					break;
				case CallReturn :
					this.receivedCallReturn (message);
					break;
				case Cast :
					this.receivedCast (message);
					break;
				case RegisterReturn :
					this.receivedRegisterReturn (message);
					break;
				case AcquireReturn :
					this.receivedAcquireReturn (message);
					break;
				default :
					Preconditions.checkState (false);
					break;
			}
		}
		
		final void receivedRegisterReturn (final ChannelMessage message) {
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
		
		final HashBiMap<ComponentCallReference, String> acquires;
		final ChannelCallbacks channelCallbacksProxy;
		ChannelController channelController;
		final ChannelController channelControllerProxy;
		ComponentCallbacks componentCallbacks;
		final ComponentCallbacks componentCallbacksProxy;
		final ComponentController componentControllerProxy;
		final ComponentInternals componentInternalsProxy;
		final BasicComponent facade;
		final HashBiMap<ComponentCallReference, String> inboundCalls;
		final CallbackIsolate isolate;
		final HashBiMap<ComponentCallReference, String> outboundCalls;
		final CallbackReactor reactor;
		final HashBiMap<ComponentCallReference, String> registers;
		
		enum State
					implements
						StateMachine.State
		{
			Binding (),
			Bootstrapped (),
			Initialized (),
			New (),
			Ready (),
			Terminated (),
			Terminating ();
		}
		
		enum Transition
					implements
						StateMachine.Transition
		{
			Bind (),
			Binding (),
			Bootstrap (),
			Executing (),
			Initialize (),
			Terminate (),
			Terminating ();
		}
	}
	
	static enum Token
	{
		Acquire ("acquire"),
		AcquireReturn ("acquire-return"),
		Action ("action"),
		Call ("call"),
		CallReturn ("call-return"),
		Cast ("cast"),
		Component ("component"),
		Correlation ("correlation"),
		Descriptors ("descriptors"),
		Error ("error"),
		Fqdn ("fqdn"),
		Group ("group"),
		Inputs ("inputs"),
		Ip ("ip"),
		Ok ("ok"),
		Operation ("operation"),
		Outputs ("outputs"),
		Port ("port"),
		Register ("register"),
		RegisterReturn ("register-return"),
		Request ("request"),
		SocketIpv4Tcp ("socket:ipv4:tcp"),
		Specifications ("specifications"),
		Type ("type");
		Token (final String string) {
			Preconditions.checkNotNull (string);
			this.string = string;
		}
		
		final String string;
	}
}
