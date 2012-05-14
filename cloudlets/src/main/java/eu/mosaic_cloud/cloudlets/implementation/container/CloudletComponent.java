/*y
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

package eu.mosaic_cloud.cloudlets.implementation.container;


import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletManager;
import eu.mosaic_cloud.cloudlets.implementation.container.CloudletComponentFsm.FsmState;
import eu.mosaic_cloud.cloudlets.implementation.container.CloudletComponentFsm.FsmTransition;
import eu.mosaic_cloud.cloudlets.tools.ConfigProperties;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelFactoryAndResolver;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine.StateAndOutput;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.DeferredException;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture.Trigger;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


/**
 * This callback class enables the container to communicate with other platform
 * components. Methods defined in the callback will be called by the mOSAIC
 * platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class CloudletComponent
		extends Object
{
	private CloudletComponent (final ComponentEnvironment componentEnvironment)
	{
		super ();
		{
			Preconditions.checkNotNull (componentEnvironment);
			this.componentEnvironment = componentEnvironment;
			this.transcript = Transcript.create (this, true);
			this.exceptions = TranscriptExceptionTracer.create (this.transcript, this.componentEnvironment.exceptions);
			this.fsm = new CloudletComponentFsm (this, this.transcript, this.exceptions);
		}
		{
			this.reactor = this.componentEnvironment.reactor;
			this.threading = this.componentEnvironment.threading;
			this.classLoader = this.componentEnvironment.classLoader;
			this.componentPendingOutboundCalls = new IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>> ();
		}
		this.transcript.traceDebugging ("creating the cloudlet component...");
		try {
			{
				this.configuration = this.resolveConfiguration ();
				this.selfGroup = this.resolveSelfGroup ();
			}
			{
				this.isolate = this.reactor.createIsolate ();
				this.componentCallbacksProxy = this.reactor.createProxy (ComponentCallbacks.class);
				this.componentControllerProxy = this.reactor.createProxy (ComponentController.class);
				this.channelFactoryProxy = this.reactor.createProxy (ChannelFactoryAndResolver.class);
				this.transcript.traceDebugging ("using the callbacks isolate `%{object:identity}`...", this.isolate);
				this.transcript.traceDebugging ("using the component callbacks proxy `%{object:identity}`...", this.componentCallbacksProxy);
				this.transcript.traceDebugging ("using the component controller proxy `%{object:identity}`...", this.componentControllerProxy);
				this.transcript.traceDebugging ("using the interoperability channel resolver proxy `%{object:identity}`...", this.channelFactoryProxy);
			}
			{
				this.channelIdentifier = UUID.randomUUID ().toString ();
				this.channel = ZeroMqChannel.create (this.channelIdentifier, this.threading, this.exceptions);
				this.transcript.traceDebugging ("using the interoperability identity `%s`...", this.channelIdentifier);
				this.transcript.traceDebugging ("using the interoperability channel `%{object:identifier}`...", this.channel);
			}
			{
				this.manager = CloudletManager.create (this.configuration, this.classLoader, this.reactor, this.threading, this.exceptions, this.channelFactoryProxy, this.channelFactoryProxy);
				this.transcript.traceDebugging ("using the cloudlet manager `%{object:identity}`...", this.manager);
			}
			{
				this.fsm.execute (FsmTransition.CreateCompleted, FsmState.RegisterPending2);
			}
			{
				this.componentCallbacksHandler = new ComponentCallbacksHandler ();
				this.channelFactoryHandler = new ChannelFactoryHandler ();
				CloudletComponent.this.reactor.assignHandler (CloudletComponent.this.componentCallbacksProxy, CloudletComponent.this.componentCallbacksHandler, CloudletComponent.this.isolate);
				CloudletComponent.this.reactor.assignHandler (CloudletComponent.this.channelFactoryProxy, CloudletComponent.this.channelFactoryHandler, CloudletComponent.this.isolate);
				this.transcript.traceDebugging ("using the component callbacks handler `%{object:identity}` assigned to `%{object:identity}`...", this.componentCallbacksHandler, this.componentCallbacksProxy);
				this.transcript.traceDebugging ("using the interoperability channel resolver handler `%{object:identity}` assigned to `%{object:identity}`...", this.channelFactoryHandler, this.channelFactoryProxy);
			}
		} catch (final CaughtException.Wrapper wrapper) {
			wrapper.trace (this.exceptions);
			this.handleInternalFailure (null, wrapper.exception.caught);
			throw (wrapper);
		} catch (final Throwable exception) {
			this.handleInternalFailure (null, exception);
			throw (new DeferredException (exception).wrap ());
		}
		this.transcript.traceInformation ("created the cloudlet component.");
	}
	
	final void handleCleanup (final boolean gracefully)
	{
		if (gracefully) {
			this.transcript.traceDebugging ("cleaning-up the cloudlet component (gracefully)...");
		} else {
			this.transcript.traceWarning ("cleaning-up the cloudlet component (forced)...");
		}
		if ((this.componentControllerProxy != null) && !gracefully) {
			this.transcript.traceDebugging ("destroying the component controller proxy (forced)...");
			try {
				this.reactor.destroyProxy (this.componentControllerProxy);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "destroying the component controller proxy failed; ignoring!");
			}
		}
		if ((this.componentCallbacksProxy != null) && !gracefully) {
			this.transcript.traceDebugging ("destroying the component callbacks proxy (forced)...");
			try {
				this.reactor.destroyProxy (this.componentCallbacksProxy);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "destroying the component callbacks proxy failed; ignoring!");
			}
		}
		if ((this.channelFactoryProxy != null) && !gracefully) {
			this.transcript.traceDebugging ("destroying the interoperability channel resolver proxy (forced)...");
			try {
				this.reactor.destroyProxy (this.channelFactoryProxy);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "destroying the interoperability channel proxy failed; ignoring!");
			}
		}
		// FIXME: this should be asynchronous and handled as part of the
		//-- life-cycle
		if (this.manager != null) {
			this.transcript.traceDebugging ("destroying the cloudlet manager...");
			try {
				this.manager.destroy ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "destroying the cloudlet manager failed; ignoring!");
			}
		}
		// FIXME: this should be asynchronous and handled as part of the
		//-- life-cycle
		if (this.channel != null) {
			this.transcript.traceDebugging ("destroying the interoperability channel...");
			try {
				this.channel.terminate ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "destroying the interoperability channel failed; ignoring!");
			}
		}
		if (this.isolate != null) {
			this.transcript.traceDebugging ("destroying the callbacks isolate...");
			try {
				this.reactor.destroyIsolate (this.isolate);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "destroying the callbacks isolate failed; ignoring!");
			}
		}
	}
	
	private final void handleInternalFailure (final Callbacks proxy, final Throwable exception)
	{
		this.fsm.new FsmVoidTransaction (FsmTransition.InternalFailure) {
			@Override
			protected final StateAndOutput<FsmState, Void> execute ()
			{
				CloudletComponent.this.exceptions.traceDeferredException (exception, "failed proxy `%{object:identity}`; aborting!", proxy);
				if (CloudletComponent.this.fsm.hasState (FsmState.Failed)) {
					return (StateAndOutput.create (FsmState.Failed, null));
				}
				CloudletComponent.this.handleCleanup (false);
				return (StateAndOutput.create (FsmState.Failed, null));
			}
		}.trigger ();
	}
	
	private final IConfiguration resolveConfiguration ()
	{
		this.transcript.traceDebugging ("resolving the cloudlet component configuration...");
		final String configurationDescriptor = this.componentEnvironment.supplementary.get ("descriptor", String.class, null); // $NON-NLS-1$
		Preconditions.checkNotNull (configurationDescriptor, "unknown cloudlet component configuration descriptor");
		this.transcript.traceDebugging ("resolving the cloudlet component configuration `%s`...", configurationDescriptor);
		final IConfiguration configuration;
		try {
			configuration = PropertyTypeConfiguration.create (this.classLoader, configurationDescriptor);
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new IllegalArgumentException ("error encountered while loading cloudlet component configuration", exception));
		}
		return (configuration);
	}
	
	private final ComponentIdentifier resolveSelfGroup ()
	{
		this.transcript.traceDebugging ("resolving the cloudlet component self-group...");
		final String groupName = ConfigUtils.resolveParameter (this.configuration, ConfigProperties.getString ("CloudletComponent.3"), String.class, ConfigProperties.getString ("CloudletComponent.14")); // $NON-NLS-1$
		Preconditions.checkNotNull (groupName, "unknown cloudlet component self group");
		final ComponentIdentifier group = ComponentIdentifier.resolve (groupName);
		return (group);
	}
	
	public static final CloudletComponent create (final ComponentEnvironment context)
	{
		return (new CloudletComponent (context));
	}
	
	final ZeroMqChannel channel;
	final ChannelFactoryHandler channelFactoryHandler;
	final ChannelFactoryAndResolver channelFactoryProxy;
	final String channelIdentifier;
	final ClassLoader classLoader;
	final ComponentCallbacksHandler componentCallbacksHandler;
	final ComponentCallbacks componentCallbacksProxy;
	final ComponentController componentControllerProxy;
	final ComponentEnvironment componentEnvironment;
	final IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>> componentPendingOutboundCalls;
	final IConfiguration configuration;
	final TranscriptExceptionTracer exceptions;
	final CloudletComponentFsm fsm;
	final CallbackIsolate isolate;
	final CloudletManager manager;
	final CallbackReactor reactor;
	final ComponentIdentifier selfGroup;
	final ThreadingContext threading;
	final Transcript transcript;
	
	public static final class ComponentCallbacksProvider
			extends Object
			implements
				eu.mosaic_cloud.components.core.ComponentCallbacksProvider
	{
		@Override
		public final ComponentCallbacks provide (final ComponentEnvironment context)
		{
			final CloudletComponent component = CloudletComponent.create (context);
			return (component.componentCallbacksProxy);
		}
	}
	
	final class ChannelFactoryHandler
			extends Object
			implements
				ChannelFactoryAndResolver,
				CallbackHandler
	{
		@Override
		public final Channel create ()
		{
			CloudletComponent.this.transcript.traceDebugging ("requested the interoperability channel...");
			// FIXME: This should be done in `Active` state
			// FIXME: This should be done in an `FsmAccess`
			return (CloudletComponent.this.channel);
		}
		
		@Override
		public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (proxy == CloudletComponent.this.componentCallbacksProxy);
			CloudletComponent.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			CloudletComponent.this.transcript.traceDebugging ("registered the interoperability channel resolver handler.");
			CloudletComponent.this.fsm.new FsmVoidTransaction (FsmTransition.RegisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					return (CloudletComponent.this.componentCallbacksHandler.registerCallbacksExecute ());
				}
			}.trigger ();
		}
		
		@Override
		public final void resolve (final String target, final ResolverCallbacks callbacks)
		{
			Preconditions.checkNotNull (target);
			Preconditions.checkNotNull (callbacks);
			CloudletComponent.this.transcript.traceDebugging ("resolving the interoperability channel endpoint for target `%s`...", target);
			// FIXME: This should be done in `Active` state
			// FIXME: This should be done in an `FsmAccess`
			final ComponentIdentifier identifier = ComponentIdentifier.resolve (target);
			final String operation = ConfigProperties.getString ("CloudletComponent.7");
			final ComponentCallReference reference = ComponentCallReference.create ();
			final ComponentCallRequest request = ComponentCallRequest.create (operation, null, reference);
			final DeferredFuture<ComponentCallReply> future = DeferredFuture.create (ComponentCallReply.class);
			CloudletComponent.this.componentControllerProxy.call (identifier, request);
			CloudletComponent.this.componentPendingOutboundCalls.put (reference, future.trigger);
			CloudletComponent.this.fsm.new FsmFutureCompletionAccess<ComponentCallReply> () {
				@Override
				protected Void execute (final Future<ComponentCallReply> future1)
				{
					Preconditions.checkState (future == future1);
					final ComponentCallReply reply;
					try {
						reply = future.get ();
					} catch (final Throwable exception) {
						// FIXME: should call callbacks with failure
						CloudletComponent.this.exceptions.traceIgnoredException (exception, "resolving the interoperability channel endpoint for target `%s` failed; ignoring!", target);
						return (null);
					}
					if (!reply.ok) {
						// FIXME: should call callbacks with failure
						CloudletComponent.this.exceptions.traceIgnoredException (new Exception (), "resolving the interoperability channel endpoint for target `%s` failed; ignoring!", target);
						return (null);
					}
					final String peerIdentifierKey = ConfigProperties.getString ("CloudletComponent.13");
					final String peerEndpointKey = ConfigProperties.getString ("CloudletComponent.12");
					final String peerIdentifier;
					final String peerEndpoint;
					try {
						peerIdentifier = (String) ((Map<?, ?>) reply.outputsOrError).get (peerIdentifierKey);
						peerEndpoint = (String) ((Map<?, ?>) reply.outputsOrError).get (peerEndpointKey);
					} catch (final Throwable exception) {
						// FIXME: should call callbacks with failure
						CloudletComponent.this.exceptions.traceIgnoredException (exception, "resolving the interoperability channel endpoint for target `%s` failed; ignoring!", target);
						return (null);
					}
					CloudletComponent.this.transcript.traceDebugging ("resolved the interoperability channel endpoint for target `%s` successfully (with endpoint `%s`, and identity `%s`); delegating.", target, peerEndpoint, peerIdentifier);
					try {
						callbacks.resolved (CloudletComponent.this.channelFactoryProxy, target, peerIdentifier, peerEndpoint);
					} catch (final CaughtException.Wrapper wrapper) {
						wrapper.trace (CloudletComponent.this.exceptions);
						CloudletComponent.this.exceptions.traceIgnoredException (wrapper.exception.caught);
						return (null);
					} catch (final Throwable exception) {
						CloudletComponent.this.exceptions.traceIgnoredException (exception);
						return (null);
					}
					return (null);
				}
			}.observe (future);
		}
		
		@Override
		public final void unregisteredCallbacks (final Callbacks proxy)
		{
			CloudletComponent.this.transcript.traceDebugging ("unregistered the interoperability channel resolver handler.");
			CloudletComponent.this.fsm.new FsmVoidTransaction (FsmTransition.UnregisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					return (CloudletComponent.this.componentCallbacksHandler.unregisterCallbacksExecute ());
				}
			}.trigger ();
		}
	}
	
	final class ComponentCallbacksHandler
			extends Object
			implements
				ComponentCallbacks,
				CallbackHandler
	{
		@Override
		public final CallbackCompletion<Void> called (final ComponentController component, final ComponentCallRequest request)
		{
			CloudletComponent.this.transcript.traceInformation ("called the cloudlet component with operation `%s` and reference `%{object:identity}`...", request.operation, request.reference);
			return (CloudletComponent.this.fsm.new FsmCallbackAccess () {
				@Override
				protected CallbackCompletion<Void> execute ()
				{
					CloudletComponent.this.transcript.traceError ("called the cloudlet component with an unsupported operation `%s` and reference `%{object:identity}`; returning failure!", request.operation, request.reference);
					final ComponentCallReply reply = ComponentCallReply.create (false, "unsupported-call", request.reference);
					CloudletComponent.this.componentControllerProxy.callReturn (reply);
					return (CallbackCompletion.createOutcome ());
				}
			}.trigger ());
		}
		
		@Override
		public final CallbackCompletion<Void> callReturned (final ComponentController component, final ComponentCallReply reply)
		{
			CloudletComponent.this.transcript.traceDebugging ("call returned for the cloudlet component with reference `%{object:identity}` and outcome `%b`...", reply.reference, Boolean.valueOf (reply.ok));
			return (CloudletComponent.this.fsm.new FsmCallbackAccess () {
				@Override
				protected CallbackCompletion<Void> execute ()
				{
					final Trigger<ComponentCallReply> trigger = CloudletComponent.this.componentPendingOutboundCalls.remove (reply.reference);
					if (trigger != null) {
						trigger.triggerSucceeded (reply);
					} else {
						CloudletComponent.this.transcript.traceError ("call returned for the cloudlet component with an unexpected reference `%{object:identity}`; ignoring!", reply.reference);
					}
					return (CallbackCompletion.createOutcome ());
				}
			}.trigger ());
		}
		
		@Override
		public final CallbackCompletion<Void> casted (final ComponentController component, final ComponentCastRequest request)
		{
			CloudletComponent.this.transcript.traceInformation ("casted the cloudlet component with operation `%s`...", request.operation);
			return (CloudletComponent.this.fsm.new FsmCallbackAccess () {
				@Override
				protected CallbackCompletion<Void> execute ()
				{
					CloudletComponent.this.transcript.traceError ("casted the cloudlet component with an unsupported operation `%s`; ignoring!", request.operation);
					return (CallbackCompletion.createOutcome ());
				}
			}.trigger ());
		}
		
		@Override
		public final CallbackCompletion<Void> failed (final ComponentController component, final Throwable exception)
		{
			CloudletComponent.this.transcript.traceError ("failed the cloudlet component.");
			CloudletComponent.this.handleInternalFailure (null, exception);
			return (CallbackCompletion.createOutcome ());
		}
		
		@Override
		public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (proxy == CloudletComponent.this.componentCallbacksProxy);
			CloudletComponent.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public final CallbackCompletion<Void> initialized (final ComponentController component)
		{
			CloudletComponent.this.transcript.traceDebugging ("initialized the cloudlet component.");
			return (CloudletComponent.this.fsm.new FsmCallbackTransaction (FsmTransition.InitializeCompleted) {
				@Override
				protected StateAndOutput<FsmState, CallbackCompletion<Void>> execute ()
				{
					CloudletComponent.this.reactor.assignDelegate (CloudletComponent.this.componentControllerProxy, component);
					// FIXME: this should be asynchronous and handled as part of
					//-- the life-cycle
					final int count = 1;
					CloudletComponent.this.transcript.traceDebugging ("creating the cloudlet instances (with count `%d`)...", Integer.valueOf (count));
					for (int index = 0; index < count; index++) {
						final boolean succeeded = CloudletComponent.this.manager.createInstance ();
						Preconditions.checkState (succeeded);
					}
					return (StateAndOutput.create (FsmState.Active, CallbackCompletion.createOutcome ()));
				}
			}.trigger ());
		}
		
		@Override
		public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			CloudletComponent.this.transcript.traceDebugging ("registered the component callbacks handler.");
			CloudletComponent.this.fsm.new FsmVoidTransaction (FsmTransition.RegisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					return (ComponentCallbacksHandler.this.registerCallbacksExecute ());
				}
			}.trigger ();
		}
		
		@Override
		public final CallbackCompletion<Void> registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok)
		{
			CloudletComponent.this.transcript.traceInformation ("registered the cloudlet component.");
			return (CloudletComponent.this.fsm.new FsmCallbackAccess () {
				@Override
				protected CallbackCompletion<Void> execute ()
				{
					// FIXME: log the situation
					return (CallbackCompletion.createOutcome ());
				}
			}.trigger ());
		}
		
		@Override
		public final CallbackCompletion<Void> terminated (final ComponentController component)
		{
			CloudletComponent.this.transcript.traceInformation ("terminated the cloudlet component.");
			return (CloudletComponent.this.fsm.new FsmCallbackTransaction (FsmTransition.ExternalDestroy) {
				@Override
				protected StateAndOutput<FsmState, CallbackCompletion<Void>> execute ()
				{
					CloudletComponent.this.transcript.traceDebugging ("destroying the component controller proxy...");
					CloudletComponent.this.reactor.destroyProxy (CloudletComponent.this.componentControllerProxy);
					CloudletComponent.this.transcript.traceDebugging ("destroying the component callbacks proxy...");
					CloudletComponent.this.reactor.destroyProxy (CloudletComponent.this.componentCallbacksProxy);
					CloudletComponent.this.transcript.traceDebugging ("destroying the interoperability channel resolver proxy...");
					CloudletComponent.this.reactor.destroyProxy (CloudletComponent.this.channelFactoryProxy);
					return (StateAndOutput.create (FsmState.UnregisterPending2, null));
				}
			}.trigger ());
		}
		
		@Override
		public final void unregisteredCallbacks (final Callbacks proxy)
		{
			CloudletComponent.this.transcript.traceDebugging ("unregistered the component callbacks handler.");
			CloudletComponent.this.fsm.new FsmVoidTransaction (FsmTransition.UnregisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					return (ComponentCallbacksHandler.this.unregisterCallbacksExecute ());
				}
			}.trigger ();
		}
		
		final StateAndOutput<FsmState, Void> registerCallbacksExecute ()
		{
			final FsmState state = CloudletComponent.this.fsm.getState ();
			switch (state) {
				case RegisterPending2 :
					return (StateAndOutput.create (FsmState.RegisterPending1, null));
				case RegisterPending1 :
					return (StateAndOutput.create (FsmState.InitializePending, null));
				default:
					throw (new AssertionError ());
			}
		}
		
		final StateAndOutput<FsmState, Void> unregisterCallbacksExecute ()
		{
			final FsmState state = CloudletComponent.this.fsm.getState ();
			switch (state) {
				case UnregisterPending2 :
					return (StateAndOutput.create (FsmState.UnregisterPending1, null));
				case UnregisterPending1 :
					CloudletComponent.this.handleCleanup (true);
					return (StateAndOutput.create (FsmState.Destroyed, null));
				default:
					throw (new AssertionError ());
			}
		}
	}
}
