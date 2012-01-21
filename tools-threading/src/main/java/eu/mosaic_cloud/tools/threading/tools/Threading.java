
package eu.mosaic_cloud.tools.threading.tools;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ManagedThread;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ManagedThreadGroup;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ThreadConfiguration;


public final class Threading
		extends Object
{
	private Threading ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final boolean await (final CountDownLatch latch)
	{
		Preconditions.checkNotNull (latch);
		try {
			latch.await ();
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean await (final CountDownLatch latch, final long timeout)
	{
		Preconditions.checkNotNull (latch);
		Preconditions.checkArgument (timeout >= 0);
		try {
			return (latch.await (timeout, TimeUnit.MILLISECONDS));
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
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
		return (CurrentContext.instance.get ());
	}
	
	public static final Thread getCurrentThread ()
	{
		return (Thread.currentThread ());
	}
	
	public static final ThreadGroup getCurrentThreadGroup ()
	{
		return (Thread.currentThread ().getThreadGroup ());
	}
	
	public static final ThreadGroup getRootThreadGroup ()
	{
		final ThreadGroup root;
		for (ThreadGroup child = Threading.getCurrentThreadGroup (), parent = child.getParent (); true; child = parent, parent = child.getParent ())
			if (parent == null) {
				root = child;
				break;
			}
		return (root);
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
	
	public static final void interrupt (final Iterable<? extends Thread> threads)
	{
		Preconditions.checkNotNull (threads);
		for (final Thread thread : threads) {
			Preconditions.checkNotNull (thread);
			thread.interrupt ();
		}
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
	
	public static final boolean join (final Iterable<? extends Thread> threads)
	{
		return (Threading.join (threads, 0));
	}
	
	public static final boolean join (final Iterable<? extends Thread> threads, final long timeout)
	{
		// Mirrors the code from `java.lang.Thread.join`.
		Preconditions.checkNotNull (threads);
		Preconditions.checkArgument (timeout >= 0);
		try {
			final long begin = System.currentTimeMillis ();
			for (final Thread thread : threads) {
				Preconditions.checkNotNull (thread);
				final long remainingTimeout;
				if (timeout > 0) {
					remainingTimeout = timeout - (System.currentTimeMillis () - begin);
					if (remainingTimeout <= 0)
						return (false);
				} else
					remainingTimeout = 0;
				thread.join (remainingTimeout);
			}
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean join (final Thread thread)
	{
		return (Threading.join (thread, 0));
	}
	
	public static final boolean join (final Thread thread, final long timeout)
	{
		Preconditions.checkNotNull (thread);
		Preconditions.checkArgument (timeout >= 0);
		try {
			thread.join (timeout);
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final void notify (final Object monitor)
	{
		Preconditions.checkNotNull (monitor);
		synchronized (monitor) {
			monitor.notify ();
		}
	}
	
	public static final void notifyAll (final Object monitor)
	{
		Preconditions.checkNotNull (monitor);
		synchronized (monitor) {
			monitor.notifyAll ();
		}
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
	
	public static final int start (final Iterable<? extends Thread> threads)
	{
		Preconditions.checkNotNull (threads);
		int count = 0;
		for (final Thread thread : threads) {
			Preconditions.checkNotNull (thread);
			try {
				thread.start ();
				count++;
			} catch (final IllegalThreadStateException exception) {
				// intentional
			}
		}
		return (count);
	}
	
	public static final boolean wait (final Object monitor)
	{
		return (Threading.wait (monitor, 0));
	}
	
	public static final boolean wait (final Object monitor, final long timeout)
	{
		Preconditions.checkNotNull (monitor);
		Preconditions.checkArgument (timeout >= 0);
		try {
			synchronized (monitor) {
				monitor.wait (timeout);
			}
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
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
	
	private static final class CurrentContext
			extends ThreadLocal<ThreadingContext>
	{
		private CurrentContext ()
		{
			super ();
			this.initialValueActive = false;
		}
		
		@Override
		public final ThreadingContext get ()
		{
			synchronized (this) {
				if (this.initialValueActive)
					return (null);
				return (super.get ());
			}
		}
		
		@Override
		public final void remove ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final void set (final ThreadingContext value)
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		protected final ThreadingContext initialValue ()
		{
			synchronized (this) {
				try {
					this.initialValueActive = true;
					ThreadingContext context = super.initialValue ();
					final Thread thread = Threading.getCurrentThread ();
					if (context == null) {
						if (thread instanceof ManagedThread) {
							context = ((ManagedThread) thread).getContext ();
							Preconditions.checkNotNull (context);
						}
					}
					if (context == null) {
						for (ThreadGroup group = thread.getThreadGroup (); group != null; group = group.getParent ())
							if (group instanceof ManagedThreadGroup) {
								context = ((ManagedThreadGroup) group).getContext ();
								Preconditions.checkNotNull (context);
								break;
							}
					}
					if (context != null) {
						if (!context.isManaged (thread))
							context.registerThread (thread);
					}
					return (context);
				} finally {
					this.initialValueActive = false;
				}
			}
		}
		
		private boolean initialValueActive;
		public static final CurrentContext instance = new CurrentContext ();
	}
}
