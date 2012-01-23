
package eu.mosaic_cloud.tools.threading.core;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;


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
}
