
package eu.mosaic_cloud.tools.threading.tools;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ThreadConfiguration;


public final class Threading
		extends Object
{
	private Threading ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final Thread createAndStartDaemonThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createAndStartThread (threading, new ThreadConfiguration (owner, name, true), runnable));
	}
	
	public static final Thread createAndStartNormalThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createAndStartThread (threading, new ThreadConfiguration (owner, name, false), runnable));
	}
	
	public static final Thread createAndStartThread (final ThreadingContext threading, final ThreadConfiguration configuration, final Runnable runnable)
	{
		final Thread thread = Threading.createThread (threading, configuration, runnable);
		thread.start ();
		return (thread);
	}
	
	public static final Thread createDaemonThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createThread (threading, new ThreadConfiguration (owner, name, true), runnable));
	}
	
	public static final Thread createNormalThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createThread (threading, new ThreadConfiguration (owner, name, false), runnable));
	}
	
	public static final Thread createThread (final ThreadingContext threading, final ThreadConfiguration configuration, final Runnable runnable)
	{
		Preconditions.checkNotNull (threading);
		return (threading.newThread (configuration, runnable));
	}
	
	public static final void exit ()
	{
		Threading.exit (Threading.defaultAbortExitCode);
	}
	
	public static final void exit (final int exitCode)
	{
		Threading.exitNoWait (exitCode);
		Threading.loop ();
	}
	
	public static final void exitNoWait ()
	{
		Threading.exitNoWait (Threading.defaultAbortExitCode);
	}
	
	public static final void exitNoWait (final int exitCode)
	{
		final int finalExitCode;
		if ((exitCode >= 0) && (exitCode <= 255))
			finalExitCode = exitCode;
		else
			finalExitCode = Threading.defaultAbortExitCode;
		System.exit (finalExitCode);
	}
	
	public static final ThreadingContext getCurrentContext ()
	{
		final ThreadingContext context = Threading.context.get ();
		Preconditions.checkState (context != null);
		return (context);
	}
	
	public static final Thread getCurrentThread ()
	{
		return (Thread.currentThread ());
	}
	
	public static final ThreadGroup getCurrentThreadGroup ()
	{
		return (Thread.currentThread ().getThreadGroup ());
	}
	
	public static final void halt ()
	{
		Threading.halt (Threading.defaultHaltExitCode);
	}
	
	public static final void halt (final int exitCode)
	{
		final int finalExitCode;
		if ((exitCode >= 0) && (exitCode <= 255))
			finalExitCode = exitCode;
		else
			finalExitCode = Threading.defaultHaltExitCode;
		Runtime.getRuntime ().halt (finalExitCode);
		Threading.loop ();
	}
	
	public static final void interruptCurrentThread ()
	{
		Thread.currentThread ().interrupt ();
	}
	
	public static final boolean isCurrentThread (final Thread thread)
	{
		Preconditions.checkNotNull (thread);
		return (thread == Thread.currentThread ());
	}
	
	public static final boolean isCurrentThreadGroup (final ThreadGroup group)
	{
		Preconditions.checkNotNull (group);
		return (group == Thread.currentThread ().getThreadGroup ());
	}
	
	public static final boolean isCurrentThreadInterrupted ()
	{
		return (Thread.currentThread ().isInterrupted ());
	}
	
	public static final void registerExitCallback (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (owner);
		Preconditions.checkNotNull (runnable);
		Preconditions.checkNotNull (runnable);
		Runtime.getRuntime ().addShutdownHook (threading.newThread (new ThreadConfiguration (owner, name, true), runnable));
	}
	
	public static final ThreadingContext sequezeThreadingContextOutOfDryRock ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final void setCurrentContext (final ThreadingContext context)
	{
		Preconditions.checkNotNull (context);
		synchronized (context) {
			Preconditions.checkState (Threading.context.get () == null);
			Threading.context.set (context);
		}
	}
	
	public static final boolean sleep (final long timeout)
	{
		try {
			Thread.sleep (timeout);
			return (true);
		} catch (final InterruptedException exception) {
			Thread.currentThread ().interrupt ();
			return (false);
		}
	}
	
	private static final void loop ()
	{
		while (true) {
			final Object object = new Object ();
			synchronized (object) {
				try {
					object.wait ();
				} catch (final InterruptedException exception) {
					Thread.currentThread ().interrupt ();
				}
			}
		}
	}
	
	public static final int defaultAbortExitCode = 1;
	public static final int defaultHaltExitCode = 255;
	private static final ThreadLocal<ThreadingContext> context = new ThreadLocal<ThreadingContext> ();
}
