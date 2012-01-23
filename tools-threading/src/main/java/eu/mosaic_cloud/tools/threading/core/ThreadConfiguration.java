
package eu.mosaic_cloud.tools.threading.core;


import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import com.google.common.base.Preconditions;


public final class ThreadConfiguration
		extends Object
{
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
	
	public static final ThreadConfiguration create (final Object owner)
	{
		return (ThreadConfiguration.create (owner, null, true, -1, null));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name)
	{
		return (ThreadConfiguration.create (owner, name, true, -1, null));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon)
	{
		return (ThreadConfiguration.create (owner, name, daemon, -1, null));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon, final int priority)
	{
		return (ThreadConfiguration.create (owner, name, daemon, priority, null));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon, final int priority, final Thread.UncaughtExceptionHandler catcher)
	{
		return (new ThreadConfiguration (new WeakReference<Object> (owner), name, daemon, priority, catcher, null));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon, final Thread.UncaughtExceptionHandler catcher)
	{
		return (ThreadConfiguration.create (owner, name, daemon, -1, catcher));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final Thread.UncaughtExceptionHandler catcher)
	{
		return (ThreadConfiguration.create (owner, name, true, -1, catcher));
	}
	
	public static final ThreadConfiguration create (final Object owner, final Thread.UncaughtExceptionHandler catcher)
	{
		return (ThreadConfiguration.create (owner, null, true, -1, catcher));
	}
}
