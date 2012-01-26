
package eu.mosaic_cloud.tools.callbacks.implementations.basic.v2;


import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.v2.CallbackReference;
import eu.mosaic_cloud.tools.callbacks.core.v2.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;


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
	public final <_Callbacks_ extends Callbacks> CallbackReference assignHandler (final _Callbacks_ proxy, final CallbackIsolate isolate, final CallbackHandler<_Callbacks_> handler)
	{
		return (this.reactor.triggerAssign (proxy, isolate, handler));
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
	
	public final boolean destoy ()
	{
		return (this.destroy (0));
	}
	
	public final boolean destroy (final long timeout)
	{
		this.reactor.triggerDestroy ();
		return (this.reactor.await (timeout));
	}
	
	@Override
	public final CallbackReference destroyIsolate (final CallbackIsolate isolate)
	{
		return (this.reactor.triggerDestroyIsolate (isolate));
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackReference destroyProxy (final _Callbacks_ proxy)
	{
		return (this.reactor.triggerDestroyProxy (proxy));
	}
	
	final Reactor reactor;
	
	public static final BasicCallbackReactor create (final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new BasicCallbackReactor (threading, exceptions));
	}
	
	static abstract class Action<_Target_ extends ActionTarget>
			extends Object
	{
		Action (final Reactor reactor, final _Target_ target, final Completion completion)
		{
			super ();
			this.reactor = reactor;
			this.target = target;
			this.completion = completion;
			if (this.completion != null)
				this.reference = CallbackReference.create (this.reactor.reference, this.completion);
			else
				this.reference = null;
		}
		
		final Completion completion;
		final Reactor reactor;
		final CallbackReference reference;
		final _Target_ target;
	}
	
	static interface ActionTarget
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
				this.destroyCompletion = new Completion (this.reactor);
				this.proxy = (CallbackProxy) Proxy.newProxyInstance (specification.getClassLoader (), new Class[] {specification, CallbackProxy.class}, this);
				this.actions = new ConcurrentLinkedQueue<ActorAction> ();
				this.handler = new AtomicReference<CallbackHandler<?>> (null);
				this.scheduler = new AtomicReference<Scheduler> (null);
				this.failed = new AtomicReference<Throwable> (null);
				this.assignAction = new AtomicReference<ActorAssignAction> (null);
				this.destroyAction = new AtomicReference<ActorDestroyAction> (null);
				this.handlerStatus = new AtomicReference<HandlerStatus> (HandlerStatus.Unassigned);
				this.scheduleStatus = new AtomicReference<SchedulerStatus> (SchedulerStatus.Idle);
				this.status = new AtomicReference<Status> (Status.Active);
			}
		}
		
		@Override
		public final Object invoke (final Object callbacks, final Method method, final Object[] arguments)
				throws Throwable
		{
			if (method.getDeclaringClass () == Object.class)
				return (method.invoke (this, arguments));
			if (method.getReturnType () != CallbackReference.class)
				throw (new IllegalAccessError ());
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final ActorInvokeAction action = new ActorInvokeAction (this, method, arguments);
				this.enqueueAction (action);
				return (action.reference);
			}
		}
		
		final void enqueueAction (final ActorAction action)
		{
			synchronized (this.monitor) {
				if (action instanceof ActorInvokeAction) {
					this.schedule (false);
					Preconditions.checkState (this.actions.offer (action));
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
			throw (new UnsupportedOperationException ());
		}
		
		final void executeDestroy ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
				switch (this.handlerStatus.getAndSet (null)) {
					case Unassigned :
					case Unregistered :
						break;
					case Assigned :
						break;
					default:
						throw (new IllegalStateException ());
				}
				final ActorDestroyAction action = this.destroyAction.get ();
				Preconditions.checkState (action != null);
				this.handler.set (null);
				this.scheduler.set (null);
				this.actions.clear ();
			}
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
					if (this.scheduleStatus.get () == SchedulerStatus.Scheduled)
						return;
					if (this.handler.get () == null)
						return;
				}
				final Scheduler scheduler = this.scheduler.get ();
				Preconditions.checkNotNull (scheduler);
				scheduler.enqueueActor (this);
			}
		}
		
		final CallbackReference triggerAssign (final Scheduler scheduler, final CallbackHandler<?> handler)
		{
			Preconditions.checkNotNull (scheduler);
			Preconditions.checkNotNull (handler);
			Preconditions.checkArgument (this.specification.isInstance (handler));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.handlerStatus.compareAndSet (HandlerStatus.Unassigned, HandlerStatus.Registering));
				synchronized (scheduler.monitor) {
					Preconditions.checkState (scheduler.status.get () == Scheduler.Status.Active);
					Preconditions.checkState (this.handler.compareAndSet (null, handler));
					Preconditions.checkState (this.scheduler.compareAndSet (null, scheduler));
					final ActorAssignAction action = new ActorAssignAction (this, handler);
					this.enqueueAction (action);
					scheduler.enqueueActor (this);
					return (action.reference);
				}
			}
		}
		
		final CallbackReference triggerDestroy ()
		{
			synchronized (this.monitor) {
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							final ActorDestroyAction action = this.destroyAction.get ();
							Preconditions.checkState (action != null);
							return (action.reference);
						default:
							throw (new IllegalStateException ());
					}
				}
				final ActorDestroyAction action = new ActorDestroyAction (this, this.destroyCompletion);
				Preconditions.checkState (this.destroyAction.compareAndSet (null, action));
				switch (this.handlerStatus.get ()) {
					case Unassigned :
						this.executeDestroy ();
						break;
					case Registering :
					case Assigned :
						this.schedule (true);
						break;
					default:
						throw (new IllegalStateException ());
				}
				return (action.reference);
			}
		}
		
		final ConcurrentLinkedQueue<ActorAction> actions;
		final AtomicReference<ActorAssignAction> assignAction;
		final AtomicReference<ActorDestroyAction> destroyAction;
		final Completion destroyCompletion;
		final AtomicReference<Throwable> failed;
		final AtomicReference<CallbackHandler<?>> handler;
		final AtomicReference<HandlerStatus> handlerStatus;
		final Monitor monitor;
		final CallbackProxy proxy;
		final Reactor reactor;
		final AtomicReference<Scheduler> scheduler;
		final AtomicReference<SchedulerStatus> scheduleStatus;
		final Class<_Callbacks_> specification;
		final AtomicReference<Status> status;
		
		static enum HandlerStatus
		{
			Assigned (),
			Registering (),
			Unassigned (),
			Unregistered ();
		}
		
		static enum SchedulerStatus
		{
			Idle (),
			Scheduled ();
		}
		
		static enum Status
		{
			Active (),
			Destroyed (),
			Destroying (),
			Failed ();
		}
	}
	
	static abstract class ActorAction
			extends Action<Actor<?>>
	{
		ActorAction (final Actor<?> actor, final Completion completion)
		{
			super (actor.reactor, actor, completion);
		}
	}
	
	static final class ActorAssignAction
			extends ActorAction
	{
		ActorAssignAction (final Actor<?> actor, final CallbackHandler<?> handler)
		{
			super (actor, new Completion (actor.reactor));
			this.handler = handler;
		}
		
		final CallbackHandler<?> handler;
	}
	
	static final class ActorDestroyAction
			extends ActorAction
	{
		ActorDestroyAction (final Actor<?> actor, final Completion completion)
		{
			super (actor, completion);
		}
	}
	
	static final class ActorInvokeAction
			extends ActorAction
	{
		ActorInvokeAction (final Actor<?> actor, final Method method, final Object[] arguments)
		{
			super (actor, new Completion (actor.reactor));
			this.method = method;
			this.arguments = arguments;
		}
		
		final Object[] arguments;
		final Method method;
	}
	
	static final class Completion
			extends AbstractFuture<Boolean>
			implements
				CallbackCompletion
	{
		Completion (final Reactor reactor)
		{
			super ();
			this.reactor = reactor;
		}
		
		@Override
		public final boolean cancel (final boolean interrupt)
		{
			if (interrupt)
				throw (new UnsupportedOperationException ());
			return (false);
		}
		
		@Override
		protected final void interruptTask ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		final void triggerFailure (final Throwable exception)
		{
			this.setException (exception);
		}
		
		final void triggerSuccess ()
		{
			this.set (Boolean.TRUE);
		}
		
		final Reactor reactor;
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
				this.schedulersActive = new ConcurrentHashMap<CallbackIsolate, BasicCallbackReactor.Scheduler> ();
				this.actorsActive = new ConcurrentHashMap<CallbackProxy, BasicCallbackReactor.Actor<?>> ();
				this.status = new AtomicReference<BasicCallbackReactor.Reactor.Status> (Status.Active);
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
				this.schedulersActive.put (scheduler.isolate, scheduler);
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
				final Actor<_Callbacks_> handler = new Actor<_Callbacks_> (this, specification);
				this.actorsActive.put (handler.proxy, handler);
				return (specification.cast (handler.proxy));
			}
		}
		
		final void executeDestroy ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Destroying);
				if (!this.schedulersActive.isEmpty ()) {
					this.executor.execute (new ReactorDestroyAction (this));
					return;
				}
				this.executor.shutdown ();
				this.schedulersActive.clear ();
				this.actorsActive.clear ();
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackReference triggerAssign (final _Callbacks_ proxy, final CallbackIsolate isolate, final CallbackHandler<_Callbacks_> handler)
		{
			Preconditions.checkNotNull (proxy);
			Preconditions.checkNotNull (isolate);
			Preconditions.checkNotNull (handler);
			Preconditions.checkArgument (Callbacks.class.isInstance (handler));
			Preconditions.checkArgument (!CallbackProxy.class.isInstance (handler));
			Preconditions.checkArgument (CallbackHandler.class.isInstance (handler));
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				final Scheduler scheduler = this.schedulersActive.get (isolate);
				Preconditions.checkNotNull (scheduler);
				final Actor<?> actor = this.actorsActive.get (proxy);
				Preconditions.checkNotNull (actor);
				return (actor.triggerAssign (scheduler, handler));
			}
		}
		
		final void triggerDestroy ()
		{
			synchronized (this.monitor) {
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
						case Destroyed :
							break;
						default:
							throw (new IllegalStateException ());
					}
				}
				this.executor.execute (new ReactorDestroyAction (this));
			}
		}
		
		final CallbackReference triggerDestroyIsolate (final CallbackIsolate isolate)
		{
			Preconditions.checkNotNull (isolate);
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				final Scheduler scheduler = this.schedulersActive.get (isolate);
				Preconditions.checkNotNull (scheduler);
				return (scheduler.triggerDestroy ());
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackReference triggerDestroyProxy (final _Callbacks_ proxy)
		{
			Preconditions.checkNotNull (proxy);
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				final Actor<?> actor = this.actorsActive.get (proxy);
				Preconditions.checkNotNull (actor);
				return (actor.triggerDestroy ());
			}
		}
		
		final ConcurrentHashMap<CallbackProxy, Actor<?>> actorsActive;
		final TranscriptExceptionTracer exceptions;
		final ExecutorService executor;
		final BasicCallbackReactor facade;
		final Monitor monitor;
		final Reference reference;
		final ConcurrentHashMap<CallbackIsolate, Scheduler> schedulersActive;
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
			extends Action<Reactor>
	{
		ReactorAction (final Reactor reactor, final Completion completion)
		{
			super (reactor, reactor, completion);
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
			this.reactor.executeDestroy ();
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
				Runnable
	{
		Scheduler (final Reactor reactor)
		{
			super ();
			this.monitor = Monitor.create (this);
			synchronized (this.monitor) {
				this.reactor = reactor;
				this.destroyCompletion = new Completion (this.reactor);
				this.isolate = CallbackIsolate.create (this.reactor.reference, this.destroyCompletion);
				this.actorsRegistered = new ConcurrentSkipListSet<BasicCallbackReactor.Actor<?>> (new IdentityComparator ());
				this.actorsPending = new ConcurrentLinkedQueue<BasicCallbackReactor.Actor<?>> ();
				this.destroyAction = new AtomicReference<SchedulerDestroyAction> (null);
				this.scheduleStatus = new AtomicReference<SchedulerStatus> (SchedulerStatus.Enabled);
				this.status = new AtomicReference<Status> (Status.Active);
			}
		}
		
		@Override
		public final void run ()
		{
			while (true) {
				final Actor<?> actor = this.actorsPending.poll ();
				if (actor == null)
					break;
				actor.executeActions ();
			}
			if (this.destroyAction.get () != null)
				this.executeDestroy ();
		}
		
		final void enqueueActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				Preconditions.checkState ((this.status.get () == Status.Active) || (this.status.get () == Status.Destroying));
				this.actorsPending.add (actor);
				this.schedule ();
			}
		}
		
		final void executeDestroy ()
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Destroying);
				final SchedulerDestroyAction action = this.destroyAction.get ();
				Preconditions.checkNotNull (action);
				if (!this.actorsRegistered.isEmpty ())
					return;
				this.actorsRegistered.clear ();
				this.actorsPending.clear ();
				action.completion.triggerSuccess ();
				Preconditions.checkState (this.status.compareAndSet (Status.Destroying, Status.Destroyed));
			}
		}
		
		final void registerActor (final Actor<?> actor)
		{
			synchronized (this.monitor) {
				Preconditions.checkState (this.status.get () == Status.Active);
				Preconditions.checkState (this.actorsRegistered.add (actor));
			}
		}
		
		final void schedule ()
		{
			synchronized (this.monitor) {
				throw (new UnsupportedOperationException ());
			}
		}
		
		final CallbackReference triggerDestroy ()
		{
			synchronized (this.monitor) {
				if (!this.status.compareAndSet (Status.Active, Status.Destroying)) {
					switch (this.status.get ()) {
						case Destroying :
							final SchedulerDestroyAction action = this.destroyAction.get ();
							Preconditions.checkNotNull (action);
							return (action.reference);
						default:
							throw (new IllegalStateException ());
					}
				}
				final SchedulerDestroyAction action = new SchedulerDestroyAction (this, this.destroyCompletion);
				Preconditions.checkState (this.destroyAction.compareAndSet (null, action));
				this.schedule ();
				return (action.reference);
			}
		}
		
		final ConcurrentLinkedQueue<Actor<?>> actorsPending;
		final ConcurrentSkipListSet<Actor<?>> actorsRegistered;
		final AtomicReference<SchedulerDestroyAction> destroyAction;
		final Completion destroyCompletion;
		final CallbackIsolate isolate;
		final Monitor monitor;
		final Reactor reactor;
		final AtomicReference<SchedulerStatus> scheduleStatus;
		final AtomicReference<Status> status;
		
		static enum SchedulerStatus
		{
			Disabled (),
			Enabled (),
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
			extends Action<Scheduler>
	{
		SchedulerAction (final Scheduler scheduler, final Completion completion)
		{
			super (scheduler.reactor, scheduler, completion);
		}
	}
	
	static final class SchedulerDestroyAction
			extends SchedulerAction
	{
		SchedulerDestroyAction (final Scheduler scheduler, final Completion completion)
		{
			super (scheduler, completion);
		}
	}
}
