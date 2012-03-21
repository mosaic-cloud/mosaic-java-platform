/*
 * #%L
 * mosaic-tools-callbacks
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

package eu.mosaic_cloud.tools.callbacks.implementations.basic;


import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCanceled;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionBackend;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackFunnelHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolateBackend;
import eu.mosaic_cloud.tools.callbacks.core.CallbackPassthrough;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;


public final class BasicCallbackReactor
		extends Object
		implements
			CallbackReactor
{
	BasicCallbackReactor (final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		super ();
		this.reactor = new Reactor (this, threading, exceptions);
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackCompletion<Void> assignDelegate (final _Callbacks_ proxy, final _Callbacks_ delegate)
	{
		return (this.reactor.triggerAssign (proxy, delegate));
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackCompletion<Void> assignHandler (final _Callbacks_ proxy, final CallbackHandler handler, final CallbackIsolate isolate)
	{
		return (this.reactor.triggerAssign (proxy, handler, isolate));
	}
	
	@Override
	public final boolean await ()
	{
		return (this.await (0));
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		return (this.reactor.await (timeout));
	}
	
	@Override
	public final CallbackIsolate createIsolate ()
	{
		return (this.reactor.createIsolate ());
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> _Callbacks_ createProxy (final Class<_Callbacks_> specification)
	{
		return (this.reactor.createProxy (specification));
	}
	
	public final void destroy ()
	{
		Preconditions.checkState (this.destroy (-1));
	}
	
	public final boolean destroy (final long timeout)
	{
		this.reactor.triggerDestroy ();
		return (this.reactor.await (timeout));
	}
	
	@Override
	public final CallbackCompletion<Void> destroyIsolate (final CallbackIsolate isolate)
	{
		return (this.reactor.triggerDestroyIsolate (isolate));
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackCompletion<Void> destroyProxy (final _Callbacks_ proxy)
	{
		return (this.reactor.triggerDestroyProxy (proxy));
	}
	
	public final void initialize ()
	{
		Preconditions.checkState (this.initialize (-1));
	}
	
	public final boolean initialize (final long timeout)
	{
		Preconditions.checkArgument (timeout >= -1);
		return (true);
	}
	
	public static final BasicCallbackReactor create (final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new BasicCallbackReactor (threading, exceptions));
	}
	
	final Reactor reactor;
	
	static abstract class Action<_Target_ extends ActionTarget, _Outcome_ extends Object>
			extends Object
	{
		Action (final Reactor reactor, final _Target_ target, final Future<_Outcome_> future)
		{
			super ();
			this.reactor = reactor;
			this.target = target;
			this.future = future;
		}
		
		final Future<_Outcome_> future;
		final Reactor reactor;
		final _Target_ target;
	}
	
	interface ActionTarget
	{}
	
	static final class Actor<_Callbacks_ extends Callbacks>
			extends Object
			implements
				ActionTarget,
				InvocationHandler
	{
		Actor (final Reactor reactor, final Class<_Callbacks_> specification)
		{
			super ();
			this.monitor = Monitor.create (this);
			synchronized (this.monitor) {
				this.reactor = reactor;
				this.specification = specification;
				this.destroyFuture = new Future<Void> (this.reactor);
				this.proxy = (CallbackProxy) Proxy.newProxyInstance (specification.getClassLoader (), new Class[] {specification, CallbackProxy.class}, this);
				this.actions = new ConcurrentLinkedQueue<ActorAction<?>> ();
				this.handler = Atomics.newReference (null);
				this.scheduler = Atomics.newReference (null);
				this.delegate = Atomics.newReference (null);
				this.failed = Atomics.newReference (null);
				this.assignAction = Atomics.newReference (null);
				this.destroyAction = Atomics.newReference (null);
				this.handlerStatus = Atomics.newReference (HandlerStatus.Unassigned);
				this.scheduleStatus = Atomics.newReference (ScheduleStatus.Idle);
				this.status = Atomics.newReference (Status.Active);
				this.reactor.registerActor (this);
				this.reactor.transcript.traceDebugging ("created proxy `%{object:identity}` (owned by actor `%{object:identity}` from reactor `%{object:identity}`).", this.proxy, this, this.reactor.facade);
			}
		}
		
		@Override
		public final Object invoke (final Object callbacks, final Method method, final Object[] arguments)
				throws Throwable
		{
			if (method.getDeclaringClass () == Object.class)
				try {
					return (method.invoke (this, arguments));
				} catch (final InvocationTargetException exception) {
					this.reactor.exceptions.traceHandledException (exception);
					throw (exception.getCause ());
				}
			// FIXME
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("invocking (triggered) for proxy `%{object:identity}` the method `%{method}` with arguments `%{array}`...", this.proxy, method, arguments);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				if (this.handlerStatus.get () != HandlerStatus.Delegated) {
					if (method.getReturnType () == CallbackCompletion.class) {
						final ActorCallbackAction<?> action = new ActorCallbackAction<Object> (this, method, arguments);
						this.enqueueAction (action);
						return (action.future.completion);
					}
					// FIXME
					if (method.isAnnotationPresent (CallbackPassthrough.class)) {
						if (this.handlerStatus.get () == HandlerStatus.Assigned)
							try {
								return (method.invoke (this.handler.get (), arguments));
							} catch (final InvocationTargetException exception) {
								this.reactor.exceptions.traceHandledException (exception);
								throw (exception.getCause ());
							}
						throw (new IllegalAccessError ());
					}
					throw (new IllegalAccessError ());
				}
			}
			return (this.delegateInvokeCallback (method, arguments, null));
		}
		
		final void delegateInvokeCallback (final ActorCallbackAction<?> action)
		{
			this.delegateInvokeCallback (action.method, action.arguments, action.future);
		}
		
		final CallbackCompletion<?> delegateInvokeCallback (final Method method, final Object[] arguments, final Future<?> chainedFuture)
		{
			final Object delegate = this.delegate.get ();
			Preconditions.checkState (delegate != null);
			this.reactor.transcript.traceDebugging ("invocking method callback on delegate `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) the method `%{method}` with arguments `%{array}`...", delegate, this.proxy, this, method, arguments);
			final CallbackCompletion<?> returnedCompletion;
			final CallbackCompletion<?> finalCompletion;
			try {
				try {
					returnedCompletion = (CallbackCompletion<?>) method.invoke (delegate, arguments);
				} catch (final InvocationTargetException exception) {
					this.reactor.exceptions.traceHandledException (exception);
					throw (exception.getCause ());
				}
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceDeferredException (exception);
				if (chainedFuture != null) {
					chainedFuture.triggerFailure (exception);
					finalCompletion = chainedFuture.completion;
				} else
					finalCompletion = CallbackCompletion.createFailure (exception);
				this.triggerFailure (exception);
				return (finalCompletion);
			}
			if (chainedFuture != null) {
				if (returnedCompletion != null) {
					((Future<Object>) chainedFuture).triggerChain ((CallbackCompletion<Object>) returnedCompletion);
				} else
					chainedFuture.triggerSuccess (null);
				finalCompletion = chainedFuture.completion;
			} else if (returnedCompletion != null)
				finalCompletion = returnedCompletion;
			else
				finalCompletion = CallbackCompletion.createOutcome ();
			return (finalCompletion);
		}
		
		final void enqueueAction (final ActorAction<?> action)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("enqueueing action `%{object}` on actor `%{object:identity}`...", action, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				if (action instanceof ActorCallbackAction) {
					Preconditions.checkState (this.actions.offer (action));
					this.schedule (false);
				} else if (action instanceof ActorAssignAction) {
					Preconditions.checkState (this.assignAction.compareAndSet (null, (ActorAssignAction) action));
					this.schedule (true);
				} else if (action instanceof ActorDestroyAction) {
					Preconditions.checkState (this.destroyAction.compareAndSet (null, (ActorDestroyAction) action));
					this.schedule (true);
				} else
					throw (new IllegalStateException ());
			}
		}
		
		final void executeActions ()
		{
			this.reactor.transcript.traceDebugging ("executing enqueued actions on actor `%{object:identity}`...", this);
			Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Scheduled, ScheduleStatus.Running));
			while (true) {
				boolean reschedule = false;
				if ((this.assignAction.get () != null) && (this.failed.get () == null)) {
					this.executeAssign ();
					reschedule |= true;
				}
				while (true) {
					if (this.failed.get () != null)
						break;
					final ActorAction<?> action = this.actions.poll ();
					if (action == null)
						break;
					if (action instanceof ActorCallbackAction) {
						this.executeInvokeCallback ((ActorCallbackAction<?>) action);
						reschedule |= true;
					} else
						throw (new IllegalStateException ());
				}
				if (this.destroyAction.get () != null) {
					this.executeDestroy ();
					reschedule |= true;
				}
				if (!reschedule)
					break;
			}
			Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Running, ScheduleStatus.Idle));
			this.reactor.transcript.traceDebugging ("executed enqueued actions on actor `%{object:identity}`...", this);
		}
		
		final void executeAssign ()
		{
			synchronized (this.monitor) {
				final ActorAssignAction action = this.assignAction.get ();
				Preconditions.checkState (action != null);
				this.reactor.transcript.traceDebugging ("executing action `%{object}` on actor `%{object:identity}`...", action, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.handlerStatus.get () == HandlerStatus.Registering);
				final CallbackHandler handler = this.handler.get ();
				Preconditions.checkState ((handler != null) && (handler == action.handler));
				final Scheduler scheduler = this.scheduler.get ();
				Preconditions.checkState ((scheduler != null) && (scheduler == action.scheduler));
				this.reactor.transcript.traceDebugging ("assigning handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
				this.reactor.transcript.traceDebugging ("invocking register callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
				try {
					handler.registeredCallbacks (this.specification.cast (this.proxy), scheduler.isolate);
				} catch (final Throwable exception) {
					this.reactor.exceptions.traceDeferredException (exception);
					this.triggerFailure (exception);
					return;
				}
				action.future.triggerSuccess (null);
				Preconditions.checkState (this.assignAction.compareAndSet (action, null));
				Preconditions.checkState (this.handlerStatus.compareAndSet (HandlerStatus.Registering, HandlerStatus.Assigned));
				this.reactor.transcript.traceDebugging ("assigned handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`).", handler, this.proxy, this, scheduler.isolate, scheduler);
				this.reactor.transcript.traceDebugging ("executed action `%{object}` on actor `%{object:identity}`...", action, this);
			}
		}
		
		final void executeDestroy ()
		{
			synchronized (this.monitor) {
				final ActorDestroyAction destroy = this.destroyAction.get ();
				Preconditions.checkState (destroy != null);
				this.reactor.transcript.traceDebugging ("executing action `%{object}` on actor `%{object:identity}`...", destroy, this);
				Preconditions.checkState (this.status.get () == Status.Destroying);
				switch (this.handlerStatus.get ()) {
					case Unassigned :
					case Registering :
					case Assigned :
					case Delegated :
						break;
					default:
						throw (new IllegalStateException ());
				}
				this.reactor.transcript.traceDebugging ("destroying proxy `%{object:identity}` (owned by actor `%{object:identity}`)...", this.proxy, this);
				final Throwable failure = this.failed.get ();
				if (failure == null) {
					final ActorAssignAction assign = this.assignAction.get ();
					final CallbackHandler handler = this.handler.get ();
					if ((handler != null) && (assign == null)) {
						final Scheduler scheduler = this.scheduler.get ();
						Preconditions.checkState (scheduler != null);
						this.reactor.transcript.traceDebugging ("invocking unregister callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
						try {
							handler.unregisteredCallbacks (this.specification.cast (this.proxy));
						} catch (final Throwable exception) {
							this.reactor.exceptions.traceDeferredException (exception);
							this.triggerFailure (exception);
							return;
						}
						scheduler.unregisterActor (this);
						Preconditions.checkState (this.handler.compareAndSet (handler, null));
						Preconditions.checkState (this.scheduler.compareAndSet (scheduler, null));
					} else {
						if (assign != null) {
							assign.future.triggerFailure (new CallbackCanceled ());
							Preconditions.checkState (this.assignAction.compareAndSet (assign, null));
						}
						for (final ActorAction<?> action : this.actions)
							action.future.triggerFailure (new CallbackCanceled ());
						this.actions.clear ();
					}
				} else {
					final ActorAssignAction assign = this.assignAction.get ();
					final CallbackHandler handler = this.handler.get ();
					if (handler != null) {
						final Scheduler scheduler = this.scheduler.get ();
						Preconditions.checkState (scheduler != null);
						this.reactor.transcript.traceDebugging ("invocking failure callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
						try {
							handler.failedCallbacks (this.specification.cast (this.proxy), failure);
						} catch (final Throwable exception) {
							this.reactor.exceptions.traceIgnoredException (exception);
						}
						scheduler.unregisterActor (this);
						Preconditions.checkState (this.handler.compareAndSet (handler, null));
						Preconditions.checkState (this.scheduler.compareAndSet (scheduler, null));
					}
					if (assign != null) {
						assign.future.triggerFailure (failure);
						Preconditions.checkState (this.assignAction.compareAndSet (assign, null));
					}
					for (final ActorAction<?> action : this.actions)
						action.future.triggerFailure (failure);
					this.actions.clear ();
				}
				Preconditions.checkState (this.assignAction.get () == null);
				Preconditions.checkState (this.handler.get () == null);
				Preconditions.checkState (this.scheduler.get () == null);
				this.reactor.unregisterActor (this);
				if (failure == null)
					destroy.future.triggerSuccess (null);
				else
					destroy.future.triggerFailure (failure);
				Preconditions.checkState (this.destroyAction.compareAndSet (destroy, null));
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
				this.reactor.transcript.traceDebugging ("destroyed proxy `%{object:identity}` (owned by actor `%{object:identity}`).", this.proxy, this);
				this.reactor.transcript.traceDebugging ("executed action `%{object}` on actor `%{object:identity}`.", destroy, this);
			}
		}
		
		final void executeInvokeCallback (final ActorCallbackAction<?> action)
		{
			Preconditions.checkState (action != null);
			this.reactor.transcript.traceDebugging ("executing action `%{object}` on actor `%{object:identity}`...", action, this);
			final CallbackHandler handler;
			final Scheduler scheduler;
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.handlerStatus.get () == HandlerStatus.Assigned);
				handler = this.handler.get ();
				Preconditions.checkState (handler != null);
				scheduler = this.scheduler.get ();
				Preconditions.checkState (scheduler != null);
			}
			this.reactor.transcript.traceDebugging ("invocking method callback on handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`) the method `%{method}` with arguments `%{array}`...", handler, this.proxy, this, scheduler.isolate, scheduler, action.method, action.arguments);
			final CallbackCompletion<?> returnedCompletion;
			try {
				try {
					if (handler instanceof CallbackFunnelHandler)
						returnedCompletion = (CallbackCompletion<?>) ((CallbackFunnelHandler) handler).executeCallback (this.proxy, action.method, action.arguments);
					else
						returnedCompletion = (CallbackCompletion<?>) action.method.invoke (handler, action.arguments);
				} catch (final InvocationTargetException exception) {
					this.reactor.exceptions.traceHandledException (exception);
					throw (exception.getCause ());
				}
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceDeferredException (exception);
				action.future.triggerFailure (exception);
				this.triggerFailure (exception);
				return;
			}
			if (returnedCompletion != null)
				((Future<Object>) action.future).triggerChain ((CallbackCompletion<Object>) returnedCompletion);
			else
				action.future.triggerSuccess (null);
			this.reactor.transcript.traceDebugging ("executed action `%{object}` on actor `%{object:identity}`...", action, this);
		}
		
		final void schedule (final boolean force)
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				if (!force) {
					switch (this.handlerStatus.get ()) {
						case Assigned :
						case Registering :
							break;
						case Unassigned :
							return;
						default:
							throw (new IllegalStateException ());
					}
					if (this.handler.get () == null)
						return;
				}
				if ((this.scheduleStatus.get () == ScheduleStatus.Scheduled) || (this.scheduleStatus.get () == ScheduleStatus.Running))
					return;
				final Scheduler scheduler = this.scheduler.get ();
				Preconditions.checkNotNull (scheduler);
				Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Idle, ScheduleStatus.Scheduled));
				scheduler.enqueueActor (this);
				this.reactor.transcript.traceDebugging ("scheduled actor `%{object:identity}`.", this);
			}
		}
		
		final CallbackCompletion<Void> triggerAssign (final CallbackHandler handler, final Scheduler scheduler)
		{
			Preconditions.checkNotNull (scheduler);
			Preconditions.checkNotNull (handler);
			Preconditions.checkArgument (this.specification.isInstance (handler) || CallbackFunnelHandler.class.isInstance (handler));
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("assigning (triggered) handler `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`) backed by isolate `%{object:identity}` (owned by scheduler `%{object:identity}`)...", handler, this.proxy, this, scheduler.isolate, scheduler);
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.handlerStatus.compareAndSet (HandlerStatus.Unassigned, HandlerStatus.Registering));
				Preconditions.checkState (this.handler.compareAndSet (null, handler));
				Preconditions.checkState (this.scheduler.compareAndSet (null, scheduler));
				Preconditions.checkState (this.delegate.get () == null);
				scheduler.registerActor (this);
				final ActorAssignAction action = new ActorAssignAction (this, handler, scheduler);
				this.enqueueAction (action);
				return (action.future.completion);
			}
		}
		
		final CallbackCompletion<Void> triggerAssign (final Callbacks delegate)
		{
			Preconditions.checkNotNull (delegate);
			Preconditions.checkArgument (this.specification.isInstance (delegate));
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("assigning (triggered) delegate `%{object}` for proxy `%{object:identity}` (owned by actor `%{object:identity}`)...", delegate, this.proxy, this);
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.handlerStatus.compareAndSet (HandlerStatus.Unassigned, HandlerStatus.Delegated));
				Preconditions.checkState (this.delegate.compareAndSet (null, (_Callbacks_) delegate));
				Preconditions.checkState (this.handler.get () == null);
				Preconditions.checkState (this.scheduler.get () == null);
				for (final Action<?, ?> action : this.actions)
					if (action instanceof ActorCallbackAction)
						this.delegateInvokeCallback ((ActorCallbackAction<?>) action);
					else
						throw (new IllegalStateException ());
				return (CallbackCompletion.createOutcome ());
			}
		}
		
		final CallbackCompletion<Void> triggerDestroy ()
		{
			this.reactor.transcript.traceDebugging ("destroying (triggered) proxy `%{object:identity}` (owned by actor `%{object:identity}`)...", this.proxy, this);
			synchronized (this.monitor) {
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							return (this.destroyFuture.completion);
						default:
							throw (new IllegalStateException ());
					}
				}
				final ActorDestroyAction action = new ActorDestroyAction (this, this.destroyFuture);
				switch (this.handlerStatus.get ()) {
					case Unassigned :
					case Delegated :
						Preconditions.checkState (this.destroyAction.compareAndSet (null, action));
						this.executeDestroy ();
						break;
					case Registering :
					case Assigned :
						this.enqueueAction (action);
						break;
					default:
						throw (new IllegalStateException ());
				}
				return (action.future.completion);
			}
		}
		
		final void triggerFailure (final Throwable exception)
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				this.failed.compareAndSet (null, exception);
				this.triggerDestroy ();
			}
		}
		
		final ConcurrentLinkedQueue<ActorAction<?>> actions;
		final AtomicReference<ActorAssignAction> assignAction;
		final AtomicReference<_Callbacks_> delegate;
		final AtomicReference<ActorDestroyAction> destroyAction;
		final Future<Void> destroyFuture;
		final AtomicReference<Throwable> failed;
		final AtomicReference<CallbackHandler> handler;
		final AtomicReference<HandlerStatus> handlerStatus;
		final Monitor monitor;
		final CallbackProxy proxy;
		final Reactor reactor;
		final AtomicReference<Scheduler> scheduler;
		final AtomicReference<ScheduleStatus> scheduleStatus;
		final Class<_Callbacks_> specification;
		final AtomicReference<Status> status;
		
		static enum HandlerStatus
		{
			Assigned (),
			Delegated (),
			Registering (),
			Unassigned ();
		}
		
		static enum ScheduleStatus
		{
			Idle (),
			Running (),
			RunningReschedule (),
			Scheduled ();
		}
		
		static enum Status
		{
			Active (),
			Destroyed (),
			Destroying ();
		}
	}
	
	static abstract class ActorAction<_Outcome_ extends Object>
			extends Action<Actor<?>, _Outcome_>
	{
		ActorAction (final Actor<?> actor, final Future<_Outcome_> future)
		{
			super (actor.reactor, actor, future);
		}
	}
	
	static final class ActorAssignAction
			extends ActorAction<Void>
	{
		ActorAssignAction (final Actor<?> actor, final CallbackHandler handler, final Scheduler scheduler)
		{
			super (actor, new Future<Void> (actor.reactor));
			this.handler = handler;
			this.scheduler = scheduler;
		}
		
		final CallbackHandler handler;
		final Scheduler scheduler;
	}
	
	static final class ActorCallbackAction<_Outcome_ extends Object>
			extends ActorAction<_Outcome_>
	{
		ActorCallbackAction (final Actor<?> actor, final Method method, final Object[] arguments)
		{
			super (actor, new Future<_Outcome_> (actor.reactor));
			this.method = method;
			this.arguments = arguments;
		}
		
		final Object[] arguments;
		final Method method;
	}
	
	static final class ActorDestroyAction
			extends ActorAction<Void>
	{
		ActorDestroyAction (final Actor<?> actor, final Future<Void> future)
		{
			super (actor, future);
		}
	}
	
	static final class Future<_Outcome_ extends Object>
			extends AbstractFuture<_Outcome_>
			implements
				CallbackCompletionBackend
	{
		Future (final Reactor reactor)
		{
			super ();
			this.reactor = reactor.reference;
			this.completion = CallbackCompletion.createDeferred ((CallbackCompletionBackend) this);
		}
		
		@Override
		public final boolean awaitCompletion (final CallbackCompletion<?> completion, final long timeout)
		{
			Preconditions.checkArgument (completion == this.completion);
			return (this.await (timeout));
		}
		
		@Override
		public final boolean cancel (final boolean interrupt)
		{
			if (interrupt)
				throw (new UnsupportedOperationException ());
			return (false);
		}
		
		@Override
		public final Throwable getCompletionException (final CallbackCompletion<?> completion)
		{
			Preconditions.checkArgument (completion == this.completion);
			final Object outcome;
			try {
				outcome = Threading.await ((ListenableFuture<Object>) this, 0, Future.timeoutMarker);
			} catch (final ExecutionException exception) {
				return (exception.getCause ());
			}
			if (outcome == Future.timeoutMarker)
				throw (new IllegalStateException ());
			return (null);
		}
		
		@Override
		public final Object getCompletionOutcome (final CallbackCompletion<?> completion)
		{
			Preconditions.checkArgument (completion == this.completion);
			return (this.getOutcome ());
		}
		
		@Override
		public final BasicCallbackReactor getReactor ()
		{
			return (this.reactor.get ());
		}
		
		@Override
		public final void observeCompletion (final CallbackCompletion<?> completion, final CallbackCompletionObserver observer)
		{
			Preconditions.checkArgument (completion == this.completion);
			Preconditions.checkNotNull (observer);
			this.addListener (new Listener (observer), Future.inlineExecutor);
		}
		
		@Override
		protected final void interruptTask ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		final boolean await (final long timeout)
		{
			final Object outcome = Threading.awaitOrCatch ((ListenableFuture<Object>) this, timeout, Future.timeoutMarker, Future.exceptionMarker);
			if (outcome == Future.timeoutMarker)
				return (false);
			if (outcome == Future.exceptionMarker)
				return (true);
			return (true);
		}
		
		final _Outcome_ getOutcome ()
		{
			final Object outcome = Threading.awaitOrCatch ((ListenableFuture<Object>) this, 0, Future.timeoutMarker, Future.exceptionMarker);
			if (outcome == Future.timeoutMarker)
				throw (new IllegalStateException ());
			if (outcome == Future.exceptionMarker)
				throw (new IllegalStateException ());
			return ((_Outcome_) outcome);
		}
		
		final void triggerChain (final CallbackCompletion<_Outcome_> completion)
		{
			Preconditions.checkNotNull (completion);
			completion.observe (new ChainedObserver (completion));
		}
		
		final void triggerFailure (final Throwable exception)
		{
			Preconditions.checkNotNull (exception);
			this.setException (exception);
		}
		
		final void triggerSuccess (final _Outcome_ outcome)
		{
			this.set (outcome);
		}
		
		final CallbackCompletion<_Outcome_> completion;
		final Reference reactor;
		private static final Object exceptionMarker = new Object ();
		private static final Executor inlineExecutor = MoreExecutors.sameThreadExecutor ();
		private static final Object timeoutMarker = new Object ();
		
		final class ChainedObserver
				extends Object
				implements
					CallbackCompletionObserver,
					CallbackProxy
		{
			ChainedObserver (final CallbackCompletion<_Outcome_> completion)
			{
				super ();
				this.completion = completion;
			}
			
			@Override
			public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion)
			{
				try {
					Preconditions.checkState (completion == this.completion);
					final Throwable exception = this.completion.getException ();
					if (exception == null)
						Future.this.triggerSuccess (this.completion.getOutcome ());
					else
						Future.this.triggerFailure (exception);
				} catch (final Throwable exception) {
					Future.this.triggerFailure (exception);
				}
				return (null);
			}
			
			final CallbackCompletion<_Outcome_> completion;
		}
		
		final class Listener
				extends Object
				implements
					Runnable
		{
			Listener (final CallbackCompletionObserver observer)
			{
				super ();
				this.observer = observer;
			}
			
			@Override
			public final void run ()
			{
				this.observer.completed (Future.this.completion);
			}
			
			final CallbackCompletionObserver observer;
		}
	}
	
	static final class IdentityComparator
			extends Object
			implements
				Comparator<Object>
	{
		IdentityComparator ()
		{
			super ();
		}
		
		@Override
		public final int compare (final Object left, final Object right)
		{
			return ((left.hashCode () - right.hashCode ()));
		}
	}
	
	final static class Reactor
			extends Object
			implements
				ActionTarget
	{
		Reactor (final BasicCallbackReactor facade, final ThreadingContext threading, final ExceptionTracer exceptions)
		{
			super ();
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (threading);
			Preconditions.checkNotNull (exceptions);
			this.monitor = Monitor.create (this);
			synchronized (this.monitor) {
				this.facade = facade;
				this.reference = this.facade.new Reference ();
				this.threading = threading;
				this.transcript = Transcript.create (this.facade);
				this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
				this.executor = this.threading.createCachedThreadPool (ThreadConfiguration.create (this.facade, "isolates", true));
				this.schedulers = new ConcurrentHashMap<CallbackIsolate, BasicCallbackReactor.Scheduler> ();
				this.actors = new ConcurrentHashMap<CallbackProxy, BasicCallbackReactor.Actor<?>> ();
				this.status = Atomics.newReference (Status.Active);
				this.transcript.traceDebugging ("created reactor `%{object:identity}`.", this.facade);
			}
		}
		
		final boolean await (final long timeout)
		{
			return (Threading.join (this.executor, timeout));
		}
		
		final CallbackIsolate createIsolate ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Scheduler scheduler = new Scheduler (this);
				return (scheduler.isolate);
			}
		}
		
		final <_Callbacks_ extends Callbacks> _Callbacks_ createProxy (final Class<_Callbacks_> specification)
		{
			Preconditions.checkNotNull (specification);
			Preconditions.checkArgument (Callbacks.class.isAssignableFrom (specification));
			Preconditions.checkArgument (!CallbackProxy.class.isAssignableFrom (specification));
			Preconditions.checkArgument (!CallbackHandler.class.isAssignableFrom (specification));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Actor<_Callbacks_> actor = new Actor<_Callbacks_> (this, specification);
				return (specification.cast (actor.proxy));
			}
		}
		
		final void enqueueRunnable (final Runnable runnable)
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				this.transcript.traceDebugging ("enqueueing runnable `%{object}` on reactor `%{object:identity}`...", runnable, this.facade);
				this.executor.execute (runnable);
			}
		}
		
		final void executeDestroy ()
		{
			this.transcript.traceDebugging ("destroying reactor `%{object:identity}`...", this.facade);
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Destroying);
			}
			if (!this.actors.isEmpty ()) {
				for (final Actor<?> actor : this.actors.values ())
					actor.triggerDestroy ();
				this.transcript.traceDebugging ("defer destroying reactor `%{object:identity}` due to registered actors...", this.facade);
				this.enqueueRunnable (new ReactorDestroyAction (this));
				return;
			}
			if (!this.schedulers.isEmpty ()) {
				for (final Scheduler scheduler : this.schedulers.values ())
					scheduler.triggerDestroy ();
				this.transcript.traceDebugging ("defer destroying reactor `%{object:identity}` due to registered schedulers...", this.facade);
				this.enqueueRunnable (new ReactorDestroyAction (this));
				return;
			}
			synchronized (this.monitor) {
				Preconditions.checkState (this.schedulers.isEmpty ());
				Preconditions.checkState (this.actors.isEmpty ());
				this.executor.shutdown ();
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
			}
			this.transcript.traceDebugging ("destroyed reactor `%{object:identity}`.", this.facade);
		}
		
		final void registerActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("registering actor `%{object:identity}` on reactor `%{object:identity}`...", actor, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.actors.put (actor.proxy, actor) == null);
			}
		}
		
		final void registerScheduler (final Scheduler scheduler)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("registering scheduler `%{object:identity}` on reactor `%{object:identity}`...", scheduler, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.schedulers.put (scheduler.isolate, scheduler) == null);
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackCompletion<Void> triggerAssign (final _Callbacks_ proxy, final _Callbacks_ delegate)
		{
			Preconditions.checkNotNull (proxy);
			Preconditions.checkNotNull (delegate);
			Preconditions.checkArgument (Callbacks.class.isInstance (delegate));
			Preconditions.checkArgument (CallbackProxy.class.isInstance (delegate));
			Preconditions.checkArgument (!CallbackHandler.class.isInstance (delegate));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Actor<?> actor = this.actors.get (proxy);
				Preconditions.checkNotNull (actor);
				return (actor.triggerAssign (delegate));
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackCompletion<Void> triggerAssign (final _Callbacks_ proxy, final CallbackHandler handler, final CallbackIsolate isolate)
		{
			Preconditions.checkNotNull (proxy);
			Preconditions.checkNotNull (isolate);
			Preconditions.checkNotNull (handler);
			// FIXME
			// Preconditions.checkArgument (Callbacks.class.isInstance (handler));
			Preconditions.checkArgument (!CallbackProxy.class.isInstance (handler));
			Preconditions.checkArgument (CallbackHandler.class.isInstance (handler));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Actor<?> actor = this.actors.get (proxy);
				Preconditions.checkNotNull (actor);
				final Scheduler scheduler = this.schedulers.get (isolate);
				Preconditions.checkNotNull (scheduler);
				return (actor.triggerAssign (handler, scheduler));
			}
		}
		
		final void triggerDestroy ()
		{
			synchronized (this.monitor) {
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							return;
						default:
							throw (new IllegalStateException ());
					}
				}
				this.enqueueRunnable (new ReactorDestroyAction (this));
			}
		}
		
		final CallbackCompletion<Void> triggerDestroyIsolate (final CallbackIsolate isolate)
		{
			Preconditions.checkNotNull (isolate);
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				final Scheduler scheduler = this.schedulers.get (isolate);
				Preconditions.checkNotNull (scheduler);
				return (scheduler.triggerDestroy ());
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackCompletion<Void> triggerDestroyProxy (final _Callbacks_ proxy)
		{
			Preconditions.checkNotNull (proxy);
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				final Actor<?> actor = this.actors.get (proxy);
				Preconditions.checkNotNull (actor);
				return (actor.triggerDestroy ());
			}
		}
		
		final void unregisterActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("unregistering actor `%{object:identity}` from reactor `%{object:identity}`...", actor, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.actors.remove (actor.proxy) == actor);
			}
		}
		
		final void unregisterScheduler (final Scheduler scheduler)
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("unregistering scheduler `%{object:identity}` from reactor `%{object:identity}`...", scheduler, this.facade);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.schedulers.remove (scheduler.isolate) == scheduler);
			}
		}
		
		final ConcurrentHashMap<CallbackProxy, Actor<?>> actors;
		final TranscriptExceptionTracer exceptions;
		final ExecutorService executor;
		final BasicCallbackReactor facade;
		final Monitor monitor;
		final Reference reference;
		final ConcurrentHashMap<CallbackIsolate, Scheduler> schedulers;
		final AtomicReference<Status> status;
		final ThreadingContext threading;
		final Transcript transcript;
		
		static enum Status
		{
			Active (),
			Destroyed (),
			Destroying ();
		}
	}
	
	static abstract class ReactorAction
			extends Action<Reactor, Void>
	{
		ReactorAction (final Reactor reactor, final Future<Void> future)
		{
			super (reactor, reactor, future);
		}
	}
	
	static final class ReactorDestroyAction
			extends ReactorAction
			implements
				Runnable
	{
		ReactorDestroyAction (final Reactor reactor)
		{
			super (reactor, null);
		}
		
		@Override
		public final void run ()
		{
			try {
				this.reactor.executeDestroy ();
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceIgnoredException (exception);
			}
		}
	}
	
	final class Reference
			extends WeakReference<BasicCallbackReactor>
	{
		Reference ()
		{
			super (BasicCallbackReactor.this);
		}
		
		@Override
		public final void clear ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final boolean enqueue ()
		{
			return (super.enqueue ());
		}
		
		@Override
		public BasicCallbackReactor get ()
		{
			return (super.get ());
		}
		
		@Override
		public final boolean isEnqueued ()
		{
			return (super.isEnqueued ());
		}
	}
	
	static final class Scheduler
			extends Object
			implements
				ActionTarget,
				CallbackIsolateBackend
	{
		Scheduler (final Reactor reactor)
		{
			super ();
			this.monitor = Monitor.create (this);
			synchronized (this.monitor) {
				this.reactor = reactor;
				this.destroyFuture = new Future<Void> (this.reactor);
				this.isolate = CallbackIsolate.create (this);
				this.actorsRegistered = new ConcurrentSkipListSet<BasicCallbackReactor.Actor<?>> (new IdentityComparator ());
				this.actorsEnqueued = new ConcurrentLinkedQueue<BasicCallbackReactor.Actor<?>> ();
				this.actionsEnqueued = new ConcurrentLinkedQueue<SchedulerRunnableAction> ();
				this.destroyAction = Atomics.newReference (null);
				this.scheduleStatus = Atomics.newReference (ScheduleStatus.Idle);
				this.status = Atomics.newReference (Status.Active);
				this.reactor.registerScheduler (this);
				this.reactor.transcript.traceDebugging ("created isolate `%{object:identity}` (owned by scheduler `%{object:identity}` from reactor `%{object:identity}`).", this.isolate, this, this.reactor.facade);
			}
		}
		
		@Override
		public final boolean awaitIsolate (final CallbackIsolate isolate, final long timeout)
		{
			Preconditions.checkState (isolate == this.isolate);
			return (this.destroyFuture.await (timeout));
		}
		
		@Override
		public final CallbackCompletion<Void> destroyIsolate (final CallbackIsolate isolate)
		{
			Preconditions.checkState (isolate == this.isolate);
			return (this.triggerDestroy ());
		}
		
		@Override
		public final CallbackCompletion<Void> enqueueOnIsolate (final CallbackIsolate isolate, final Runnable runnable)
		{
			Preconditions.checkState (isolate == this.isolate);
			Preconditions.checkNotNull (runnable);
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("enqueueing runnable `%{object:identity}` on scheduler `%{object:identity}`...", runnable, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				final SchedulerRunnableAction action = new SchedulerRunnableAction (this, runnable);
				this.actionsEnqueued.add (action);
				this.schedule ();
				return (action.future.completion);
			}
		}
		
		@Override
		public final BasicCallbackReactor getReactor ()
		{
			return (this.reactor.facade);
		}
		
		final void enqueueActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("enqueueing actor `%{object:identity}` on scheduler `%{object:identity}`...", actor, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				this.actorsEnqueued.add (actor);
				this.schedule ();
			}
		}
		
		final void executeActions ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Scheduled, ScheduleStatus.Running));
			}
			{
				this.reactor.transcript.traceDebugging ("executing enqueued actors on scheduler `%{object:identity}`...", this);
				while (true) {
					final Actor<?> actor = this.actorsEnqueued.poll ();
					if (actor == null)
						break;
					actor.executeActions ();
				}
				this.reactor.transcript.traceDebugging ("executed enqueued actors on scheduler `%{object:identity}.`", this);
			}
			{
				this.reactor.transcript.traceDebugging ("executing enqueued runnables on scheduler `%{object:identity}`...", this);
				while (true) {
					final SchedulerRunnableAction action = this.actionsEnqueued.poll ();
					if (action == null)
						break;
					// FIXME
					this.reactor.transcript.traceDebugging ("invocking runnable `%{object:identity}` on scheduler `%{object:identity}`...", action.runnable, this);
					try {
						action.runnable.run ();
						action.future.triggerSuccess (null);
					} catch (final Throwable exception) {
						this.reactor.exceptions.traceDeferredException (exception);
						action.future.triggerFailure (exception);
					}
				}
				this.reactor.transcript.traceDebugging ("executed enqueued runnables on scheduler `%{object:identity}.`", this);
			}
			{
				if (this.destroyAction.get () != null)
					this.executeDestroy ();
			}
			synchronized (this.monitor) {
				if (this.scheduleStatus.get () == ScheduleStatus.RunningReschedule) {
					Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.RunningReschedule, ScheduleStatus.Scheduled));
					this.reactor.enqueueRunnable (new SchedulerExecuteAction (this));
					this.reactor.transcript.traceDebugging ("scheduled scheduler `%{object:identity}`.", this);
				} else
					Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Running, ScheduleStatus.Idle));
			}
		}
		
		final void executeDestroy ()
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("destroying scheduler `%{object:identity}`...", this);
				Preconditions.checkState (this.status.get () == Status.Destroying);
				final SchedulerDestroyAction action = this.destroyAction.get ();
				Preconditions.checkNotNull (action);
				if (!this.actorsRegistered.isEmpty ()) {
					for (final Actor<?> actor : this.actorsRegistered)
						actor.triggerDestroy ();
					this.reactor.transcript.traceDebugging ("defer destroying scheduler `%{object:identity}` due to registered actors...", this);
					this.schedule ();
					return;
				}
				Preconditions.checkState (this.actorsRegistered.isEmpty ());
				Preconditions.checkState (this.actorsEnqueued.isEmpty ());
				Preconditions.checkState (this.destroyAction.compareAndSet (action, null));
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
				this.reactor.unregisterScheduler (this);
				action.future.triggerSuccess (null);
				this.reactor.transcript.traceDebugging ("destroyed isolate `%{object:identity}` (owned by scheduler `%{object:identity}).`", this.isolate, this);
			}
		}
		
		final void registerActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("registering actor `%{object:identity}` on scheduler `%{object:identity}`...", actor, this);
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.actorsRegistered.add (actor));
			}
		}
		
		final void schedule ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				switch (this.scheduleStatus.get ()) {
					case Idle :
						Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Idle, ScheduleStatus.Scheduled));
						this.reactor.enqueueRunnable (new SchedulerExecuteAction (this));
						this.reactor.transcript.traceDebugging ("scheduled scheduler `%{object:identity}`.", this);
						break;
					case Scheduled :
						break;
					case Running :
						Preconditions.checkState (this.scheduleStatus.compareAndSet (ScheduleStatus.Running, ScheduleStatus.RunningReschedule));
						break;
					case RunningReschedule :
						break;
					default:
						throw (new IllegalStateException ());
				}
			}
		}
		
		final CallbackCompletion<Void> triggerDestroy ()
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("destroying (triggered) for scheduler `%{object:identity}`...", this);
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							return (this.destroyFuture.completion);
						default:
							throw (new IllegalStateException ());
					}
				}
				final SchedulerDestroyAction action = new SchedulerDestroyAction (this, this.destroyFuture);
				Preconditions.checkState (this.destroyAction.compareAndSet (null, action));
				this.schedule ();
				return (action.future.completion);
			}
		}
		
		final void unregisterActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				this.reactor.transcript.traceDebugging ("unregistering actor `%{object:identity}` from scheduler `%{object:identity}`...", actor, this);
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				Preconditions.checkState (this.actorsRegistered.remove (actor));
			}
		}
		
		final ConcurrentLinkedQueue<SchedulerRunnableAction> actionsEnqueued;
		final ConcurrentLinkedQueue<Actor<?>> actorsEnqueued;
		final ConcurrentSkipListSet<Actor<?>> actorsRegistered;
		final AtomicReference<SchedulerDestroyAction> destroyAction;
		final Future<Void> destroyFuture;
		final CallbackIsolate isolate;
		final Monitor monitor;
		final Reactor reactor;
		final AtomicReference<ScheduleStatus> scheduleStatus;
		final AtomicReference<Status> status;
		
		static enum ScheduleStatus
		{
			Idle (),
			Running (),
			RunningReschedule (),
			Scheduled ();
		}
		
		static enum Status
		{
			Active (),
			Destroyed (),
			Destroying ();
		}
	}
	
	static abstract class SchedulerAction
			extends Action<Scheduler, Void>
	{
		SchedulerAction (final Scheduler scheduler, final Future<Void> future)
		{
			super (scheduler.reactor, scheduler, future);
		}
	}
	
	static final class SchedulerDestroyAction
			extends SchedulerAction
	{
		SchedulerDestroyAction (final Scheduler scheduler, final Future<Void> future)
		{
			super (scheduler, future);
		}
	}
	
	static final class SchedulerExecuteAction
			extends SchedulerAction
			implements
				Runnable
	{
		SchedulerExecuteAction (final Scheduler scheduler)
		{
			super (scheduler, null);
		}
		
		@Override
		public final void run ()
		{
			try {
				this.target.executeActions ();
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceIgnoredException (exception);
			}
		}
	}
	
	static final class SchedulerRunnableAction
			extends SchedulerAction
	{
		SchedulerRunnableAction (final Scheduler scheduler, final Runnable runnable)
		{
			super (scheduler, new Future<Void> (scheduler.reactor));
			this.runnable = runnable;
		}
		
		final Runnable runnable;
	}
}
