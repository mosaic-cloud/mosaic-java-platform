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

package eu.mosaic_cloud.cloudlets.runtime;


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
import eu.mosaic_cloud.cloudlets.runtime.CloudletFsm.FsmCallbackCompletionTransaction;
import eu.mosaic_cloud.cloudlets.runtime.CloudletFsm.FsmState;
import eu.mosaic_cloud.cloudlets.runtime.CloudletFsm.FsmTransition;
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
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


public final class Cloudlet<Context extends Object>
		extends Object
{
	@SuppressWarnings ("unchecked")
	private Cloudlet (final CloudletEnvironment environment)
	{
		super ();
		{
			Preconditions.checkNotNull (environment);
			this.environment = environment;
			this.fsm = new CloudletFsm (this);
		}
		{
			this.transcript = Transcript.create (this);
			this.exceptions = TranscriptExceptionTracer.create (this.transcript, this.environment.exceptions);
			this.failures = QueueingExceptionTracer.create (this.exceptions);
			this.reactor = this.environment.reactor;
			this.threading = this.environment.threading;
			this.classLoader = this.environment.classLoader;
			this.configuration = this.environment.configuration;
		}
		{
			Context controllerContext;
			ICloudletCallback<Context> controllerCallbacksDelegate;
			try {
				controllerContext = (Context) this.environment.cloudletContextClass.newInstance ();
			} catch (final ReflectiveOperationException exception) {
				controllerContext = null;
				this.handleInternalFailure (null, new Error ());
			}
			try {
				controllerCallbacksDelegate = (ICloudletCallback<Context>) this.environment.cloudletCallbackClass.newInstance ();
			} catch (final ReflectiveOperationException exception) {
				controllerCallbacksDelegate = null;
				this.handleInternalFailure (null, new Error ());
			}
			this.controllerHandler = new ControllerHandler ();
			this.callbacksHandler = new CallbacksHandler ();
			this.callbacksDelegate = controllerCallbacksDelegate;
			this.controllerContext = controllerContext;
			this.genericCallbacksHandler = new GenericCallbacksHandler ();
			this.genericCallbacksDelegates = new ConcurrentHashMap<Callbacks, CallbackProxy> ();
			this.connectorsFactory = new ConnectorsFactory ();
			this.connectorsFactoryDelegate = DefaultConnectorsFactory.create (this.controllerProxy, this.environment.connectors, this.threading, this.exceptions);
		}
		{
			this.isolate = this.reactor.createIsolate ();
			this.controllerProxy = this.reactor.createProxy (ICloudletController.class);
			this.callbacksProxy = this.reactor.createProxy (ICloudletCallback.class);
			this.genericCallbacksProxies = new ConcurrentHashMap<CallbackProxy, Callbacks> ();
		}
	}
	
	public final boolean await ()
	{
		return (this.await (-1));
	}
	
	public final boolean await (final long timeout)
	{
		return (this.isolate.await (timeout));
	}
	
	public final CallbackCompletion<Void> destroy ()
	{
		final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture.create (Void.class);
		this.isolate.enqueue (new Runnable () {
			@Override
			public final void run ()
			{
				Cloudlet.this.controllerHandler.destroy (future);
			}
		});
		return (future.completion);
	}
	
	public final CallbackCompletion<Void> initialize ()
	{
		final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture.create (Void.class);
		this.isolate.enqueue (new Runnable () {
			@Override
			public final void run ()
			{
				Cloudlet.this.controllerHandler.initialize (future);
			}
		});
		return (future.completion);
	}
	
	final <Callback extends ICallback<?>> Callback createGenericCallbacksProxy (final Class<Callback> callbacksClass, final Callback callbacksDelegate)
	{
		{
			// FIXME
			final Callback callbackProxy = callbacksClass.cast (this.genericCallbacksDelegates.get (callbacksDelegate));
			if (callbackProxy != null)
				return (callbackProxy);
		}
		{
			final Callback callbacksProxy = this.reactor.createProxy (callbacksClass);
			final Callback callbacksProxy1 = callbacksClass.cast (this.genericCallbacksDelegates.putIfAbsent (callbacksDelegate, (CallbackProxy) callbacksProxy));
			Preconditions.checkState (callbacksProxy1 == null);
			this.genericCallbacksProxies.put ((CallbackProxy) callbacksProxy, callbacksDelegate);
			this.reactor.assignHandler (callbacksProxy, this.genericCallbacksHandler, this.isolate);
			return (callbacksProxy);
		}
	}
	
	final void handleDelegateFailure (final Callbacks delegate, final Throwable exception)
	{
		this.fsm.new FsmVoidAccess () {
			@Override
			protected Void execute ()
			{
				Cloudlet.this.failures.traceHandledException (exception);
				if (Cloudlet.this.fsm.hasState (FsmState.Active)) {
					Cloudlet.this.controllerProxy.destroy ();
				}
				return (null);
			}
		}.trigger ();
	}
	
	final void handleInternalFailure (final Callbacks proxy, final Throwable exception)
	{
		this.fsm.new FsmVoidTransaction (FsmTransition.InternalFailure) {
			@Override
			protected StateAndOutput<FsmState, Void> execute ()
			{
				Cloudlet.this.failures.traceHandledException (exception);
				if (Cloudlet.this.fsm.hasState (FsmState.Failed)) {
					return (StateAndOutput.create (FsmState.Failed, null));
				}
				Cloudlet.this.reactor.destroyProxy (Cloudlet.this.controllerProxy);
				Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
				for (final CallbackProxy genericCallbacksProxy : Cloudlet.this.genericCallbacksProxies.keySet ()) {
					Cloudlet.this.reactor.destroyProxy (genericCallbacksProxy);
				}
				Cloudlet.this.reactor.destroyIsolate (Cloudlet.this.isolate);
				if (Cloudlet.this.initializeFuture != null) {
					Preconditions.checkState (Cloudlet.this.destroyFuture == null);
					Cloudlet.this.initializeFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures));
					Cloudlet.this.initializeFuture = null;
				}
				if (Cloudlet.this.destroyFuture != null) {
					Preconditions.checkState (Cloudlet.this.initializeFuture == null);
					Cloudlet.this.destroyFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures.queue));
					Cloudlet.this.destroyFuture = null;
				}
				return (StateAndOutput.create (FsmState.Failed, null));
			}
		}.trigger ();
	}
	
	public static final <Context extends Object> Cloudlet<Context> create (final CloudletEnvironment environment)
	{
		return (new Cloudlet<Context> (environment));
	}
	
	final ICloudletCallback<Context> callbacksDelegate;
	final CallbacksHandler callbacksHandler;
	final ICloudletCallback<Context> callbacksProxy;
	final ClassLoader classLoader;
	final IConfiguration configuration;
	final ConnectorsFactory connectorsFactory;
	final IConnectorsFactory connectorsFactoryDelegate;
	final Context controllerContext;
	final ControllerHandler controllerHandler;
	final ICloudletController<Context> controllerProxy;
	CallbackCompletionDeferredFuture<Void> destroyFuture;
	final CloudletEnvironment environment;
	final TranscriptExceptionTracer exceptions;
	final QueueingExceptionTracer failures;
	final CloudletFsm fsm;
	final ConcurrentHashMap<Callbacks, CallbackProxy> genericCallbacksDelegates;
	final GenericCallbacksHandler genericCallbacksHandler;
	final ConcurrentHashMap<CallbackProxy, Callbacks> genericCallbacksProxies;
	CallbackCompletionDeferredFuture<Void> initializeFuture;
	final CallbackIsolate isolate;
	final CallbackReactor reactor;
	final ThreadingContext threading;
	final Transcript transcript;
	
	final class CallbacksHandler
			extends Object
			implements
				ICloudletCallback<Context>,
				CallbackHandler
	{
		@Override
		public final CallbackCompletion<Void> destroy (final Context context, final CloudletCallbackArguments<Context> arguments)
		{
			try {
				return (Cloudlet.this.callbacksDelegate.destroy (context, arguments));
			} catch (final Throwable exception) {
				Cloudlet.this.handleDelegateFailure (this, exception);
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final CallbackCompletion<Void> destroyFailed (final Context context, final CloudletCallbackCompletionArguments<Context> arguments)
		{
			try {
				return (Cloudlet.this.callbacksDelegate.destroyFailed (context, arguments));
			} catch (final Throwable exception) {
				Cloudlet.this.handleDelegateFailure (this, exception);
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final CallbackCompletion<Void> destroySucceeded (final Context context, final CloudletCallbackCompletionArguments<Context> arguments)
		{
			try {
				return (Cloudlet.this.callbacksDelegate.destroySucceeded (context, arguments));
			} catch (final Throwable exception) {
				Cloudlet.this.handleDelegateFailure (this, exception);
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (proxy == Cloudlet.this.callbacksProxy);
			Cloudlet.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public final CallbackCompletion<Void> initialize (final Context context, final CloudletCallbackArguments<Context> arguments)
		{
			try {
				return (Cloudlet.this.callbacksDelegate.initialize (context, arguments));
			} catch (final Throwable exception) {
				Cloudlet.this.handleDelegateFailure (this, exception);
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final CallbackCompletion<Void> initializeFailed (final Context context, final CloudletCallbackCompletionArguments<Context> arguments)
		{
			try {
				return (Cloudlet.this.callbacksDelegate.initializeFailed (context, arguments));
			} catch (final Throwable exception) {
				Cloudlet.this.handleDelegateFailure (this, exception);
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final CallbackCompletion<Void> initializeSucceeded (final Context context, final CloudletCallbackCompletionArguments<Context> arguments)
		{
			try {
				return (Cloudlet.this.callbacksDelegate.initializeSucceeded (context, arguments));
			} catch (final Throwable exception) {
				Cloudlet.this.handleDelegateFailure (this, exception);
				return (CallbackCompletion.createFailure (exception));
			}
		}
		
		@Override
		public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			final FsmCallbackCompletionTransaction initializeSucceededCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksInitializeSucceededCompleted) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> initializedCompletion)
				{
					Preconditions.checkState (Cloudlet.this.initializeFuture != null);
					final Throwable exception = initializedCompletion.getException ();
					if (exception == null) {
						Cloudlet.this.initializeFuture.trigger.triggerSucceeded (null);
						Cloudlet.this.initializeFuture = null;
						return (StateAndOutput.create (FsmState.Active, null));
					} else {
						Cloudlet.this.failures.traceHandledException (exception);
						Cloudlet.this.initializeFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures));
						Cloudlet.this.initializeFuture = null;
						Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
						return (StateAndOutput.create (FsmState.CallbacksUnregisterPending, null));
					}
				}
			};
			final FsmCallbackCompletionTransaction initializeFailedCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksInitializeSucceededCompleted) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> initializedCompletion)
				{
					Preconditions.checkState (Cloudlet.this.initializeFuture != null);
					Cloudlet.this.initializeFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures));
					Cloudlet.this.initializeFuture = null;
					final Throwable exception = initializedCompletion.getException ();
					if (exception != null) {
						Cloudlet.this.failures.traceHandledException (exception);
					}
					Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
					return (StateAndOutput.create (FsmState.CallbacksUnregisterPending, null));
				}
			};
			final FsmCallbackCompletionTransaction initializeCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksInitializeCompleted) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> initializeCompletion)
				{
					final Throwable exception = initializeCompletion.getException ();
					if (exception == null) {
						final CloudletCallbackCompletionArguments<Context> arguments = new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.controllerProxy);
						final CallbackCompletion<Void> initializedCompletion = Cloudlet.this.callbacksProxy.initializeSucceeded (Cloudlet.this.controllerContext, arguments);
						initializeSucceededCompletedTransaction.observe (initializedCompletion);
						return (StateAndOutput.create (FsmState.CallbacksInitializeSucceededPending, null));
					} else {
						Cloudlet.this.failures.traceHandledException (exception);
						final CloudletCallbackCompletionArguments<Context> arguments = new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.controllerProxy, exception);
						final CallbackCompletion<Void> initializedCompletion = Cloudlet.this.callbacksProxy.initializeFailed (Cloudlet.this.controllerContext, arguments);
						initializeFailedCompletedTransaction.observe (initializedCompletion);
						return (StateAndOutput.create (FsmState.CallbacksInitializeFailedPending, null));
					}
				}
			};
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.CallbacksRegisterCompleted) {
				@Override
				public final StateAndOutput<FsmState, Void> execute ()
				{
					final CloudletCallbackArguments<Context> arguments = new CloudletCallbackArguments<Context> (Cloudlet.this.controllerProxy);
					final CallbackCompletion<Void> completion = Cloudlet.this.callbacksProxy.initialize (Cloudlet.this.controllerContext, arguments);
					initializeCompletedTransaction.observe (completion);
					return (StateAndOutput.create (FsmState.CallbacksInitializePending, null));
				}
			}.trigger ();
		}
		
		@Override
		public final void unregisteredCallbacks (final Callbacks proxy)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.CallbacksUnregisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					Cloudlet.this.reactor.destroyProxy (Cloudlet.this.controllerProxy);
					return (StateAndOutput.create (FsmState.ControllerUnregisterPending, null));
				}
			}.trigger ();
		}
	}
	
	final class ConnectorFactory<Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>>
			extends Object
			implements
				InvocationHandler
	{
		ConnectorFactory (final Class<Factory> factoryClass, final Factory factoryDelegate)
		{
			super ();
			this.factoryClass = factoryClass;
			this.factoryDelegate = factoryDelegate;
			this.factoryProxy = this.factoryClass.cast (Proxy.newProxyInstance (Cloudlet.this.classLoader, new Class<?>[] {this.factoryClass}, this));
		}
		
		@Override
		public Object invoke (final Object proxy, final Method method, final Object[] oldArguments)
				throws Throwable
		{
			Preconditions.checkState (proxy == this.factoryProxy);
			Preconditions.checkState (method != null);
			Preconditions.checkState (oldArguments != null);
			try {
				return (Cloudlet.this.fsm.new FsmAccess<Void, Object> () {
					@Override
					protected final Object execute (final Void input)
					{
						final Object[] newArguments;
						if (IConnector.class.isAssignableFrom (method.getReturnType ())) {
							final Class<?>[] argumentTypes = method.getParameterTypes ();
							newArguments = new Object[oldArguments.length];
							for (int index = 0; index < oldArguments.length; index++) {
								final Class<?> argumentType = argumentTypes[index];
								final Object argument = oldArguments[index];
								final Object newArgument;
								if ((argument != null) && argumentType.isInterface () && ICallback.class.isAssignableFrom (argumentType))
									newArgument = Cloudlet.this.createGenericCallbacksProxy ((Class<ICallback<?>>) argumentType, (ICallback<?>) argumentType.cast (argument));
								else
									newArgument = argument;
								newArguments[index] = newArgument;
							}
						}
						try {
							try {
								return (method.invoke (ConnectorFactory.this.factoryDelegate, oldArguments));
							} catch (final InvocationTargetException exception) {
								Cloudlet.this.exceptions.traceHandledException (exception);
								throw (exception.getCause ());
							}
						} catch (final CaughtException.Wrapper exception) {
							throw (exception);
						} catch (final Throwable exception) {
							throw (new DeferredException (exception).wrap ());
						}
					}
				}.trigger (null));
			} catch (final CaughtException.Wrapper wrapper) {
				wrapper.exception.trace (Cloudlet.this.exceptions);
				throw (wrapper.exception.caught);
			}
		}
		
		final Class<Factory> factoryClass;
		final Factory factoryDelegate;
		final Factory factoryProxy;
	}
	
	final class ConnectorsFactory
			extends Object
			implements
				IConnectorsFactory
	{
		ConnectorsFactory ()
		{
			super ();
			this.factories = new ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, Cloudlet<Context>.ConnectorFactory<? extends IConnector, ? extends IConnectorFactory<?>>> ();
		}
		
		@Override
		public final <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> Factory getConnectorFactory (final Class<Factory> factoryClass)
		{
			Preconditions.checkNotNull (factoryClass);
			Preconditions.checkArgument (factoryClass.isInterface ());
			Preconditions.checkArgument (IConnectorFactory.class.isAssignableFrom (factoryClass));
			try {
				return (Cloudlet.this.fsm.new FsmAccess<Void, Factory> () {
					@Override
					protected final Factory execute (final Void input)
					{
						{
							final ConnectorFactory<?, ?> factory = ConnectorsFactory.this.factories.get (factoryClass);
							if (factory != null)
								return (factoryClass.cast (factory.factoryProxy));
						}
						{
							final Factory factoryDelegate = Cloudlet.this.connectorsFactoryDelegate.getConnectorFactory (factoryClass);
							Preconditions.checkArgument (factoryDelegate != null);
							Preconditions.checkArgument (factoryClass.isInstance (factoryDelegate));
							final ConnectorFactory<Connector, Factory> factory = new ConnectorFactory<Connector, Factory> (factoryClass, factoryDelegate);
							final ConnectorFactory<?, ?> factory1 = ConnectorsFactory.this.factories.putIfAbsent (factoryClass, factory);
							Preconditions.checkState (factory1 == null);
							return (factory.factoryProxy);
						}
					}
				}).trigger (null);
			} catch (final CaughtException.Wrapper exception) {
				exception.rethrow ();
				throw (new AssertionError ());
			}
		}
		
		final ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, ConnectorFactory<? extends IConnector, ? extends IConnectorFactory<?>>> factories;
	}
	
	final class ControllerHandler
			extends Object
			implements
				ICloudletController<Context>,
				CallbackHandler
	{
		@Override
		public final CallbackCompletion<Void> destroy ()
		{
			return (Cloudlet.this.destroy ());
		}
		
		@Override
		public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (proxy == Cloudlet.this.controllerProxy);
			Cloudlet.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public final IConfiguration getConfiguration ()
		{
			return (Cloudlet.this.configuration);
		}
		
		@Override
		public final <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> Factory getConnectorFactory (final Class<Factory> factory)
		{
			return (Cloudlet.this.connectorsFactory.getConnectorFactory (factory));
		}
		
		@Override
		public final CloudletState getState ()
		{
			return (Cloudlet.this.fsm.getState ().getCloudletState ());
		}
		
		@Override
		public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ControllerRegisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					Cloudlet.this.reactor.assignHandler (Cloudlet.this.callbacksProxy, Cloudlet.this.callbacksHandler, isolate);
					return (StateAndOutput.create (FsmState.CallbacksRegisterPending, null));
				}
			}.trigger ();
		}
		
		@Override
		public final void unregisteredCallbacks (final Callbacks proxy)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ControllerUnregisterCompleted) {
				@Override
				protected StateAndOutput<FsmState, Void> execute ()
				{
					Preconditions.checkState (Cloudlet.this.destroyFuture != null);
					if (Cloudlet.this.failures.queue.isEmpty ()) {
						Cloudlet.this.destroyFuture.trigger.triggerSucceeded (null);
						Cloudlet.this.destroyFuture = null;
						// FIXME
						Cloudlet.this.reactor.destroyIsolate (Cloudlet.this.isolate);
						return (StateAndOutput.create (FsmState.Destroyed, null));
					} else {
						Cloudlet.this.destroyFuture.trigger.triggerFailed (QueuedExceptions.create (Cloudlet.this.failures));
						Cloudlet.this.destroyFuture = null;
						// FIXME
						Cloudlet.this.reactor.destroyIsolate (Cloudlet.this.isolate);
						return (StateAndOutput.create (FsmState.Failed, null));
					}
				}
			}.trigger ();
		}
		
		final void destroy (final CallbackCompletionDeferredFuture<Void> future)
		{
			final FsmCallbackCompletionTransaction destroySucceededCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksDestroySucceededCompleted) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> destroyedCompletion)
				{
					final Throwable exception = destroyedCompletion.getException ();
					if (exception != null) {
						Cloudlet.this.failures.traceHandledException (exception);
					}
					Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
					return (StateAndOutput.create (FsmState.CallbacksUnregisterPending, null));
				}
			};
			final FsmCallbackCompletionTransaction destroyFailedCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksDestroyFailedCompleted) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> destroyedCompletion)
				{
					final Throwable exception = destroyedCompletion.getException ();
					if (exception != null) {
						Cloudlet.this.failures.traceHandledException (exception);
					}
					Cloudlet.this.reactor.destroyProxy (Cloudlet.this.callbacksProxy);
					return (StateAndOutput.create (FsmState.CallbacksUnregisterPending, null));
				}
			};
			final FsmCallbackCompletionTransaction destroyCompletedTransaction = Cloudlet.this.fsm.new FsmCallbackCompletionTransaction (FsmTransition.CallbacksDestroyCompleted) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute (final CallbackCompletion<Void> destroyCompletion)
				{
					final Throwable exception = destroyCompletion.getException ();
					if (exception == null) {
						final CloudletCallbackCompletionArguments<Context> arguments = new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.controllerProxy);
						final CallbackCompletion<Void> destroyedCompletion = Cloudlet.this.callbacksProxy.destroySucceeded (Cloudlet.this.controllerContext, arguments);
						destroySucceededCompletedTransaction.observe (destroyedCompletion);
						return (StateAndOutput.create (FsmState.CallbacksDestroySucceededPending, null));
					} else {
						Cloudlet.this.failures.traceHandledException (exception);
						final CloudletCallbackCompletionArguments<Context> arguments = new CloudletCallbackCompletionArguments<Context> (Cloudlet.this.controllerProxy, exception);
						final CallbackCompletion<Void> destroyedCompletion = Cloudlet.this.callbacksProxy.destroyFailed (Cloudlet.this.controllerContext, arguments);
						destroyFailedCompletedTransaction.observe (destroyedCompletion);
						return (StateAndOutput.create (FsmState.CallbacksDestroyFailedPending, null));
					}
				}
			};
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ExternalDestroy) {
				@Override
				public final StateAndOutput<FsmState, Void> execute ()
				{
					Preconditions.checkState (Cloudlet.this.destroyFuture == null);
					Cloudlet.this.destroyFuture = future;
					final CloudletCallbackArguments<Context> arguments = new CloudletCallbackArguments<Context> (Cloudlet.this.controllerProxy);
					final CallbackCompletion<Void> completion = Cloudlet.this.callbacksProxy.destroy (Cloudlet.this.controllerContext, arguments);
					destroyCompletedTransaction.observe (completion);
					return (StateAndOutput.create (FsmState.CallbacksDestroyPending, null));
				}
			}.trigger ();
		}
		
		final void initialize (final CallbackCompletionDeferredFuture<Void> future)
		{
			Cloudlet.this.fsm.new FsmVoidTransaction (FsmTransition.ExternalInitialize) {
				@Override
				protected final StateAndOutput<FsmState, Void> execute ()
				{
					Preconditions.checkState (Cloudlet.this.initializeFuture == null);
					Cloudlet.this.initializeFuture = future;
					Cloudlet.this.reactor.assignHandler (Cloudlet.this.controllerProxy, Cloudlet.this.controllerHandler, Cloudlet.this.isolate);
					return (StateAndOutput.create (FsmState.ControllerRegisterPending, null));
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
		public final CallbackCompletion<?> executeCallback (final Callbacks proxy, final Method method, final Object[] arguments)
		{
			return (Cloudlet.this.fsm.new FsmCallbackAccess () {
				@Override
				protected CallbackCompletion<Void> execute ()
				{
					final Callbacks delegate = Cloudlet.this.genericCallbacksDelegates.get (proxy);
					Preconditions.checkState (delegate != null);
					try {
						try {
							return ((CallbackCompletion) (method.invoke (delegate, arguments)));
						} catch (final InvocationTargetException exception) {
							Cloudlet.this.exceptions.traceHandledException (exception);
							throw (exception.getCause ());
						}
					} catch (final Throwable exception) {
						Cloudlet.this.handleDelegateFailure (delegate, exception);
						return (CallbackCompletion.createFailure (exception));
					}
				}
			}.trigger ());
		}
		
		@Override
		public final void failedCallbacks (final Callbacks proxy, final Throwable exception)
		{
			Preconditions.checkState (Cloudlet.this.genericCallbacksProxies.containsKey (proxy));
			Cloudlet.this.handleInternalFailure (proxy, exception);
		}
		
		@Override
		public final void registeredCallbacks (final Callbacks proxy, final CallbackIsolate isolate)
		{
			Cloudlet.this.fsm.new FsmVoidAccess () {
				@Override
				protected Void execute ()
				{
					Preconditions.checkState (Cloudlet.this.genericCallbacksProxies.containsKey (proxy));
					return (null);
				}
			}.trigger ();
		}
		
		@Override
		public final void unregisteredCallbacks (final Callbacks proxy)
		{
			Cloudlet.this.fsm.new FsmVoidAccess () {
				@Override
				protected Void execute ()
				{
					final Callbacks delegate = Cloudlet.this.genericCallbacksProxies.get (proxy);
					Preconditions.checkState (delegate != null);
					Preconditions.checkState (Cloudlet.this.genericCallbacksDelegates.remove (delegate) == proxy);
					Preconditions.checkState (Cloudlet.this.genericCallbacksProxies.remove (proxy) == delegate);
					return (null);
				}
			}.trigger ();
		}
	}
}
