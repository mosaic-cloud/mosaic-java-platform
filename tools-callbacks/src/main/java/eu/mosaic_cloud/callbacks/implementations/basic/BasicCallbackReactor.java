
package eu.mosaic_cloud.callbacks.implementations.basic;


import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractListenableFuture;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.mosaic_cloud.callbacks.core.CallbackFuture;
import eu.mosaic_cloud.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.callbacks.core.Callbacks;
import eu.mosaic_cloud.callbacks.core.CallbacksTrigger;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.transcript.core.Transcript;


public final class BasicCallbackReactor
		extends AbstractService
		implements
			CallbackReactor
{
	private BasicCallbackReactor (final ExecutorService executor)
	{
		super ();
		this.monitor = Monitor.create (this);
		synchronized (this.monitor) {
			this.transcript = Transcript.create (this);
			this.exceptionHandler = new ExceptionHandler ();
			if (executor != null)
				this.executor = executor;
			else {
				final ThreadFactory threadFactory = new ThreadFactoryBuilder ().setDaemon (true).setPriority (Thread.NORM_PRIORITY).setUncaughtExceptionHandler (this.exceptionHandler).setNameFormat (String.format ("%s#%08x#%%d", this.getClass ().getSimpleName (), Integer.valueOf (System.identityHashCode (this)))).build ();
				this.executor = Executors.newFixedThreadPool (Runtime.getRuntime ().availableProcessors (), threadFactory);
			}
			this.proxies = new WeakHashMap<CallbacksTrigger, CallbacksProxy> ();
			this.invocations = new WeakHashMap<CallbackReference, CallbackInvocation> ();
		}
	}
	
	@Override
	public final <_Callbacks_ extends Callbacks> _Callbacks_ register (final Class<_Callbacks_> specification, final _Callbacks_ delegate)
	{
		Preconditions.checkNotNull (specification);
		Preconditions.checkArgument (Callbacks.class.isAssignableFrom (specification));
		Preconditions.checkNotNull (delegate);
		Preconditions.checkArgument (specification.isInstance (delegate));
		final CallbacksTrigger trigger;
		synchronized (this.monitor) {
			this.transcript.traceDebugging ("registering trigger for class `%s` with delegate `%s#%08x`...", specification.getName (), delegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (delegate)));
			final CallbacksProxy proxy;
			try {
				proxy = new CallbacksProxy (this, specification);
			} catch (final RuntimeException exception) {
				this.transcript.traceRethrownException (exception, "unexpected error encountered while registering trigger for class `%s` with delegate `%s#%08x`; re-throwing!", specification.getName (), delegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (delegate)));
				throw (exception);
			} catch (final Error exception) {
				this.transcript.traceRethrownException (exception, "unexpected error encountered while registering trigger for class `%s` with delegate `%s#%08x`; re-throwing!", specification.getName (), delegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (delegate)));
				throw (exception);
			}
			trigger = proxy.trigger;
			this.proxies.put (trigger, proxy);
			proxy.delegate.set (delegate);
			this.transcript.traceDebugging ("registered trigger `%08x` for class `%s` with delegate `%s#%08x`;", Integer.valueOf (System.identityHashCode (trigger)), proxy.specification.getName (), delegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (delegate)));
		}
		return (specification.cast (trigger));
	}
	
	@Override
	public final CallbackFuture resolve (final CallbackReference reference)
	{
		Preconditions.checkNotNull (reference);
		synchronized (this.monitor) {
			final CallbackFuture future = this.invocations.get (reference);
			Preconditions.checkNotNull (future);
			return (future);
		}
	}
	
	@Override
	public final void unregister (final CallbacksTrigger trigger)
	{
		Preconditions.checkNotNull (trigger);
		synchronized (this.monitor) {
			this.transcript.traceDebugging ("unregistering trigger `%08x`...", Integer.valueOf (System.identityHashCode (trigger)));
			final CallbacksProxy proxy = this.proxies.get (trigger);
			Preconditions.checkNotNull (proxy);
			final Object delegate = proxy.delegate.get ();
			Preconditions.checkNotNull (delegate);
			Preconditions.checkState (proxy.delegate.compareAndSet (delegate, null));
			for (final CallbackInvocation invocation : proxy.queuedInvocations)
				invocation.triggerCancel ();
			this.transcript.traceDebugging ("unregistered trigger `%08x` for class `%s` with delegate `%s#%08x`;", Integer.valueOf (System.identityHashCode (trigger)), proxy.specification.getName (), delegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (delegate)));
		}
	}
	
	@Override
	public final void update (final CallbacksTrigger trigger, final Object newDelegate)
	{
		Preconditions.checkNotNull (trigger);
		Preconditions.checkNotNull (newDelegate);
		synchronized (this.monitor) {
			this.transcript.traceDebugging ("updating trigger `%08x`...", Integer.valueOf (System.identityHashCode (trigger)));
			final CallbacksProxy proxy = this.proxies.get (trigger);
			Preconditions.checkNotNull (proxy);
			Preconditions.checkArgument (proxy.specification.isInstance (newDelegate));
			final Object oldDelegate = proxy.delegate.get ();
			Preconditions.checkNotNull (oldDelegate);
			Preconditions.checkState (proxy.delegate.compareAndSet (oldDelegate, newDelegate));
			this.transcript.traceDebugging ("updated trigger `%08x` for class `%s` with delegate `%s#%08x`;", Integer.valueOf (System.identityHashCode (proxy.trigger)), proxy.specification.getName (), newDelegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (newDelegate)));
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
			for (final CallbacksProxy proxy : this.proxies.values ())
				proxy.delegate.set (null);
			for (final CallbackInvocation invocation : this.invocations.values ())
				invocation.triggerCancel ();
			this.notifyStopped ();
		}
	}
	
	final void completed (final CallbackInvocation invocation)
	{
		assert (invocation != null);
		final CallbacksProxy proxy = invocation.proxy;
		proxy.scheduledInvocation.compareAndSet (invocation, null);
		proxy.queuedInvocations.remove (invocation);
		this.schedule (proxy);
	}
	
	final void dispatch (final CallbackInvocation invocation)
	{
		assert (invocation != null);
		final CallbacksProxy proxy = invocation.proxy;
		final Object delegate = proxy.delegate.get ();
		if (delegate == null) {
			invocation.triggerCancel ();
			return;
		}
		this.transcript.traceDebugging ("dispatching callback `%s` for trigger `%08x` for class `%s` with delegate `%s#%08x`...", invocation.method.getName (), Integer.valueOf (System.identityHashCode (proxy.trigger)), proxy.specification.getName (), delegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (delegate)));
		try {
			invocation.method.invoke (delegate, invocation.arguments);
			invocation.triggerSucceed ();
		} catch (final Throwable exception) {
			this.transcript.traceIgnoredException (exception, "unexpected error encountered while dispatching callback `%s` for trigger `%08x` for class `%s` with delegate `%s#%08x`; deferring!", invocation.method.getName (), Integer.valueOf (System.identityHashCode (proxy.trigger)), proxy.specification.getName (), delegate.getClass ().getName (), Integer.valueOf (System.identityHashCode (delegate)));
			invocation.triggerFail (exception);
		}
	}
	
	final void handleException (final Throwable exception)
	{
		this.transcript.traceIgnoredException (exception, "unexpected error encountered; aborting!");
		this.stop ();
	}
	
	final CallbackReference trigger (final CallbacksProxy proxy, final Method method, final Object[] arguments)
	{
		assert (proxy != null);
		Preconditions.checkState ((method.getReturnType () == Void.class) || (method.getReturnType () == CallbackReference.class));
		this.transcript.traceDebugging ("enqueueing callback `%s` for trigger `%08x` for class `%s`...", method.getName (), Integer.valueOf (System.identityHashCode (proxy.trigger)), proxy.specification.getName ());
		final CallbackInvocation invocation;
		synchronized (this.monitor) {
			Preconditions.checkState (proxy.delegate.get () != null);
			invocation = new CallbackInvocation (proxy, method, arguments, CallbackReference.create (this), this.exceptionHandler);
			proxy.queuedInvocations.add (invocation);
			this.invocations.put (invocation.reference, invocation);
		}
		this.schedule (proxy);
		return (invocation.reference);
	}
	
	private final void schedule (final CallbacksProxy proxy)
	{
		synchronized (this.monitor) {
			if (this.state () != State.RUNNING)
				return;
			final CallbackInvocation invocation = proxy.queuedInvocations.peek ();
			if (invocation == null)
				return;
			if (proxy.scheduledInvocation.compareAndSet (null, invocation)) {
				this.transcript.traceDebugging ("scheduling callback `%s` for trigger `%08x` for class `%s`...", invocation.method.getName (), Integer.valueOf (System.identityHashCode (proxy.trigger)), proxy.specification.getName ());
				final Future<Void> future;
				try {
					future = this.executor.submit (invocation);
				} catch (final Error exception) {
					this.transcript.traceError ("unexpected error encountered while scheduling callback `%s` for trigger `%08x` for class `%s`", invocation.method.getName (), Integer.valueOf (System.identityHashCode (proxy.trigger)), proxy.specification.getName ());
					Preconditions.checkState (proxy.scheduledInvocation.compareAndSet (invocation, null));
					return;
				}
				Preconditions.checkState (invocation.future.compareAndSet (null, future));
				Preconditions.checkState (proxy.queuedInvocations.poll () == invocation);
			}
		}
	}
	
	private final ExceptionHandler exceptionHandler;
	private final ExecutorService executor;
	private final WeakHashMap<CallbackReference, CallbackInvocation> invocations;
	private final Monitor monitor;
	private final WeakHashMap<CallbacksTrigger, CallbacksProxy> proxies;
	private final Transcript transcript;
	
	public static final BasicCallbackReactor create ()
	{
		return (new BasicCallbackReactor (null));
	}
	
	public static final BasicCallbackReactor create (final ExecutorService executor)
	{
		return (new BasicCallbackReactor (executor));
	}
	
	static private final class CallbackInvocation
			extends AbstractListenableFuture<Void>
			implements
				Callable<Void>,
				CallbackFuture
	{
		CallbackInvocation (final CallbacksProxy proxy, final Method method, final Object[] arguments, final CallbackReference reference, final UncaughtExceptionHandler exceptionHandler)
		{
			super ();
			this.proxy = proxy;
			this.method = method;
			this.arguments = arguments;
			this.reference = reference;
			this.future = new AtomicReference<Future<Void>> ();
			this.exceptionHandler = exceptionHandler;
		}
		
		@Override
		public final Void call ()
		{
			try {
				this.proxy.reactor.dispatch (this);
			} catch (final Throwable exception) {
				this.exceptionHandler.uncaughtException (Thread.currentThread (), exception);
			}
			return (null);
		}
		
		@Override
		public final boolean cancel (final boolean maybeInterrupt)
		{
			return (this.cancel ());
		}
		
		@Override
		protected final void done ()
		{
			this.proxy.reactor.completed (this);
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
		
		final Object[] arguments;
		final UncaughtExceptionHandler exceptionHandler;
		final AtomicReference<Future<Void>> future;
		final Method method;
		final CallbacksProxy proxy;
		final CallbackReference reference;
	}
	
	static private final class CallbacksProxy
			extends Object
			implements
				InvocationHandler
	{
		CallbacksProxy (final BasicCallbackReactor reactor, final Class<? extends Object> specification)
		{
			super ();
			this.reactor = reactor;
			this.specification = specification;
			this.trigger = (CallbacksTrigger) Proxy.newProxyInstance (specification.getClassLoader (), new Class[] {specification, CallbacksTrigger.class}, this);
			this.delegate = new AtomicReference<Object> (null);
			this.queuedInvocations = new ConcurrentLinkedQueue<CallbackInvocation> ();
			this.scheduledInvocation = new AtomicReference<CallbackInvocation> (null);
		}
		
		@Override
		public final Object invoke (final Object callbacks, final Method method, final Object[] arguments)
		{
			if (CallbacksProxy.equalsMethod.equals (method))
				return (Boolean.valueOf (this.delegate.get () == arguments[0]));
			if (CallbacksProxy.hashCodeMethod.equals (method))
				return (Integer.valueOf (System.identityHashCode (this)));
			return (this.reactor.trigger (this, method, arguments));
		}
		
		final AtomicReference<Object> delegate;
		final ConcurrentLinkedQueue<CallbackInvocation> queuedInvocations;
		final BasicCallbackReactor reactor;
		final AtomicReference<CallbackInvocation> scheduledInvocation;
		final Class<? extends Object> specification;
		final CallbacksTrigger trigger;
		
		static {
			try {
				equalsMethod = Object.class.getMethod ("equals", Object.class);
				hashCodeMethod = Object.class.getMethod ("hashCode");
			} catch (final Exception exception) {
				throw (new Error (exception));
			}
		}
		
		static final Method equalsMethod;
		static final Method hashCodeMethod;
	}
	
	private final class ExceptionHandler
			extends Object
			implements
				UncaughtExceptionHandler
	{
		ExceptionHandler ()
		{
			super ();
		}
		
		@Override
		public final void uncaughtException (final Thread thread, final Throwable exception)
		{
			BasicCallbackReactor.this.handleException (exception);
		}
	}
}
