
package eu.mosaic_cloud.tools.threading.implementations.basic;


import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;


public final class BasicThreadingContext
		implements
			ThreadingContext
{
	private BasicThreadingContext (final ThreadConfiguration configuration)
	{
		super ();
		Preconditions.checkNotNull (configuration);
		final Object owner = configuration.owner.get ();
		Preconditions.checkNotNull (owner);
		this.configuration = configuration;
		this.group = new BasicThreadGroup (Threading.getCurrentThreadGroup (), this.configuration);
	}
	
	@Override
	public final ExecutorService newCachedThreadPool (final ThreadConfiguration configuration)
	{
		return (Executors.unconfigurableExecutorService (Executors.newCachedThreadPool (this.newThreadFactory (configuration, true))));
	}
	
	@Override
	public final ExecutorService newFixedThreadPool (final ThreadConfiguration configuration, final int threads)
	{
		return (Executors.unconfigurableExecutorService (Executors.newFixedThreadPool (threads, this.newThreadFactory (configuration, true))));
	}
	
	@Override
	public final ScheduledExecutorService newScheduledThreadPool (final ThreadConfiguration configuration, final int coreThreads)
	{
		return (Executors.unconfigurableScheduledExecutorService (Executors.newScheduledThreadPool (coreThreads, this.newThreadFactory (configuration, true))));
	}
	
	@Override
	public final ExecutorService newSingleThreadExecutor (final ThreadConfiguration configuration)
	{
		return (Executors.unconfigurableExecutorService (Executors.newSingleThreadExecutor (this.newThreadFactory (configuration, false))));
	}
	
	@Override
	public final ScheduledExecutorService newSingleThreadScheduledExecutor (final ThreadConfiguration configuration)
	{
		return (Executors.unconfigurableScheduledExecutorService (Executors.newSingleThreadScheduledExecutor (this.newThreadFactory (configuration, false))));
	}
	
	@Override
	public final Thread newThread (final ThreadConfiguration configuration, final Runnable runnable)
	{
		final Object thisOwner = this.configuration.owner.get ();
		Preconditions.checkNotNull (thisOwner);
		return (new BasicThread (this.group, configuration, runnable, -1));
	}
	
	@Override
	public final ThreadFactory newThreadFactory (final ThreadConfiguration configuration)
	{
		return (this.newThreadFactory (configuration, true));
	}
	
	public final ThreadFactory newThreadFactory (final ThreadConfiguration configuration, final boolean index)
	{
		final Object thisOwner = this.configuration.owner.get ();
		Preconditions.checkNotNull (thisOwner);
		return (new BasicThreadFactory (new BasicThreadGroup (this.group, configuration), configuration, index));
	}
	
	final WeakReference<Object> buildOwner (final ThreadConfiguration configuration)
	{
		Preconditions.checkNotNull (configuration.owner);
		return (new WeakReference<Object> (configuration.owner));
	}
	
	private final ThreadConfiguration configuration;
	private final BasicThreadGroup group;
	
	public static final BasicThreadingContext create (final Object owner, final Thread.UncaughtExceptionHandler catcher)
	{
		return (new BasicThreadingContext (new ThreadConfiguration (owner, catcher)));
	}
	
	static final String buildThreadGroupName (final ThreadGroup parent, final ThreadConfiguration configuration)
	{
		Preconditions.checkNotNull (configuration);
		final Object owner = configuration.owner.get ();
		Preconditions.checkNotNull (owner);
		Preconditions.checkArgument ((configuration.name == null) || ThreadingContext.namePattern.matcher (configuration.name).matches ());
		final String ownerName;
		if (owner instanceof Class)
			ownerName = ((Class<?>) owner).getCanonicalName ();
		else
			ownerName = String.format ("%s/%08x", owner.getClass ().getCanonicalName (), Integer.valueOf (System.identityHashCode (owner)));
		final String finalName;
		if (configuration.name == null)
			finalName = ownerName;
		else
			finalName = String.format ("%s/%s", ownerName, configuration.name);
		return (finalName);
	}
	
	static final String buildThreadName (final ThreadGroup group, final ThreadConfiguration configuration, final int index)
	{
		Preconditions.checkNotNull (configuration);
		final Object owner = configuration.owner.get ();
		Preconditions.checkNotNull (owner);
		Preconditions.checkArgument ((index == -1) || (index >= 1));
		Preconditions.checkArgument ((configuration.name == null) || ThreadingContext.namePattern.matcher (configuration.name).matches ());
		final String ownerName;
		if (group != null)
			ownerName = group.getName ();
		else if (owner instanceof Class)
			ownerName = ((Class<?>) owner).getCanonicalName ();
		else
			ownerName = String.format ("%s/%08x", owner.getClass ().getCanonicalName (), Integer.valueOf (System.identityHashCode (owner)));
		final String finalName;
		if (index != -1)
			if (configuration.name != null)
				finalName = String.format ("%s//%s/%02d", ownerName, configuration.name, Integer.valueOf (index));
			else
				finalName = String.format ("%s//%02d", ownerName, Integer.valueOf (index));
		else if (configuration.name != null)
			finalName = String.format ("%s//%s", group.getName (), configuration.name);
		else
			finalName = ownerName;
		return (finalName);
	}
	
	private final class BasicThread
			extends Thread
	{
		BasicThread (final BasicThreadGroup group, final ThreadConfiguration configuration, final Runnable runnable, final int index)
		{
			super (group, runnable);
			Preconditions.checkNotNull (group);
			Preconditions.checkNotNull (configuration);
			Preconditions.checkArgument (configuration.daemon || !group.isDaemon ());
			Preconditions.checkArgument ((configuration.priority == -1) || (configuration.priority <= group.getMaxPriority ()));
			Preconditions.checkNotNull (runnable);
			Preconditions.checkArgument ((index == -1) || (index >= 1));
			this.group = group;
			this.configuration = configuration;
			this.index = index;
			this.running = new AtomicBoolean (false);
			this.setDaemon (this.configuration.daemon);
			if (configuration.priority != -1)
				this.setPriority (configuration.priority);
			else
				this.setPriority (Math.min (group.getMaxPriority (), Thread.NORM_PRIORITY));
			if (this.configuration.classLoader != null)
				this.setContextClassLoader (this.configuration.classLoader);
			this.setName (BasicThreadingContext.buildThreadName (this.group, this.configuration, this.index));
		}
		
		@Override
		public final void run ()
		{
			Preconditions.checkState (this == Thread.currentThread ());
			Preconditions.checkState (this.running.compareAndSet (false, true));
			Threading.setCurrentContext (BasicThreadingContext.this);
			super.run ();
		}
		
		private final ThreadConfiguration configuration;
		private final BasicThreadGroup group;
		private final int index;
		private final AtomicBoolean running;
	}
	
	private final class BasicThreadFactory
			extends Object
			implements
				ThreadFactory
	{
		BasicThreadFactory (final BasicThreadGroup group, final ThreadConfiguration configuration, final boolean index)
		{
			super ();
			Preconditions.checkNotNull (group);
			Preconditions.checkNotNull (configuration);
			Preconditions.checkArgument (configuration.daemon || !group.isDaemon ());
			Preconditions.checkArgument ((configuration.priority == -1) || (configuration.priority <= group.getMaxPriority ()));
			this.group = group;
			this.configuration = configuration;
			this.index = index ? new AtomicInteger (1) : null;
		}
		
		@Override
		public final Thread newThread (final Runnable runnable)
		{
			return (new BasicThread (this.group, this.configuration.setName (null), runnable, this.index != null ? this.index.incrementAndGet () : -1));
		}
		
		private final ThreadConfiguration configuration;
		private final BasicThreadGroup group;
		private final AtomicInteger index;
	}
	
	private final class BasicThreadGroup
			extends ThreadGroup
	{
		BasicThreadGroup (final BasicThreadGroup parent, final ThreadConfiguration configuration)
		{
			this ((ThreadGroup) parent, configuration);
			Preconditions.checkNotNull (parent);
		}
		
		BasicThreadGroup (final ThreadGroup parent, final ThreadConfiguration configuration)
		{
			super (parent, BasicThreadingContext.buildThreadGroupName (null, configuration));
			Preconditions.checkNotNull (parent);
			this.parent = (parent instanceof BasicThreadGroup) ? (BasicThreadGroup) parent : null;
			this.configuration = configuration;
			super.setDaemon (configuration.daemon);
			if (configuration.priority != -1)
				this.setMaxPriority (configuration.priority);
			else
				this.setMaxPriority (parent.getMaxPriority ());
		}
		
		@Override
		public final int activeCount ()
		{
			return super.activeCount ();
		}
		
		@Override
		public final int activeGroupCount ()
		{
			return super.activeGroupCount ();
		}
		
		@SuppressWarnings ("deprecation")
		@Override
		@Deprecated
		public final boolean allowThreadSuspension (final boolean b)
		{
			return super.allowThreadSuspension (b);
		}
		
		@Override
		public final int enumerate (final Thread[] collector)
		{
			return super.enumerate (collector);
		}
		
		@Override
		public final int enumerate (final Thread[] collector, final boolean recursive)
		{
			return super.enumerate (collector, recursive);
		}
		
		@Override
		public final int enumerate (final ThreadGroup[] collector)
		{
			return super.enumerate (collector);
		}
		
		@Override
		public final int enumerate (final ThreadGroup[] collector, final boolean recursive)
		{
			return super.enumerate (collector, recursive);
		}
		
		@Override
		public final boolean isDestroyed ()
		{
			return super.isDestroyed ();
		}
		
		@Override
		public final void list ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final String toString ()
		{
			return super.toString ();
		}
		
		@Override
		public final void uncaughtException (final Thread thread, final Throwable exception)
		{
			super.uncaughtException (thread, exception);
		}
		
		private final ThreadConfiguration configuration;
		private final BasicThreadGroup parent;
	}
}
