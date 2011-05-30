
package eu.mosaic_cloud.tools;


import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;


public final class DefaultThreadPoolFactory
		implements
			ThreadFactory
{
	private DefaultThreadPoolFactory (final Object owner, final boolean daemon, final int priority, final UncaughtExceptionHandler exceptionHandler)
	{
		super ();
		Preconditions.checkNotNull (owner);
		Preconditions.checkNotNull (exceptionHandler);
		this.group = new ThreadGroup (String.format ("%s#%08x", owner.getClass ().getSimpleName (), Integer.valueOf (System.identityHashCode (owner))));
		this.nameFormat = String.format ("%s#%08x#%%d", owner.getClass ().getSimpleName (), Integer.valueOf (System.identityHashCode (owner)));
		this.daemon = daemon;
		this.priority = priority;
		this.exceptionHandler = exceptionHandler;
		this.counter = new AtomicInteger (1);
	}
	
	@Override
	public final Thread newThread (final Runnable runnable)
	{
		final Thread thread = new Thread (this.group, runnable, String.format (this.nameFormat, Integer.valueOf (this.counter.getAndIncrement ())));
		thread.setDaemon (this.daemon);
		thread.setPriority (this.priority);
		thread.setUncaughtExceptionHandler (this.exceptionHandler);
		return (thread);
	}
	
	private final AtomicInteger counter;
	private final boolean daemon;
	private final UncaughtExceptionHandler exceptionHandler;
	private final ThreadGroup group;
	private final String nameFormat;
	private final int priority;
	
	public static final DefaultThreadPoolFactory create (final Object owner)
	{
		return (new DefaultThreadPoolFactory (owner, true, Thread.NORM_PRIORITY, UncaughtExceptionHandlers.systemExit ()));
	}
	
	public static final DefaultThreadPoolFactory create (final Object owner, final boolean daemon, final int priority, final UncaughtExceptionHandler exceptionHandler)
	{
		return (new DefaultThreadPoolFactory (owner, daemon, priority, exceptionHandler));
	}
}
