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

package eu.mosaic_cloud.tools.threading.implementations.basic;


import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadController;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.ThreadBundle;
import eu.mosaic_cloud.tools.threading.tools.Threading;


public final class BasicThreadingContext
		implements
			ThreadingContext,
			ThreadController,
			Iterable<Thread>
{
	private BasicThreadingContext (final ThreadGroup group, final ThreadConfiguration configuration)
	{
		super ();
		Preconditions.checkNotNull (group);
		Preconditions.checkNotNull (configuration);
		Preconditions.checkState (System.getSecurityManager () instanceof BasicThreadingSecurityManager);
		this.owner = BasicThreadingContext.buildOwner (configuration);
		this.configuration = configuration;
		this.group = new BasicThreadGroup (group, this.configuration);
		this.defaultGroup = new BasicThreadGroup (this.group, configuration.setName ("default"));
		this.threads = ThreadBundle.create ();
		this.initialized = new AtomicBoolean (false);
		this.sealed = new AtomicBoolean (false);
	}
	
	// !!!!
	/*
	 * Returned executors should extend `ThreadPoolExecutor` (or the like) and override certain methods for logging
	 * and error handling.
	 */
	@Override
	public final boolean await ()
	{
		return (this.threads.await ());
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		return (this.threads.await (timeout));
	}
	
	@Override
	public final ExecutorService createCachedThreadPool (final ThreadConfiguration configuration)
	{
		return (Executors.unconfigurableExecutorService (Executors.newCachedThreadPool (this.createThreadFactory (configuration, true))));
	}
	
	@Override
	public final ExecutorService createFixedThreadPool (final ThreadConfiguration configuration, final int threads)
	{
		return (Executors.unconfigurableExecutorService (Executors.newFixedThreadPool (threads, this.createThreadFactory (configuration, true))));
	}
	
	@Override
	public final ScheduledExecutorService createScheduledThreadPool (final ThreadConfiguration configuration, final int coreThreads)
	{
		return (Executors.unconfigurableScheduledExecutorService (Executors.newScheduledThreadPool (coreThreads, this.createThreadFactory (configuration, true))));
	}
	
	@Override
	public final ExecutorService createSingleThreadExecutor (final ThreadConfiguration configuration)
	{
		return (Executors.unconfigurableExecutorService (Executors.newSingleThreadExecutor (this.createThreadFactory (configuration, false))));
	}
	
	@Override
	public final ScheduledExecutorService createSingleThreadScheduledExecutor (final ThreadConfiguration configuration)
	{
		return (Executors.unconfigurableScheduledExecutorService (Executors.newSingleThreadScheduledExecutor (this.createThreadFactory (configuration, false))));
	}
	
	@Override
	public final Thread createThread (final ThreadConfiguration configuration, final Runnable runnable)
	{
		Preconditions.checkState (this.isActive ());
		Preconditions.checkState (!this.isSealed ());
		return (new BasicThread (this.group, configuration, runnable, -1));
	}
	
	@Override
	public final ThreadFactory createThreadFactory (final ThreadConfiguration configuration)
	{
		return (this.createThreadFactory (configuration, true));
	}
	
	public final ThreadFactory createThreadFactory (final ThreadConfiguration configuration, final boolean index)
	{
		Preconditions.checkState (this.isActive ());
		Preconditions.checkState (!this.isSealed ());
		return (new BasicThreadFactory (new BasicThreadGroup (this.group, configuration), configuration, index));
	}
	
	public final void destroy ()
	{
		Preconditions.checkState (this.destroy (-1));
	}
	
	public final boolean destroy (final long timeout)
	{
		this.interrupt ();
		return (this.await (timeout));
	}
	
	@Override
	public final ThreadGroup getDefaultThreadGroup ()
	{
		return (this.defaultGroup);
	}
	
	public final void initialize ()
	{
		Preconditions.checkState (this.initialize (-1));
	}
	
	public final boolean initialize (final long timeout)
	{
		Preconditions.checkArgument (timeout >= -1);
		Preconditions.checkState (this.initialized.compareAndSet (false, true));
		return (true);
	}
	
	@Override
	public final void interrupt ()
	{
		this.threads.interrupt ();
	}
	
	@Override
	public final boolean isActive ()
	{
		if (!this.initialized.get ())
			return (false);
		final Object owner = this.configuration.owner.get ();
		if (owner == null)
			return (false);
		return (true);
	}
	
	@Override
	public final boolean isManaged (final Thread thread)
	{
		Preconditions.checkNotNull (thread);
		return (this.isManaged (thread.getThreadGroup ()));
	}
	
	@Override
	public final boolean isManaged (final ThreadGroup group)
	{
		Preconditions.checkNotNull (group);
		for (ThreadGroup parent = group; true; parent = parent.getParent ()) {
			if (parent == this.group)
				return (true);
			if (parent == null)
				return (false);
		}
	}
	
	@Override
	public final boolean isSealed ()
	{
		return (this.sealed.get ());
	}
	
	@Override
	public final Iterator<Thread> iterator ()
	{
		return (this.threads.iterator ());
	}
	
	@Override
	public final void registerThread (final Thread thread)
	{
		Preconditions.checkArgument (this.isManaged (thread));
		this.threads.register (thread);
	}
	
	public final void seal ()
	{
		this.sealed.set (true);
	}
	
	final void handleException (final Thread thread, final Throwable exception)
	{
		final UncaughtExceptionHandler catcher = this.resolveCatcher (thread);
		if (catcher != null)
			this.handleException (catcher, thread, exception);
	}
	
	final UncaughtExceptionHandler resolveCatcher (final Thread thread)
	{
		Preconditions.checkNotNull (thread);
		Preconditions.checkArgument (this.isManaged (thread));
		final UncaughtExceptionHandler threadCatcher;
		{
			if (thread instanceof BasicThread)
				threadCatcher = ((BasicThread) thread).configuration.catcher;
			else
				threadCatcher = null;
		}
		if (threadCatcher != null)
			return (threadCatcher);
		final UncaughtExceptionHandler groupCatcher;
		{
			for (ThreadGroup group = thread.getThreadGroup (); true; group = group.getParent ()) {
				if (group == null) {
					groupCatcher = null;
					break;
				}
				if (group instanceof BasicThreadGroup) {
					final UncaughtExceptionHandler groupCatcher2 = ((BasicThreadGroup) group).configuration.catcher;
					if (groupCatcher2 != null) {
						groupCatcher = groupCatcher2;
						break;
					}
				}
			}
		}
		if (groupCatcher != null)
			return (groupCatcher);
		if (this.configuration.catcher != null)
			return (this.configuration.catcher);
		return (null);
	}
	
	private final void handleException (final UncaughtExceptionHandler catcher, final Thread thread, final Throwable exception)
	{
		catcher.uncaughtException (thread, exception);
	}
	
	private final ThreadConfiguration configuration;
	private final BasicThreadGroup defaultGroup;
	private final BasicThreadGroup group;
	private final AtomicBoolean initialized;
	private final WeakReference<Object> owner;
	private final AtomicBoolean sealed;
	private final ThreadBundle<Thread> threads;
	
	public static final WeakReference<Object> buildOwner (final ThreadConfiguration configuration)
	{
		Preconditions.checkNotNull (configuration.owner);
		final Object owner = configuration.owner.get ();
		Preconditions.checkNotNull (owner);
		return (new WeakReference<Object> (owner));
	}
	
	public static final String buildThreadGroupName (final ThreadConfiguration configuration)
	{
		Preconditions.checkNotNull (configuration);
		final Object owner = configuration.owner.get ();
		Preconditions.checkNotNull (owner);
		Preconditions.checkArgument ((configuration.name == null) || ThreadConfiguration.namePattern.matcher (configuration.name).matches ());
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
	
	public static final String buildThreadName (final ThreadGroup group, final ThreadConfiguration configuration, final int index)
	{
		Preconditions.checkNotNull (configuration);
		final Object owner = configuration.owner.get ();
		Preconditions.checkNotNull (owner);
		Preconditions.checkArgument ((index == -1) || (index >= 1));
		Preconditions.checkArgument ((configuration.name == null) || ThreadConfiguration.namePattern.matcher (configuration.name).matches ());
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
	
	public static final BasicThreadingContext create (final Object owner, final Thread.UncaughtExceptionHandler catcher)
	{
		return (new BasicThreadingContext (Threading.getRootThreadGroup (), ThreadConfiguration.create (owner, null, true, catcher)));
	}
	
	public final class BasicThread
			extends Thread
			implements
				ManagedThread
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
			BasicThreadingContext.this.registerThread (this);
		}
		
		@Override
		public final boolean await ()
		{
			return (Threading.join (this));
		}
		
		@Override
		public final boolean await (final long timeout)
		{
			return (Threading.join (this, timeout));
		}
		
		@Override
		public final BasicThreadingContext getContext ()
		{
			return (BasicThreadingContext.this);
		}
		
		@Override
		public final UncaughtExceptionHandler getUncaughtExceptionHandler ()
		{
			return (super.getUncaughtExceptionHandler ());
		}
		
		@Override
		public final void run ()
		{
			Preconditions.checkState (this == Thread.currentThread ());
			Preconditions.checkState (this.running.compareAndSet (false, true));
			Preconditions.checkState (Threading.getCurrentContext () == BasicThreadingContext.this);
			super.run ();
		}
		
		private final ThreadConfiguration configuration;
		private final BasicThreadGroup group;
		private final int index;
		private final AtomicBoolean running;
	}
	
	public final class BasicThreadFactory
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
			this.index = index ? new AtomicInteger (0) : null;
		}
		
		public final BasicThreadingContext getContext ()
		{
			return (BasicThreadingContext.this);
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
	
	public final class BasicThreadGroup
			extends ThreadGroup
			implements
				ManagedThreadGroup
	{
		BasicThreadGroup (final BasicThreadGroup group, final ThreadConfiguration configuration)
		{
			this ((ThreadGroup) group, configuration);
			Preconditions.checkNotNull (group);
		}
		
		BasicThreadGroup (final ThreadGroup group, final ThreadConfiguration configuration)
		{
			super (group, BasicThreadingContext.buildThreadGroupName (configuration));
			Preconditions.checkNotNull (group);
			this.configuration = configuration;
			// !!!!
			// super.setDaemon (this.configuration.daemon);
			super.setDaemon (false);
			if (configuration.priority != -1)
				this.setMaxPriority (configuration.priority);
			else
				this.setMaxPriority (group.getMaxPriority ());
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
		
		public final ThreadConfiguration getConfiguration ()
		{
			return (this.configuration);
		}
		
		@Override
		public final BasicThreadingContext getContext ()
		{
			return (BasicThreadingContext.this);
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
			Preconditions.checkArgument (thread.getThreadGroup () == this);
			BasicThreadingContext.this.handleException (thread, exception);
		}
		
		private final ThreadConfiguration configuration;
	}
}
