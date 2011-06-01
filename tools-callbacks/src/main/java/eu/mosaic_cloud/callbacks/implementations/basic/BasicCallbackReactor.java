
package eu.mosaic_cloud.callbacks.implementations.basic;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractListenableFuture;
import com.google.common.util.concurrent.AbstractService;
import eu.mosaic_cloud.callbacks.core.CallbackFuture;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.core.CallbackTrigger;
import eu.mosaic_cloud.callbacks.core.Callbacks;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.DefaultThreadPoolFactory;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.transcript.core.Transcript;
import eu.mosaic_cloud.transcript.tools.TranscriptExceptionTracer;


public final class BasicCallbackReactor
		extends Object
		implements
			CallbackReactor
{
	private BasicCallbackReactor (final ExceptionTracer exceptions)
	{
		super ();
		this.delegate = new Reactor (this, exceptions);
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackReference assign (final _Callbacks_ trigger, final _Callbacks_ handler)
	{
		return (this.delegate.assign (trigger, handler));
	}
	
	public final void initialize ()
	{
		this.delegate.startAndWait ();
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> _Callbacks_ register (final Class<_Callbacks_> specification, final _Callbacks_ handler)
	{
		return (this.delegate.register (specification, handler));
	}
	
	@Override
	public final CallbackFuture resolve (final CallbackReference reference)
	{
		return (this.delegate.resolve (reference));
	}
	
	@Override
	public final void terminate ()
	{
		this.delegate.stop ();
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> CallbackReference unregister (final _Callbacks_ trigger)
	{
		return (this.delegate.unregister (trigger));
	}
	
	final Reactor delegate;
	
	public static final BasicCallbackReactor create ()
	{
		return (new BasicCallbackReactor (null));
	}
	
	public static final BasicCallbackReactor create (final ExceptionTracer exceptions)
	{
		return (new BasicCallbackReactor (exceptions));
	}
	
	private static abstract class Action
			extends AbstractListenableFuture<Void>
			implements
				CallbackFuture
	{
		Action (final Proxy proxy, final CallbackReference reference)
		{
			super ();
			this.proxy = proxy;
			this.reference = reference;
		}
		
		@Override
		public final boolean cancel (final boolean maybeInterrupt)
		{
			return (this.cancel ());
		}
		
		final void triggerCancel ()
		{
			this.cancel ();
		}
		
		final void triggerFail (final Throwable exception)
		{
			this.setException (exception);
		}
		
		final void triggerSucceed ()
		{
			this.set (null);
		}
		
		final Proxy proxy;
		final CallbackReference reference;
	}
	
	private static final class AssignAction
			extends Action
	{
		AssignAction (final Proxy proxy, final CallbackReference reference, final CallbackHandler<? extends Callbacks> handler)
		{
			super (proxy, reference);
			this.handler = handler;
		}
		
		final CallbackHandler<? extends Callbacks> handler;
	}
	
	private static final class InvokeAction
			extends Action
	{
		InvokeAction (final Proxy proxy, final CallbackReference reference, final Method method, final Object[] arguments)
		{
			super (proxy, reference);
			this.method = method;
			this.arguments = arguments;
		}
		
		final Object[] arguments;
		final Method method;
	}
	
	private static final class Proxy
			extends Object
			implements
				InvocationHandler,
				Runnable
	{
		Proxy (final Reactor reactor, final Class<? extends Callbacks> specification)
		{
			super ();
			this.reactor = reactor;
			this.specification = specification;
			this.trigger = (CallbackTrigger) java.lang.reflect.Proxy.newProxyInstance (specification.getClassLoader (), new Class[] {specification, CallbackTrigger.class}, this);
			this.handler = new AtomicReference<Object> (null);
			this.actions = new ConcurrentLinkedQueue<Action> ();
			this.scheduled = new AtomicReference<Action> (null);
			this.failed = new AtomicBoolean (false);
		}
		
		@Override
		public final Object invoke (final Object callbacks, final Method method, final Object[] arguments)
				throws Exception
		{
			if (method.getDeclaringClass () == Object.class)
				return (method.invoke (this, arguments));
			return (this.reactor.trigger (this, method, arguments));
		}
		
		@Override
		public final void run ()
		{
			try {
				this.reactor.execute (this);
			} catch (final Throwable exception) {
				this.reactor.exceptions.traceIgnoredException (exception);
				this.reactor.stop ();
			}
		}
		
		final ConcurrentLinkedQueue<Action> actions;
		final AtomicBoolean failed;
		final AtomicReference<Object> handler;
		final Reactor reactor;
		final AtomicReference<Action> scheduled;
		final Class<? extends Callbacks> specification;
		final CallbackTrigger trigger;
	}
	
	static private final class Reactor
			extends AbstractService
	{
		Reactor (final BasicCallbackReactor facade, final ExceptionTracer exceptions)
		{
			Preconditions.checkNotNull (facade);
			this.facade = facade;
			this.monitor = Monitor.create (this.facade);
			synchronized (this.monitor) {
				this.transcript = Transcript.create (this.facade);
				this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
				this.executor = Executors.newCachedThreadPool (DefaultThreadPoolFactory.create (this.facade, true, Thread.NORM_PRIORITY, this.exceptions));
				this.proxies = new WeakHashMap<CallbackTrigger, Proxy> ();
				this.actions = new WeakHashMap<CallbackReference, Action> ();
			}
		}
		
		@Override
		protected final void doStart ()
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("starting...");
				this.notifyStarted ();
			}
		}
		
		@Override
		protected final void doStop ()
		{
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("stopping...");
				for (final Proxy proxy : this.proxies.values ())
					this.unregister ((Callbacks) proxy.trigger);
				this.notifyStopped ();
			}
		}
		
		final <_Callbacks_ extends Callbacks> CallbackReference assign (final _Callbacks_ trigger, final _Callbacks_ handler)
		{
			Preconditions.checkNotNull (trigger);
			Preconditions.checkArgument (trigger instanceof CallbackTrigger);
			final CallbackReference reference;
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("replacing trigger `%{object:identity}`...", trigger);
				final Proxy proxy = this.proxies.get (trigger);
				Preconditions.checkNotNull (proxy);
				if (handler != null) {
					Preconditions.checkArgument (proxy.specification.isInstance (handler));
					Preconditions.checkArgument (handler instanceof CallbackHandler);
					Preconditions.checkArgument (!(handler instanceof CallbackTrigger));
				}
				final Action pending = proxy.actions.peek ();
				if (!(pending instanceof RegisterAction) || !proxy.handler.compareAndSet (null, handler)) {
					reference = CallbackReference.create (this.facade);
					final AssignAction action = new AssignAction (proxy, reference, (CallbackHandler<?>) handler);
					proxy.actions.add (action);
					this.actions.put (reference, action);
					this.transcript.traceDebugging ("enqueued assign action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`;", reference, trigger, proxy.specification, handler);
				} else {
					reference = pending.reference;
					this.transcript.traceDebugging ("continued register action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`;", reference, trigger, proxy.specification, handler);
				}
				this.schedule (proxy);
			}
			return (reference);
		}
		
		final void execute (final AssignAction action)
		{
			final Proxy proxy = action.proxy;
			final Object oldHandler = proxy.handler.get ();
			final Object newHandler = action.handler;
			Preconditions.checkState ((oldHandler != null) || (newHandler != null));
			Preconditions.checkState (proxy.handler.compareAndSet (oldHandler, newHandler));
			this.transcript.traceDebugging ("invocking assign callbacks for trigger `%{object:identity}` for class `%{class}` with old-handler `%{object}` and new-handler `%{object}`...", proxy.trigger, proxy.specification, oldHandler, newHandler);
			try {
				if (oldHandler != null)
					((CallbackHandler<Callbacks>) oldHandler).deassigned ((Callbacks) proxy.trigger, (Callbacks) newHandler);
				if (newHandler != null)
					((CallbackHandler<Callbacks>) newHandler).reassigned ((Callbacks) proxy.trigger, (Callbacks) oldHandler);
				action.triggerSucceed ();
			} catch (final Throwable exception) {
				this.exceptions.traceDeferredException (exception, "unexpected error encountered while invocking assign callbacks for trigger `%{object:identity}` for class `%{class}` with old-handler `%{object}` and new-handle `%{object}`; deferring!", proxy.trigger, proxy.specification, oldHandler, newHandler);
				action.triggerFail (exception);
				proxy.failed.set (true);
			}
			this.schedule (proxy);
		}
		
		final void execute (final InvokeAction action)
		{
			final Proxy proxy = action.proxy;
			final Object handler = proxy.handler.get ();
			if (handler == null) {
				Preconditions.checkState (proxy.scheduled.compareAndSet (action, null));
				this.transcript.traceDebugging ("deferring action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}` due to missing handler;", action.reference, proxy.trigger, proxy.specification);
				return;
			}
			this.transcript.traceDebugging ("invocking callback `%{method}` `%{array}` for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`...", action.method, action.arguments, proxy.trigger, proxy.specification, handler);
			try {
				action.method.invoke (handler, action.arguments);
				action.triggerSucceed ();
			} catch (final Throwable exception) {
				this.exceptions.traceDeferredException (exception, "unexpected error encountered while invocking callback `%{method}` `%{array}` for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`; deferring!", action.method, action.arguments, proxy.trigger, proxy.specification, handler);
				action.triggerFail (exception);
				proxy.failed.set (true);
			}
			this.schedule (proxy);
		}
		
		final void execute (final Proxy proxy)
		{
			final Action action = proxy.actions.peek ();
			Preconditions.checkState (action != null);
			Preconditions.checkState (action == proxy.scheduled.get ());
			this.transcript.traceDebugging ("executing action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}`...", action.reference, proxy.trigger, proxy.specification);
			if (action instanceof InvokeAction)
				this.execute ((InvokeAction) action);
			else if (action instanceof RegisterAction)
				this.execute ((RegisterAction) action);
			else if (action instanceof UnregisterAction)
				this.execute ((UnregisterAction) action);
			else if (action instanceof AssignAction)
				this.execute ((AssignAction) action);
			else
				throw (new IllegalStateException ());
		}
		
		final void execute (final RegisterAction action)
		{
			final Proxy proxy = action.proxy;
			final Object handler = proxy.handler.get ();
			if (handler == null) {
				Preconditions.checkState (proxy.scheduled.compareAndSet (action, null));
				this.transcript.traceDebugging ("deferring action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}` due to missing handler;", action.reference, proxy.trigger, proxy.specification);
				return;
			}
			this.transcript.traceDebugging ("invocking register callback for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`...", proxy.trigger, proxy.specification, handler);
			try {
				((CallbackHandler<Callbacks>) handler).registered ((Callbacks) proxy.trigger);
				action.triggerSucceed ();
			} catch (final Throwable exception) {
				this.exceptions.traceDeferredException (exception, "unexpected error encountered while invocking register callback for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`; deferring!", proxy.trigger, proxy.specification, handler);
				action.triggerFail (exception);
				proxy.failed.set (true);
			}
			this.schedule (proxy);
		}
		
		final void execute (final UnregisterAction action)
		{
			final Proxy proxy = action.proxy;
			final Object handler = proxy.handler.get ();
			if (handler == null) {
				Preconditions.checkState (proxy.scheduled.compareAndSet (action, null));
				this.transcript.traceDebugging ("deferring action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}` due to missing handler;", action.reference, proxy.trigger, proxy.specification);
				return;
			}
			Preconditions.checkState (proxy.handler.compareAndSet (handler, null));
			this.transcript.traceDebugging ("invocking unregister callback for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`...", proxy.trigger, proxy.specification, handler);
			try {
				((CallbackHandler<Callbacks>) handler).unregistered ((Callbacks) proxy.trigger);
				action.triggerSucceed ();
			} catch (final Throwable exception) {
				this.exceptions.traceDeferredException (exception, "unexpected error encountered while invocking unregister callback for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`; deferring!", proxy.trigger, proxy.specification, handler);
				action.triggerFail (exception);
				proxy.failed.set (true);
			}
			this.schedule (proxy);
		}
		
		final <_Callbacks_ extends Callbacks> _Callbacks_ register (final Class<_Callbacks_> specification, final _Callbacks_ handler)
		{
			Preconditions.checkNotNull (specification);
			Preconditions.checkArgument (Callbacks.class.isAssignableFrom (specification));
			if (handler != null) {
				Preconditions.checkArgument (specification.isInstance (handler));
				Preconditions.checkArgument (handler instanceof CallbackHandler);
				Preconditions.checkArgument (!(handler instanceof CallbackTrigger));
			}
			final CallbackTrigger trigger;
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("registering trigger for class `%{class}` with handler `%{object}`...", specification, handler);
				final Proxy proxy;
				try {
					proxy = new Proxy (this, specification);
				} catch (final Throwable exception) {
					this.exceptions.traceDeferredException (exception, "unexpected error encountered while registering trigger for class `%{class}` with handler `%{object}`; re-throwing!", specification, handler);
					throw (new Error (exception));
				}
				trigger = proxy.trigger;
				this.proxies.put (trigger, proxy);
				Preconditions.checkState (proxy.handler.compareAndSet (null, handler));
				final CallbackReference reference = CallbackReference.create (this.facade);
				final RegisterAction action = new RegisterAction (proxy, reference);
				proxy.actions.add (action);
				this.actions.put (reference, action);
				this.transcript.traceDebugging ("enqueued register action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}` with handler `%{object}`;", reference, trigger, specification, handler);
				this.schedule (proxy);
			}
			return (specification.cast (trigger));
		}
		
		final CallbackFuture resolve (final CallbackReference reference)
		{
			Preconditions.checkNotNull (reference);
			synchronized (this.monitor) {
				final Action action = this.actions.get (reference);
				Preconditions.checkNotNull (action);
				return (action);
			}
		}
		
		final void schedule (final Proxy proxy)
		{
			synchronized (this.monitor) {
				while (true) {
					final Action action = proxy.actions.peek ();
					if (action == null)
						break;
					if (action.isDone ()) {
						Preconditions.checkState (proxy.actions.remove () == action);
						proxy.scheduled.compareAndSet (action, null);
						continue;
					}
					if ((action instanceof RegisterAction) && (proxy.handler.get () == null))
						break;
					if (proxy.failed.get ())
						break;
					if (proxy.scheduled.compareAndSet (null, action)) {
						this.transcript.traceDebugging ("scheduling action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}`...", action.reference, proxy.trigger, proxy.specification);
						try {
							this.executor.execute (proxy);
						} catch (final Throwable exception) {
							Preconditions.checkState (proxy.scheduled.compareAndSet (action, null));
							this.exceptions.traceDeferredException (exception);
						}
					}
					break;
				}
			}
		}
		
		final CallbackReference trigger (final Proxy proxy, final Method method, final Object[] arguments)
		{
			Preconditions.checkState ((method.getReturnType () == Void.class) || (method.getReturnType () == CallbackReference.class));
			this.transcript.traceDebugging ("triggering callback `%{method}` `%{array}` for trigger `%{object:identity}` for class `%{class}`...", method, arguments, proxy.trigger, proxy.specification);
			final CallbackReference reference;
			synchronized (this.monitor) {
				reference = CallbackReference.create (this.facade);
				final InvokeAction action = new InvokeAction (proxy, reference, method, arguments);
				proxy.actions.add (action);
				this.actions.put (reference, action);
				this.transcript.traceDebugging ("enqueued invoke action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}`...", reference, proxy.trigger, proxy.specification);
				this.schedule (proxy);
			}
			return (reference);
		}
		
		final <_Callbacks_ extends Callbacks> CallbackReference unregister (final _Callbacks_ trigger)
		{
			Preconditions.checkNotNull (trigger);
			final CallbackReference reference;
			synchronized (this.monitor) {
				this.transcript.traceDebugging ("unregistering trigger `%{object:identity}`...", trigger);
				final Proxy proxy = this.proxies.get (trigger);
				Preconditions.checkNotNull (proxy);
				for (final Action action : this.actions.values ())
					if (!(action instanceof RegisterAction))
						action.triggerCancel ();
				reference = CallbackReference.create (this.facade);
				final UnregisterAction action = new UnregisterAction (proxy, reference);
				proxy.actions.add (action);
				this.actions.put (reference, action);
				this.transcript.traceDebugging ("enqueued unregister action `%{object:identity}` for trigger `%{object:identity}` for class `%{class}`;", reference, trigger, proxy.specification);
				this.schedule (proxy);
			}
			return (reference);
		}
		
		final WeakHashMap<CallbackReference, Action> actions;
		final TranscriptExceptionTracer exceptions;
		final ExecutorService executor;
		final BasicCallbackReactor facade;
		final Monitor monitor;
		final WeakHashMap<CallbackTrigger, Proxy> proxies;
		final Transcript transcript;
	}
	
	private static final class RegisterAction
			extends Action
	{
		RegisterAction (final Proxy proxy, final CallbackReference reference)
		{
			super (proxy, reference);
		}
	}
	
	private static final class UnregisterAction
			extends Action
	{
		UnregisterAction (final Proxy proxy, final CallbackReference reference)
		{
			super (proxy, reference);
		}
	}
}
