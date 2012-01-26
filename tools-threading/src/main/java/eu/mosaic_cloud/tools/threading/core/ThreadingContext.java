
package eu.mosaic_cloud.tools.threading.core;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;


public interface ThreadingContext
		extends
			Joinable
{
	public abstract ExecutorService createCachedThreadPool (final ThreadConfiguration configuration);
	
	public abstract ExecutorService createFixedThreadPool (final ThreadConfiguration configuration, int threads);
	
	public abstract ScheduledExecutorService createScheduledThreadPool (final ThreadConfiguration configuration, int coreThreads);
	
	public abstract ExecutorService createSingleThreadExecutor (final ThreadConfiguration configuration);
	
	public abstract ScheduledExecutorService createSingleThreadScheduledExecutor (final ThreadConfiguration configuration);
	
	public abstract Thread createThread (final ThreadConfiguration configuration, final Runnable runnable);
	
	public abstract ThreadFactory createThreadFactory (final ThreadConfiguration configuration);
	
	public abstract ThreadGroup getDefaultThreadGroup ();
	
	public abstract boolean isManaged (final Thread thread);
	
	public abstract boolean isManaged (final ThreadGroup group);
	
	public abstract boolean isSealed ();
	
	public abstract void registerThread (final Thread thread);
	
	public static interface ManagedThread
			extends
				Joinable
	{
		public abstract ThreadingContext getContext ();
	}
	
	public static interface ManagedThreadGroup
	{
		public abstract ThreadingContext getContext ();
	}
}
