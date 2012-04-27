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

package eu.mosaic_cloud.cloudlets.implementation.cloudlet;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.cloudlets.core.CloudletCallbackArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletCallbackCompletionArguments;
import eu.mosaic_cloud.cloudlets.core.CloudletState;
import eu.mosaic_cloud.cloudlets.core.ICallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletCallback;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletFsm.FsmCallbackCompletionTransaction;
import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletFsm.FsmState;
import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletFsm.FsmTransition;
import eu.mosaic_cloud.cloudlets.tools.DefaultConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnector;
import eu.mosaic_cloud.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackFunnelHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.callbacks.tools.CallbackCompletionDeferredFuture;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine.StateAndOutput;
import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.core.DeferredException;
import eu.mosaic_cloud.tools.exceptions.tools.QueuedExceptions;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


@SuppressWarnings ("synthetic-access")
public final class Cloudlet<TContext extends Object>
{
	@SuppressWarnings ("unchecked")
	private Cloudlet (final CloudletEnvironment environment)
	{
		super ();
		Preconditions.checkNotNull (environment);
		this.environment = environment;
		this.transcript = Transcript.create (this, true);
		this.exceptions = this.environment.createExceptionTracer (this.transcript);
		this.fsm = new CloudletFsm (this, this.transcript, this.exceptions);
		this.failures = QueueingExceptionTracer.create (this.exceptions);
		this.reactor = this.environment.getReactor ();
		try {
			TContext controllerContext;
			ICloudletCallback<TContext> controllerCallbacksDelegate; // NOPMD
			try {
				controllerContext = (TContext) this.environment.createContext ();
			} catch (final ReflectiveOperationException exception) {
				controllerContext = null; // NOPMD
				this.handleInternalFailure (null, new Error ()); // NOPMD
			}
			try {
				controllerCallbacksDelegate = (ICloudletCallback<TContext>) this.environment.createCloudletCallback ();
			} catch (final ReflectiveOperationException exception) {
				controllerCallbacksDelegate = null; // NOPMD
				this.handleInternalFailure (null, new Error ()); // NOPMD
			}
			this.controllerHandler = new ControllerHandler ();
			this.callbacksHandler = new CallbacksHandler ();
			this.callbacksDelegate = controllerCallbacksDelegate;
			this.controllerContext = controllerContext;
			this.genericCallbacksHandler = new GenericCallbacksHandler ();
			this.genericCallbacksDelegates = new ConcurrentHashMap<Callbacks, CallbackProxy> ();
			this.isolate = this.reactor.createIsolate ();
			this.controllerProxy = this.reactor.createProxy (ICloudletController.class);
			this.callbacksProxy = this.reactor.createProxy (ICloudletCallback.class);
			this.genericCallbacksProxies = new ConcurrentHashMap<CallbackProxy, Callbacks> ();
			this.connectorsFactory = new ConnectorsFactory ();
			this.connectorsFactoryDelegate = DefaultConnectorsFactory.create (this.controllerProxy, this.environment.getConnectors (), this.environment.getThreading (), this.exceptions);
			this.fsm.execute (FsmTransition.CreateCompleted, FsmState.Created);
		} catch (final CaughtException.Wrapper wrapper) {
			this.handleInternalFailure (null, wrapper.exception);
			throw (wrapper);
		} catch (final Throwable exception) { // NOPMD
			this.handleInternalFailure (null, exception);
			throw (new DeferredException (exception).wrap ());
		}
	}
	
	public CloudletState getState ()
	{
		return this.fsm.getState ().getCloudletState ();
	}
	
	public boolean await ()
	{
		return this.await (-1);
	}
	
	public boolean await (final long timeout)
	{
		return this.isolate.await (timeout);
	}
	
	public CallbackCompletion<Void> destroy ()
	{
		final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture.create (Void.class);
		this.isolate.enqueue (new Runnable () {
			@Override
			public final void run ()
			{
				Cloudlet.this.controllerHandler.destroy (future);
			}
		});
		return future.completion;
	}
	
	public CallbackCompletion<Void> initialize ()
	{
		final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture.create (Void.class);
		this.isolate.enqueue (new Runnable () {
			@Override
			public final void run ()
			{
				Cloudlet.this.controllerHandler.initialize (future);
			}
		});
		return future.completion;
	}
	
	protected CallbackCompletion<Void> enqueueTask (final Runnable task)
	{
		return this.isolate.enqueue (task);
	}
	
	private <Callback extends ICallback<?>> Callback createGenericCallbacksProxy (final Class<Callback> callbacksClass, final Callback callbacksDelegate)
	{
		{
			// FIXME: the same callback should be allowed to be used twice
			final Callback callbackProxy = callbacksClass.cast (this.genericCallbacksDelegates.get (callbacksDelegate));
			if (callbackProxy != null) {
				return callbackProxy;
			}
		}
		{
			final Callback callbacksProxy = this.reactor.createProxy (callbacksClass);
			final Callback callbacksProxy1 = callbacksClass.cast (this.genericCallbacksDelegates.putIfAbsent (callbacksDelegate, (CallbackProxy) callbacksProxy));
			Preconditions.checkState (callbacksProxy1 == null);
			this.genericCallbacksProxies.put ((CallbackProxy) callbacksProxy, callbacksDelegate);
			this.reactor.assignHandler (callbacksProxy, this.genericCallbacksHandler, this.isolate);
			return callbacksProxy;
		}
	}
	
	private CloudletEnvironment getEnvironment ()
	{
		return this.environment;
	}
	
	private void handleCleanup (final boolean gracefully)
	{ // NOPMD
		if ((this.controllerProxy != null) && !gracefully) {
			try {
				this.reactor.destroyProxy (this.controllerProxy);
			} catch (final Throwable exception) { // NOPMD
				this.exceptions.traceIgnoredException (exception);
			}
		}
		if ((this.callbacksProxy != null) && !gracefully) {
			try {
				this.reactor.destroyProxy (this.callbacksProxy);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		if ((this.genericCallbacksProxies != null) && !gracefully) {
			for (final CallbackProxy genericCallbacksProxy : this.genericCallbacksProxies.keySet ()) {
				try {
					this.reactor.destroyProxy (genericCallbacksProxy);
				} catch (final Throwable exception) {
					this.exceptions.traceIgnoredException (exception);
				}
			}
		}
		if (this.isolate != null) {
			try {
				this.reactor.destroyIsolate (this.isolate);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception);
			}
		}
		if (this.initializeFuture != null) {
			Preconditions.checkState (this.destroyFuture == null);
			this.initializeFuture.trigger.triggerFailed (QueuedExceptions.create (this.failures));
			this.initializeFuture = null; // NOPMD
		}
		if (this.destroyFuture != null) {
			Preconditions.checkState (this.initializeFuture == null);
			this.destroyFuture.trigger.triggerFailed (QueuedExceptions.create (this.failures.queue));
			this.destroyFuture = null; // NOPMD
		}
	}
	
	private void handleDelegateFailure (final Throwable exception)
	{
		this.fsm.new FsmVoidAccess () {
			@Override
			protected Void execute ()
			{
				Cloudlet.this.failures.traceHandledException (exception);
				if (Cloudlet.this.fsm.hasState (FsmState.Active)) {
					Cloudlet.this.controllerProxy.destroy ();
				}
				return null;
			}
		}.trigger ();
	}
	
	private void handleInternalFailure (final Callbacks proxy, final Throwable exception)
	{
		this.fsm.new FsmVoidTransaction (FsmTransition.InternalFailure) {
			@Override
			protected StateAndOutput<FsmState, Void> execute ()
			{
				Cloudlet.this.failures.traceHandledException (exception, "failed proxy `%{object:identity}`; aborting!", proxy);
				if (Cloudlet.this.fsm.hasState (FsmState.Failed)) {
					return StateAndOutput.create (FsmState.Failed, null);
				}
				Cloudlet.this.handleCleanup (false);
				return StateAndOutput.create (FsmState.Failed, null);
			}
		}.trigger ();
	}
	
	public static <Context extends Object> Cloudlet<Context> create (final CloudletEnvironment environment)
	{
		return new Cloudlet<Context> (environment);
	}
	
	private final ICloudletCallback<TContext> callbacksDelegate;
	private final CallbacksHandler callbacksHandler;
	private final ICloudletCallback<TContext> callbacksProxy;
	private final ConnectorsFactory connectorsFactory;
	private final IConnectorsFactory connectorsFactoryDelegate; // NOPMD
	private final TContext controllerContext;
	private final ControllerHandler controllerHandler;
	private final ICloudletController<TContext> controllerProxy;
	private CallbackCompletionDeferredFuture<Void> destroyFuture;
	private final CloudletEnvironment environment;
	private final Transcript transcript;
	private final TranscriptExceptionTracer exceptions;
	private final QueueingExceptionTracer failures;
	private final CloudletFsm fsm;
	private final ConcurrentHashMap<Callbacks, CallbackProxy> genericCallbacksDelegates; // NOPMD
	private final GenericCallbacksHandler genericCallbacksHandler; // NOPMD
	private final ConcurrentHashMap<CallbackProxy, Callbacks> genericCallbacksProxies; // NOPMD
	private CallbackCompletionDeferredFuture<Void> initializeFuture;
	private final CallbackIsolate isolate;
	private final CallbackReactor reactor;
	
	final class CallbacksHandler
			implements
				ICloudletCallback<TContext>,
				CallbackHandler
	{
		@Override
		public CallbackCompletion<Void> destroy (final TContext context, final CloudletCallbackArguments<TContext> arguments)
		{
			try {
				return Cloudlet.this.callbacksDelegate.destroy (context, arguments);
			} catch (final Throwable exception) { // NOPMD
				Cloudlet.this.handleDelegateFailure (exception);
				return CallbackCompletion.createFailure (exception);
			}
		}
		
		@Override
		public CallbackCompletion<Void> destroyFailed (final TContext context, final CloudletCallbackCompletionArguments<TContext> arguments)
		{
			try {
				return Cloudlet.this.callbacksDelegate.destroyFailed (context, arguments);
			} catch (final Throwable exception) { // NOPMD
				Cloudlet.this.handleDelegateFailure (exception);
				return CallbackCompletion.createFailure (exception);
			}
		}
		
		@Override
		public CallbackCompletion<Void> destroySucceeded (final TContext context, final CloudletCallbackCompletionArguments<TContext> arguments)
		{
			try {
				return Cloudlet.this.callbacksDelegate.destroySucceeded (context, arguments);
			} catch (final Throwable exception) { // NOPMD
				Cloudlet.this.handleDelegateFailure (exception);
				return CallbackCompletion.createFailure (exception);
			}
		}
		
		@Override
		public void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (proxy == Cloudlet.this.callbacksProxy);
			Cloudlet.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public CallbackCompletion<Void> initialize (final TContext context, final CloudletCallbackArguments<TContext> arguments)
		{
			try {
				return Cloudlet.this.callbacksDelegate.initialize (context, arguments);
			} catch (final Throwable exception) { // NOPMD
				Cloudlet.this.handleDelegateFailure (exception);
				return CallbackCompletion.createFailure (exception);
			}
		}
		
		@Override
		public CallbackCompletion<Void> initializeFailed (final TContext context, final CloudletCallbackCompletionArguments<TContext> arguments)
		{
			try {
				return Cloudlet.this.callbacksDelegate.initializeFailed (context, arguments);
			} catch (final Throwable exception) { // NOPMD
				Cloudlet.this.handleDelegateFailure (exception);
				return CallbackCompletion.createFailure (exception);
			}
		}
		
		@Override
		public CallbackCompletion<Void> initializeSucceeded (final TContext context, final CloudletCallbackCompletionArguments<TContext> arguments)
		{
			try {
				return Cloudlet.this.callbacksDelegate.initializeSucceeded (context, arguments);
			} catch (final Throwable exception) { // NOPMD
				Cloudlet.this.handleDelegateFailure (exception);
				return CallbackCompletion.createFailure (exception);
			}
		}
		
		@Override
		public void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			final FsmCallbackCompletionTransaction initializeSucceededCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksInitializeSucceededCompleted) { // NOPMD
						@Override
						protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> initializedCompletion)
						{ // NOPMD
							Preconditions.checkState (Cloudlet.this.initializeFuture != null);
							final Throwable exception = initializedCompletion.getException ();
							StateAndOutput<FsmState, Void> result;
							if (exception == null) {
								Cloudlet.this.initializeFuture.trigger.triggerSucceeded (null);
								Cloudlet.this.initializeFuture = null;
								result = StateAndOutput.create (FsmState.Active, null);
							} else {
								Cloudlet.this.failures.traceHandledException (exception);
								Cloudlet.this.initializeFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures));
								Cloudlet.this.initializeFuture = null;
								Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
								result = StateAndOutput.create (FsmState.CallbacksUnregisterPending, null);
							}
							return result;
						}
					};
			final FsmCallbackCompletionTransaction initializeFailedCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksInitializeFailedCompleted) { // NOPMD
						@Override
						protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> initializedCompletion)
						{ // NOPMD
							Preconditions.checkState (Cloudlet.this.initializeFuture != null);
							Cloudlet.this.initializeFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures));
							Cloudlet.this.initializeFuture = null;
							final Throwable exception = initializedCompletion.getException ();
							if (exception != null) {
								Cloudlet.this.failures.traceHandledException (exception);
							}
							Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
							return StateAndOutput.create (FsmState.CallbacksUnregisterPending, null);
						}
					};
			final FsmCallbackCompletionTransaction initializeCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksInitializeCompleted) { // NOPMD
						@Override
						protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> initializeCompletion)
						{ // NOPMD
							final Throwable exception = initializeCompletion.getException ();
							StateAndOutput<FsmState, Void> result;
							if (exception == null) {
								final CloudletCallbackCompletionArguments<TContext> arguments = new CloudletCallbackCompletionArguments<TContext> (Cloudlet.this.controllerProxy);
								final CallbackCompletion<Void> initializedCompletion = Cloudlet.this.callbacksProxy.initializeSucceeded (Cloudlet.this.controllerContext, arguments); // NOPMD
								initializeSucceededCompletedTransaction.observe (initializedCompletion);
								result = StateAndOutput.create (FsmState.CallbacksInitializeSucceededPending, null);
							} else {
								Cloudlet.this.failures.traceHandledException (exception);
								final CloudletCallbackCompletionArguments<TContext> arguments = new CloudletCallbackCompletionArguments<TContext> (Cloudlet.this.controllerProxy, exception);
								final CallbackCompletion<Void> initializedCompletion = Cloudlet.this.callbacksProxy.initializeFailed (Cloudlet.this.controllerContext, arguments); // NOPMD
								initializeFailedCompletedTransaction.observe (initializedCompletion);
								result = StateAndOutput.create (FsmState.CallbacksInitializeFailedPending, null);
							}
							return result;
						}
					};
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.CallbacksRegisterCompleted) {
				@Override
				public final StateAndOutput<FsmState, Void> execute ()
				{
					final CloudletCallbackArguments<TContext> arguments = new CloudletCallbackArguments<TContext> (Cloudlet.this.controllerProxy);
					final CallbackCompletion<Void> completion = Cloudlet.this.callbacksProxy.initialize (Cloudlet.this.controllerContext, arguments);
					initializeCompletedTransaction.observe (completion);
					return StateAndOutput.create (FsmState.CallbacksInitializePending, null);
				}
			}.trigger ();
		}
		
		@Override
		public void unregisteredCallbacks (final Callbacks proxy)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.CallbacksUnregisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					Cloudlet.this.reactor.destroyProxy (Cloudlet.this.controllerProxy);
					return StateAndOutput.create (FsmState.ControllerUnregisterPending, null);
				}
			}.trigger ();
		}
	}
	
	final class ConnectorFactory<TConnector extends IConnector, TFactory extends IConnectorFactory<? super TConnector>>
			// NOPMD
			implements
				InvocationHandler
	{
		ConnectorFactory (final Class<TFactory> factoryClass, final TFactory factoryDelegate)
		{
			super ();
			this.factoryClass = factoryClass;
			this.factoryDelegate = factoryDelegate;
			final CloudletEnvironment environment = Cloudlet.this.getEnvironment ();
			this.factoryProxy = this.factoryClass.cast (Proxy.newProxyInstance (environment.getClassLoader (), new Class<?>[] {this.factoryClass}, this));
		}
		
		@Override
		public Object invoke (final Object proxy, final Method method, final Object[] oldArguments)
				// NOPMD
				throws Throwable
		{
			Preconditions.checkState (proxy == this.factoryProxy);
			Preconditions.checkState (method != null);
			Preconditions.checkState (oldArguments != null);
			try {
				return Cloudlet.this.fsm.new FsmAccess<Void, Object> () {
					@SuppressWarnings ("unchecked")
					@Override
					protected final Object execute (final Void input)
					{
						final Object[] newArguments;
						if (IConnector.class.isAssignableFrom (method.getReturnType ())) {
							final Class<?>[] argumentTypes = method.getParameterTypes ();
							newArguments = new Object[oldArguments.length];
							for (int index = 0; index < oldArguments.length; index++) {
								final Class<?> argumentType = argumentTypes[index];
								final Object oldArgument = oldArguments[index];
								final Object newArgument;
								if ((oldArgument != null) && argumentType.isInterface () && ICallback.class.isAssignableFrom (argumentType)) {
									newArgument = Cloudlet.this.createGenericCallbacksProxy ((Class<ICallback<?>>) argumentType, (ICallback<?>) argumentType.cast (oldArgument));
								} else {
									newArgument = oldArgument;
								}
								newArguments[index] = newArgument;
							}
						} else {
							newArguments = oldArguments;
						}
						try {
							try {
								return method.invoke (ConnectorFactory.this.factoryDelegate, newArguments);
							} catch (final InvocationTargetException exception) {
								Cloudlet.this.exceptions.traceHandledException (exception);
								throw (exception.getCause ());
							}
						} catch (final CaughtException.Wrapper exception) { // NOPMD
							throw (exception);
						} catch (final Throwable exception) { // NOPMD
							throw (new DeferredException (exception).wrap ());
						}
					}
				}.trigger (null);
			} catch (final CaughtException.Wrapper wrapper) {
				wrapper.exception.trace (Cloudlet.this.exceptions);
				throw (wrapper.exception.caught);
			}
		}
		
		private final Class<TFactory> factoryClass;
		private final TFactory factoryDelegate;
		private final TFactory factoryProxy;
	}
	
	final class ConnectorsFactory
			implements
				IConnectorsFactory
	{
		ConnectorsFactory ()
		{
			super ();
			this.factories = new ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, ConnectorFactory<? extends IConnector, ? extends IConnectorFactory<?>>> ();
		}
		
		@Override
		public <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> Factory getConnectorFactory (final Class<Factory> factoryClass)
		{
			Preconditions.checkNotNull (factoryClass);
			Preconditions.checkArgument (factoryClass.isInterface ());
			Preconditions.checkArgument (IConnectorFactory.class.isAssignableFrom (factoryClass));
			try {
				return Cloudlet.this.fsm.new FsmAccess<Void, Factory> () {
					@Override
					protected final Factory execute (final Void input)
					{
						{
							final ConnectorFactory<?, ?> factory = ConnectorsFactory.this.factories.get (factoryClass);
							if (factory != null) {
								return factoryClass.cast (factory.factoryProxy);
							}
						}
						{
							final Factory factoryDelegate = Cloudlet.this.connectorsFactoryDelegate.getConnectorFactory (factoryClass);
							Preconditions.checkArgument (factoryDelegate != null);
							Preconditions.checkArgument (factoryClass.isInstance (factoryDelegate));
							final ConnectorFactory<Connector, Factory> factory = new ConnectorFactory<Connector, Factory> (factoryClass, factoryDelegate);
							final ConnectorFactory<?, ?> factory1 = ConnectorsFactory.this.factories.putIfAbsent (factoryClass, factory);
							Preconditions.checkState (factory1 == null);
							return factory.factoryProxy;
						}
					}
				}.trigger (null);
			} catch (final CaughtException.Wrapper exception) {
				exception.rethrow ();
				throw (new AssertionError ()); // NOPMD
			}
		}
		
		private final ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, ConnectorFactory<? extends IConnector, ? extends IConnectorFactory<?>>> factories;
	}
	
	final class ControllerHandler
			implements
				ICloudletController<TContext>,
				CallbackHandler
	{
		@Override
		public CallbackCompletion<Void> destroy ()
		{
			return Cloudlet.this.destroy ();
		}
		
		public void destroy (final CallbackCompletionDeferredFuture<Void> future)
		{
			// FIXME: There should be a better way to handle external destroys...
			switch (Cloudlet.this.fsm.getState ().getCloudletState ()) {
				case INITIALIZING :
				case DESTROYING :
				case DESTROYED :
				case FAILED :
					future.trigger.triggerSucceeded (null);
					return;
				default:
					break;
			}
			final FsmCallbackCompletionTransaction destroySucceededCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksDestroySucceededCompleted) { // NOPMD
						@Override
						protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> destroyedCompletion)
						{ // NOPMD
							final Throwable exception = destroyedCompletion.getException ();
							if (exception != null) {
								Cloudlet.this.failures.traceHandledException (exception);
							}
							Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
							return StateAndOutput.create (FsmState.CallbacksUnregisterPending, null);
						}
					};
			final FsmCallbackCompletionTransaction destroyFailedCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksDestroyFailedCompleted) { // NOPMD
						@Override
						protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> destroyedCompletion)
						{ // NOPMD
							final Throwable exception = destroyedCompletion.getException ();
							if (exception != null) {
								Cloudlet.this.failures.traceHandledException (exception);
							}
							Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
							return StateAndOutput.create (FsmState.CallbacksUnregisterPending, null);
						}
					};
			final FsmCallbackCompletionTransaction destroyCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksDestroyCompleted) { // NOPMD
						@Override
						protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> destroyCompletion)
						{
							final Throwable exception = destroyCompletion.getException ();
							StateAndOutput<FsmState, Void> result;
							if (exception == null) {
								final CloudletCallbackCompletionArguments<TContext> arguments = new CloudletCallbackCompletionArguments<TContext> (Cloudlet.this.controllerProxy);
								final CallbackCompletion<Void> destroyedCompletion = Cloudlet.this.callbacksProxy.destroySucceeded (Cloudlet.this.controllerContext, arguments); // NOPMD
								destroySucceededCompletedTransaction.observe (destroyedCompletion);
								result = StateAndOutput.create (FsmState.CallbacksDestroySucceededPending, null);
							} else {
								Cloudlet.this.failures.traceHandledException (exception);
								final CloudletCallbackCompletionArguments<TContext> arguments = new CloudletCallbackCompletionArguments<TContext> (Cloudlet.this.controllerProxy, exception);
								final CallbackCompletion<Void> destroyedCompletion = Cloudlet.this.callbacksProxy.destroyFailed (Cloudlet.this.controllerContext, arguments); // NOPMD
								destroyFailedCompletedTransaction.observe (destroyedCompletion);
								result = StateAndOutput.create (FsmState.CallbacksDestroyFailedPending, null);
							}
							return result;
						}
					};
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ExternalDestroy) {
				@Override
				public final StateAndOutput<FsmState, Void> execute ()
				{
					Preconditions.checkState (Cloudlet.this.destroyFuture == null);
					Cloudlet.this.destroyFuture = future;
					final CloudletCallbackArguments<TContext> arguments = new CloudletCallbackArguments<TContext> (Cloudlet.this.controllerProxy);
					final CallbackCompletion<Void> completion = Cloudlet.this.callbacksProxy.destroy (Cloudlet.this.controllerContext, arguments);
					destroyCompletedTransaction.observe (completion);
					return StateAndOutput.create (FsmState.CallbacksDestroyPending, null);
				}
			}.trigger ();
		}
		
		@Override
		public void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (proxy == Cloudlet.this.controllerProxy); // NOPMD
			Cloudlet.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public IConfiguration getConfiguration ()
		{
			return Cloudlet.this.environment.getConfiguration ();
		}
		
		@Override
		public <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> Factory getConnectorFactory (final Class<Factory> factory)
		{
			return Cloudlet.this.connectorsFactory.getConnectorFactory (factory);
		}
		
		@Override
		public CloudletState getState ()
		{
			return Cloudlet.this.fsm.getState ().getCloudletState ();
		}
		
		public void initialize (final CallbackCompletionDeferredFuture<Void> future)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ExternalInitialize) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute ()
				{
					Preconditions.checkState (Cloudlet.this.initializeFuture == null);
					Cloudlet.this.initializeFuture = future;
					Cloudlet.this.reactor.assignHandler (Cloudlet.this.controllerProxy, Cloudlet.this.controllerHandler, Cloudlet.this.isolate);
					return StateAndOutput.create (FsmState.ControllerRegisterPending, null);
				}
			}.trigger ();
		}
		
		@Override
		public void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ControllerRegisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					Cloudlet.this.reactor.assignHandler (Cloudlet.this.callbacksProxy, Cloudlet.this.callbacksHandler, Cloudlet.this.isolate);
					return StateAndOutput.create (FsmState.CallbacksRegisterPending, null);
				}
			}.trigger ();
		}
		
		@Override
		public void unregisteredCallbacks (final Callbacks proxy)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ControllerUnregisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					// FIXME: ...
					// Preconditions.checkState (Cloudlet.this.destroyFuture != null);
					StateAndOutput<FsmState, Void> result;
					if (Cloudlet.this.failures.queue.isEmpty ()) {
						if (Cloudlet.this.destroyFuture != null) {
							Cloudlet.this.destroyFuture.trigger.triggerSucceeded (null);
							Cloudlet.this.destroyFuture = null;
						}
						Cloudlet.this.handleCleanup (true);
						result = StateAndOutput.create (FsmState.Destroyed, null);
					} else {
						if (Cloudlet.this.destroyFuture != null) {
							Cloudlet.this.destroyFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures));
							Cloudlet.this.destroyFuture = null;
						}
						Cloudlet.this.handleCleanup (true);
						result = StateAndOutput.create (FsmState.Failed, null);
					}
					return result;
				}
			}.trigger ();
		}
	}
	
	final class GenericCallbacksHandler
			extends Object
			implements
				CallbackFunnelHandler
	{
		GenericCallbacksHandler ()
		{
			super ();
		}
		
		@Override
		public CallbackCompletion<?> executeCallback (final Callbacks proxy, final Method method, final Object[] arguments)
		{
			return Cloudlet.this.fsm.new FsmCallbackAccess () {
				@SuppressWarnings ("unchecked")
				@Override
				protected CallbackCompletion<Void> execute ()
				{
					final Callbacks delegate = Cloudlet.this.genericCallbacksProxies.get (proxy);
					Preconditions.checkState (delegate != null);
					try {
						try {
							return (CallbackCompletion<Void>) (method.invoke (delegate, arguments));
						} catch (final InvocationTargetException exception) {
							Cloudlet.this.exceptions.traceHandledException (exception);
							throw (exception.getCause ());
						}
					} catch (final Throwable exception) { // NOPMD
						Cloudlet.this.handleDelegateFailure (exception);
						return CallbackCompletion.createFailure (exception);
					}
				}
			}.trigger ();
		}
		
		@Override
		public void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (Cloudlet.this.genericCallbacksProxies.containsKey (proxy));
			Cloudlet.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			Cloudlet.this.fsm.new FsmVoidAccess () {
				@Override
				protected Void execute ()
				{
					Preconditions.checkState (Cloudlet.this.genericCallbacksProxies.containsKey (proxy));
					return null;
				}
			}.trigger ();
		}
		
		@Override
		public void unregisteredCallbacks (final Callbacks proxy)
		{
			Cloudlet.this.fsm.new FsmVoidAccess () {
				@Override
				protected Void execute ()
				{
					final Callbacks delegate = Cloudlet.this.genericCallbacksProxies.get (proxy);
					Preconditions.checkState (delegate != null);
					Preconditions.checkState (Cloudlet.this.genericCallbacksDelegates.remove (delegate) == proxy); // NOPMD
					Preconditions.checkState (Cloudlet.this.genericCallbacksProxies.remove (proxy) == delegate); // NOPMD
					return null;
				}
			}.trigger ();
		}
	}
}
