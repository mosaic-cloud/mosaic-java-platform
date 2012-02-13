/*
 * #%L
 * mosaic-tools-threading
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.tools.threading.tools;


import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Atomics;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ManagedThread;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext.ManagedThreadGroup;


public final class Threading
		extends Object
{
	private Threading ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final boolean acquire (final Semaphore semaphore)
	{
		return (Threading.acquire (semaphore, 1));
	}
	
	public static final boolean acquire (final Semaphore semaphore, final int tokens)
	{
		return (Threading.acquire (semaphore, tokens, -1));
	}
	
	public static final boolean acquire (final Semaphore semaphore, final int tokens, final long timeout)
	{
		Preconditions.checkNotNull (semaphore);
		Preconditions.checkArgument (tokens >= 0);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout > 0)
				return (semaphore.tryAcquire (tokens, timeout, TimeUnit.MILLISECONDS));
			else if (timeout == 0)
				return (semaphore.tryAcquire ());
			else if (timeout == -1) {
				semaphore.acquire (tokens);
				return (true);
			} else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean acquire (final Semaphore semaphore, final long timeout)
	{
		return (Threading.acquire (semaphore, 1, timeout));
	}
	
	public static final boolean await (final Condition condition)
	{
		return (Threading.await (condition, -1));
	}
	
	public static final boolean await (final Condition condition, final long timeout)
	{
		Preconditions.checkNotNull (condition);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout >= 0)
				return (condition.await (timeout, TimeUnit.MILLISECONDS));
			else if (timeout == -1) {
				condition.await ();
				return (true);
			} else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean await (final CountDownLatch latch)
	{
		return (Threading.await (latch, -1));
	}
	
	public static final boolean await (final CountDownLatch latch, final long timeout)
	{
		Preconditions.checkNotNull (latch);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout >= 0)
				return (latch.await (timeout, TimeUnit.MILLISECONDS));
			else if (timeout == -1) {
				latch.await ();
				return (true);
			} else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final int await (final CyclicBarrier barrier)
	{
		return (Threading.await (barrier, -1));
	}
	
	public static final int await (final CyclicBarrier barrier, final long timeout)
	{
		Preconditions.checkNotNull (barrier);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout >= 0)
				return (barrier.await (timeout, TimeUnit.MILLISECONDS));
			else if (timeout == -1)
				return (barrier.await ());
			else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (-1);
		} catch (final TimeoutException exception) {
			return (-1);
		} catch (final BrokenBarrierException exception) {
			return (-1);
		}
	}
	
	public static final <_Object_ extends Object> _Object_ await (final Future<_Object_> future)
			throws ExecutionException
	{
		return (Threading.await (future, null));
	}
	
	public static final <_Object_ extends Object> _Object_ await (final Future<_Object_> future, final _Object_ timeoutMarker)
			throws ExecutionException
	{
		return (Threading.await (future, -1, timeoutMarker));
	}
	
	public static final <_Object_ extends Object> _Object_ await (final Future<_Object_> future, final long timeout)
			throws ExecutionException
	{
		return (Threading.await (future, timeout, null));
	}
	
	public static final <_Object_ extends Object> _Object_ await (final Future<_Object_> future, final long timeout, final _Object_ timeoutMarker)
			throws ExecutionException
	{
		Preconditions.checkNotNull (future);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			final _Object_ received;
			if (timeout >= 0)
				received = future.get (timeout, TimeUnit.MILLISECONDS);
			else if (timeout == -1)
				received = future.get ();
			else
				throw (new AssertionError ());
			return (received);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (timeoutMarker);
		} catch (final TimeoutException exception) {
			return (timeoutMarker);
		}
	}
	
	public static final <_Object_ extends Object> _Object_ awaitOrCatch (final Future<_Object_> future)
	{
		return (Threading.awaitOrCatch (future, null, null));
	}
	
	public static final <_Object_ extends Object> _Object_ awaitOrCatch (final Future<_Object_> future, final _Object_ timeoutMarker, final _Object_ exceptionMarker)
	{
		return (Threading.awaitOrCatch (future, -1, timeoutMarker, exceptionMarker));
	}
	
	public static final <_Object_ extends Object> _Object_ awaitOrCatch (final Future<_Object_> future, final long timeout)
	{
		return (Threading.awaitOrCatch (future, timeout, null, null));
	}
	
	public static final <_Object_ extends Object> _Object_ awaitOrCatch (final Future<_Object_> future, final long timeout, final _Object_ timeoutMarker, final _Object_ exceptionMarker)
	{
		try {
			return (Threading.await (future, timeout, timeoutMarker));
		} catch (final ExecutionException exception) {
			return (exceptionMarker);
		}
	}
	
	public static final Thread createAndStartDaemonThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createAndStartThread (threading, ThreadConfiguration.create (owner, name, true), runnable));
	}
	
	public static final Thread createAndStartDaemonThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable, final UncaughtExceptionHandler catcher)
	{
		return (Threading.createAndStartThread (threading, ThreadConfiguration.create (owner, name, true, catcher), runnable));
	}
	
	public static final Thread createAndStartNormalThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createAndStartThread (threading, ThreadConfiguration.create (owner, name, false), runnable));
	}
	
	public static final Thread createAndStartNormalThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable, final UncaughtExceptionHandler catcher)
	{
		return (Threading.createAndStartThread (threading, ThreadConfiguration.create (owner, name, false, catcher), runnable));
	}
	
	public static final Thread createAndStartThread (final ThreadingContext context, final ThreadConfiguration configuration, final Runnable runnable)
	{
		return (Threading.createThread (context, configuration, runnable, true));
	}
	
	public static final Thread createDaemonThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createThread (threading, ThreadConfiguration.create (owner, name, true), runnable));
	}
	
	public static final Thread createDaemonThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable, final UncaughtExceptionHandler catcher)
	{
		return (Threading.createThread (threading, ThreadConfiguration.create (owner, name, true, catcher), runnable));
	}
	
	public static final Thread createNormalThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable)
	{
		return (Threading.createThread (threading, ThreadConfiguration.create (owner, name, false), runnable));
	}
	
	public static final Thread createNormalThread (final ThreadingContext threading, final Object owner, final String name, final Runnable runnable, final UncaughtExceptionHandler catcher)
	{
		return (Threading.createThread (threading, ThreadConfiguration.create (owner, name, false, catcher), runnable));
	}
	
	public static final Thread createThread (final ThreadingContext context, final ThreadConfiguration configuration, final Runnable runnable)
	{
		return (Threading.createThread (context, configuration, runnable, false));
	}
	
	public static final Thread createThread (final ThreadingContext context, final ThreadConfiguration configuration, final Runnable runnable, final boolean start)
	{
		Preconditions.checkNotNull (context);
		Preconditions.checkNotNull (configuration);
		Preconditions.checkNotNull (runnable);
		final Thread thread = context.createThread (configuration, runnable);
		if (start)
			thread.start ();
		return (thread);
	}
	
	public static final <_Object_ extends Object> _Object_ exchange (final Exchanger<_Object_> exchanger, final _Object_ sent)
	{
		return (Threading.exchange (exchanger, sent, -1));
	}
	
	public static final <_Object_ extends Object> _Object_ exchange (final Exchanger<_Object_> exchanger, final _Object_ sent, final long timeout)
	{
		Preconditions.checkNotNull (exchanger);
		Preconditions.checkNotNull (sent);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			final _Object_ received;
			if (timeout >= 0)
				received = exchanger.exchange (sent, timeout, TimeUnit.MILLISECONDS);
			else if (timeout == -1)
				received = exchanger.exchange (sent);
			else
				throw (new AssertionError ());
			Preconditions.checkNotNull (received);
			return (received);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (null);
		} catch (final TimeoutException exception) {
			return (null);
		}
	}
	
	public static final void exit ()
	{
		Threading.exit (Threading.defaultAbortExitCode);
	}
	
	public static final void exit (final int exitCode)
	{
		final int finalExitCode;
		if ((exitCode >= 0) && (exitCode <= 255))
			finalExitCode = exitCode;
		else
			finalExitCode = Threading.defaultAbortExitCode;
		Runtime.getRuntime ().exit (finalExitCode);
		Threading.loop ();
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
	
	@Deprecated
	public static final ThreadingContext getDefaultContext ()
	{
		final ThreadingContext context = Threading.defaultContext.get ();
		Preconditions.checkState (context != null);
		return (context);
	}
	
	public static final ThreadGroup getRootThreadGroup ()
	{
		final ThreadGroup root;
		for (ThreadGroup child = Thread.currentThread ().getThreadGroup (), parent = child.getParent (); true; child = parent, parent = child.getParent ())
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
		for (final Thread thread : threads)
			Threading.interrupt (thread);
	}
	
	public static final void interrupt (final Thread thread)
	{
		Preconditions.checkNotNull (thread);
		thread.interrupt ();
	}
	
	public static final void interruptCurrentThread ()
	{
		Threading.interrupt (Threading.getCurrentThread ());
	}
	
	public static final boolean isCurrentContext (final ThreadingContext context)
	{
		return (context == Threading.getCurrentContext ());
	}
	
	public static final boolean isCurrentThread (final Thread thread)
	{
		Preconditions.checkNotNull (thread);
		return (thread == Threading.getCurrentThread ());
	}
	
	public static final boolean isCurrentThreadGroup (final ThreadGroup group)
	{
		Preconditions.checkNotNull (group);
		return (group == Threading.getCurrentThreadGroup ());
	}
	
	public static final boolean isCurrentThreadInterrupted ()
	{
		return (Threading.getCurrentThread ().isInterrupted ());
	}
	
	public static final boolean join (final ExecutorService executor)
	{
		return (Threading.join (executor, -1));
	}
	
	public static final boolean join (final ExecutorService executor, final long timeout)
	{
		Preconditions.checkNotNull (executor);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			final long timeoutAmount = (timeout >= 0) ? timeout : Long.MAX_VALUE;
			final TimeUnit timeoutUnit = (timeout >= 0) ? TimeUnit.MILLISECONDS : TimeUnit.DAYS;
			return (executor.awaitTermination (timeoutAmount, timeoutUnit));
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean join (final Iterable<? extends Thread> threads)
	{
		return (Threading.join (threads, -1));
	}
	
	public static final boolean join (final Iterable<? extends Thread> threads, final long timeout)
	{
		// Mirrors the code from `java.lang.Thread.join(long)`.
		Preconditions.checkNotNull (threads);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			final long begin = System.currentTimeMillis ();
			for (final Thread thread : threads) {
				Preconditions.checkNotNull (thread);
				final long remainingTimeout;
				if (timeout > 0) {
					remainingTimeout = timeout - (System.currentTimeMillis () - begin);
					if (remainingTimeout <= 0)
						return (false);
				} else if (timeout == 0)
					remainingTimeout = 1;
				else if (timeout == -1)
					remainingTimeout = -1;
				else
					throw (new AssertionError ());
				if (remainingTimeout > 0)
					thread.join (remainingTimeout);
				else if (remainingTimeout == -1)
					thread.join ();
				else
					throw (new AssertionError ());
			}
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean join (final Process process)
	{
		Preconditions.checkNotNull (process);
		try {
			process.waitFor ();
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean join (final Thread thread)
	{
		return (Threading.join (thread, -1));
	}
	
	public static final boolean join (final Thread thread, final long timeout)
	{
		Preconditions.checkNotNull (thread);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			final long remainingTimeout;
			if (timeout > 0)
				remainingTimeout = timeout;
			else if (timeout == 0)
				remainingTimeout = 1;
			else if (timeout == -1)
				remainingTimeout = -1;
			else
				throw (new AssertionError ());
			if (remainingTimeout > 0)
				thread.join (remainingTimeout);
			else if (remainingTimeout == -1)
				thread.join ();
			else
				throw (new AssertionError ());
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean lock (final Lock lock)
	{
		return (Threading.lock (lock, -1));
	}
	
	public static final boolean lock (final Lock lock, final long timeout)
	{
		Preconditions.checkNotNull (lock);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout > 0)
				return (lock.tryLock (timeout, TimeUnit.MILLISECONDS));
			else if (timeout == 0)
				return (lock.tryLock ());
			else if (timeout == -1) {
				lock.lockInterruptibly ();
				return (true);
			} else
				throw (new AssertionError ());
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
	
	public static final <_Object_ extends Object> boolean offer (final BlockingQueue<_Object_> queue, final _Object_ object)
	{
		return (Threading.offer (queue, object, -1));
	}
	
	public static final <_Object_ extends Object> boolean offer (final BlockingQueue<_Object_> queue, final _Object_ object, final long timeout)
	{
		Preconditions.checkNotNull (queue);
		Preconditions.checkNotNull (object);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout > 0)
				return (queue.offer (object, timeout, TimeUnit.MILLISECONDS));
			else if (timeout == 0)
				return (queue.offer (object));
			else if (timeout == -1) {
				queue.put (object);
				return (true);
			} else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final <_Object_ extends Object> _Object_ poll (final BlockingQueue<_Object_> queue)
	{
		return (Threading.poll (queue, -1));
	}
	
	public static final <_Object_ extends Object> _Object_ poll (final BlockingQueue<_Object_> queue, final long timeout)
	{
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout > 0)
				return (queue.poll (timeout, TimeUnit.MILLISECONDS));
			else if (timeout == 0)
				return (queue.poll ());
			else if (timeout == -1)
				return (queue.take ());
			else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (null);
		}
	}
	
	public static final void registerExitCallback (final ThreadingContext context, final Object owner, final String name, final Runnable runnable)
	{
		Preconditions.checkNotNull (context);
		Preconditions.checkNotNull (owner);
		Preconditions.checkNotNull (runnable);
		Preconditions.checkNotNull (runnable);
		Runtime.getRuntime ().addShutdownHook (context.createThread (ThreadConfiguration.create (owner, name, true), runnable));
	}
	
	public static final <_Object_ extends Object> Reference<? extends _Object_> remove (final ReferenceQueue<_Object_> queue)
	{
		return (Threading.remove (queue, -1));
	}
	
	public static final <_Object_ extends Object> Reference<? extends _Object_> remove (final ReferenceQueue<_Object_> queue, final long timeout)
	{
		Preconditions.checkNotNull (queue);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout > 0)
				return (queue.remove (timeout));
			else if (timeout == 0)
				return (queue.remove (1));
			else if (timeout == -1)
				return (queue.remove ());
			else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (null);
		}
	}
	
	public static final void setDefaultContext (final ThreadingContext context)
	{
		Threading.defaultContext.set (context);
	}
	
	public static final boolean sleep (final long timeout)
	{
		try {
			Thread.sleep (timeout);
			return (true);
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
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
	
	public static final <_Object_ extends Object> boolean transfer (final TransferQueue<_Object_> queue, final _Object_ object)
	{
		return (Threading.transfer (queue, object, -1));
	}
	
	public static final <_Object_ extends Object> boolean transfer (final TransferQueue<_Object_> queue, final _Object_ object, final long timeout)
	{
		Preconditions.checkNotNull (queue);
		Preconditions.checkNotNull (object);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			if (timeout > 0)
				return (queue.tryTransfer (object, timeout, TimeUnit.MILLISECONDS));
			else if (timeout == 0)
				return (queue.tryTransfer (object));
			else if (timeout == -1) {
				queue.transfer (object);
				return (true);
			} else
				throw (new AssertionError ());
		} catch (final InterruptedException exception) {
			Threading.interruptCurrentThread ();
			return (false);
		}
	}
	
	public static final boolean wait (final Object monitor)
	{
		return (Threading.wait (monitor, -1));
	}
	
	public static final boolean wait (final Object monitor, final long timeout)
	{
		Preconditions.checkNotNull (monitor);
		Preconditions.checkArgument ((timeout >= 0) || (timeout == -1));
		try {
			synchronized (monitor) {
				if (timeout > 0)
					monitor.wait (timeout);
				else if (timeout == 0)
					monitor.wait (1);
				else if (timeout == -1)
					monitor.wait ();
				else
					throw (new AssertionError ());
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
					// intentional
				}
			}
		}
	}
	
	public static final int defaultAbortExitCode = AbortingExceptionTracer.defaultExitCode;
	public static final int defaultHaltExitCode = AbortingExceptionTracer.defaultExitCode;
	private static final AtomicReference<ThreadingContext> defaultContext = Atomics.newReference (null);
	
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
					final Thread thread = Thread.currentThread ();
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
