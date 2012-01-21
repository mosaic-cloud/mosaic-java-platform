
package eu.mosaic_cloud.tools.threading.core;


import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;


public interface ThreadingContext
{
	public abstract ThreadGroup getDefaultThreadGroup ();
	
	public abstract boolean isManaged (final Thread thread);
	
	public abstract boolean isManaged (final ThreadGroup group);
	
	public abstract boolean isSealed ();
	
	public abstract ExecutorService newCachedThreadPool (final ThreadConfiguration configuration);
	
	public abstract ExecutorService newFixedThreadPool (final ThreadConfiguration configuration, int threads);
	
	public abstract ScheduledExecutorService newScheduledThreadPool (final ThreadConfiguration configuration, int coreThreads);
	
	public abstract ExecutorService newSingleThreadExecutor (final ThreadConfiguration configuration);
	
	public abstract ScheduledExecutorService newSingleThreadScheduledExecutor (final ThreadConfiguration configuration);
	
	public abstract Thread newThread (final ThreadConfiguration configuration, final Runnable runnable);
	
	public abstract ThreadFactory newThreadFactory (final ThreadConfiguration configuration);
	
	public abstract void registerThread (final Thread thread);
	
	public static final Pattern namePattern = Pattern.compile ("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");
	
	public static interface ManagedThread
	{
		public abstract ThreadingContext getContext ();
	}
	
	public static interface ManagedThreadGroup
	{
		public abstract ThreadingContext getContext ();
	}
	
	public static final class ThreadConfiguration
			extends Object
	{
		public ThreadConfiguration (final Object owner)
		{
			this (owner, null, true, -1, null);
		}
		
		public ThreadConfiguration (final Object owner, final String name)
		{
			this (owner, name, true, -1, null);
		}
		
		public ThreadConfiguration (final Object owner, final String name, final boolean daemon)
		{
			this (owner, name, daemon, -1, null);
		}
		
		public ThreadConfiguration (final Object owner, final String name, final boolean daemon, final int priority)
		{
			this (owner, name, daemon, priority, null);
		}
		
		public ThreadConfiguration (final Object owner, final String name, final boolean daemon, final int priority, final Thread.UncaughtExceptionHandler catcher)
		{
			this (new WeakReference<Object> (owner), name, daemon, priority, catcher, null);
		}
		
		public ThreadConfiguration (final Object owner, final String name, final boolean daemon, final Thread.UncaughtExceptionHandler catcher)
		{
			this (owner, name, daemon, -1, catcher);
		}
		
		public ThreadConfiguration (final Object owner, final String name, final Thread.UncaughtExceptionHandler catcher)
		{
			this (owner, name, true, -1, catcher);
		}
		
		public ThreadConfiguration (final Object owner, final Thread.UncaughtExceptionHandler catcher)
		{
			this (owner, null, true, -1, catcher);
		}
		
		private ThreadConfiguration (final Reference<Object> owner, final String name, final boolean daemon, final int priority, final Thread.UncaughtExceptionHandler catcher, final ClassLoader classLoader)
		{
			super ();
			Preconditions.checkNotNull (owner);
			Preconditions.checkArgument ((name == null) || ThreadingContext.namePattern.matcher (name).matches ());
			Preconditions.checkArgument ((priority == -1) || ((priority >= Thread.MIN_PRIORITY) && (priority <= Thread.MAX_PRIORITY)));
			this.owner = owner;
			this.name = name;
			this.daemon = daemon;
			this.priority = priority;
			this.catcher = catcher;
			this.classLoader = classLoader;
		}
		
		public final ThreadConfiguration setClassLoader (final ClassLoader classLoader)
		{
			return (new ThreadConfiguration (this.owner, this.name, this.daemon, this.priority, this.catcher, classLoader));
		}
		
		public final ThreadConfiguration setName (final String name)
		{
			return (new ThreadConfiguration (this.owner, name, this.daemon, this.priority, this.catcher, this.classLoader));
		}
		
		public final Thread.UncaughtExceptionHandler catcher;
		public final ClassLoader classLoader;
		public final boolean daemon;
		public final String name;
		public final Reference<Object> owner;
		public final int priority;
	}
}
