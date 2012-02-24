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

package eu.mosaic_cloud.tools.threading.implementations.basic.tests;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import eu.mosaic_cloud.tools.exceptions.core.CaughtException;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadConfiguration;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;


public final class BasicThreadingContextTest
{
	@Before
	public final void prepare ()
	{
		BasicThreadingSecurityManager.initialize ();
		this.exceptions = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		this.threading = BasicThreadingContext.create (this, this.exceptions.catcher);
		Assert.assertTrue (this.threading.initialize (this.waitTimeout));
	}
	
	@Test
	public final void testManagedForker ()
	{
		final int forkCount = Forker.getCount (this.forkLevel, this.forkFanout, true);
		final ThreadFactory creator = this.threading.createThreadFactory (ThreadConfiguration.create (this, "forkers", true));
		final Waiter waiter = new Waiter (forkCount, this.waitTimeout, null);
		final Forker forker = new Forker (creator, this.forkLevel, this.forkFanout, waiter);
		forker.fork ();
		forker.awaitRunning (this.waitTimeout);
		for (final Thread child : forker.queue)
			Assert.assertTrue (this.threading.isManaged (child));
		waiter.trigger (forkCount);
		Assert.assertTrue (waiter.awaitCompleted (this.waitTimeout));
		Assert.assertTrue (forker.awaitCompleted (this.waitTimeout));
		Assert.assertTrue (Threading.join (forker.queue));
	}
	
	@Test
	public final void testUnmanagedCorrectForker ()
	{
		final int forkCount = Forker.getCount (this.forkLevel, this.forkFanout, true);
		final ThreadFactory creator = new ManagedUnmanagedThreadFactory (this.threading, null, ThreadConfiguration.create (this, "forkers", true), forkCount);
		final Waiter waiter = new Waiter (forkCount, this.waitTimeout, null);
		final Forker forker = new Forker (creator, this.forkLevel, this.forkFanout, waiter);
		forker.fork ();
		forker.awaitRunning (this.waitTimeout);
		for (final Thread child : forker.queue)
			Assert.assertTrue (this.threading.isManaged (child));
		waiter.trigger (forkCount);
		Assert.assertTrue (waiter.awaitCompleted (this.waitTimeout));
		Assert.assertTrue (forker.awaitCompleted (this.waitTimeout));
		Assert.assertTrue (Threading.join (forker.queue));
	}
	
	@Test
	public final void testUnmanagedIncorrectForker ()
	{
		final int managedLevel = this.forkLevel / 2;
		Preconditions.checkArgument (managedLevel >= 2);
		final int managedCount = Forker.getCount (managedLevel, this.forkFanout, true);
		final ThreadFactory creator = new ManagedUnmanagedThreadFactory (this.threading, Threading.getRootThreadGroup (), ThreadConfiguration.create (this, "forkers", true), managedCount);
		final Waiter waiter = new Waiter (1, this.waitTimeout, null);
		final Forker forker = new Forker (creator, this.forkLevel, this.forkFanout, waiter);
		forker.fork ();
		waiter.trigger ();
		Assert.assertTrue (waiter.awaitCompleted (this.waitTimeout));
		Assert.assertTrue (this.threading.destroy (this.waitTimeout));
		while (true) {
			final CaughtException exception = this.exceptions.queue.poll ();
			if (exception == null)
				break;
			Assert.assertTrue ((exception.getCause () instanceof SecurityException) || (exception.getCause () instanceof AssertionError));
		}
	}
	
	@Test
	public final void testWaiter ()
	{
		final Waiter waiter = new Waiter (1, this.waitTimeout, null);
		final ThreadingContextAsserter asserter = new ThreadingContextAsserter (this.threading, waiter);
		final Thread thread = Threading.createAndStartDaemonThread (this.threading, this, null, asserter);
		Assert.assertTrue (this.threading.isManaged (thread));
		waiter.trigger ();
		Assert.assertTrue (waiter.awaitCompleted (this.waitTimeout));
		Assert.assertTrue (Threading.join (thread));
	}
	
	@After
	public final void unprepare ()
	{
		Assert.assertTrue (this.threading.destroy (this.waitTimeout));
		Assert.assertNull (this.exceptions.queue.poll ());
	}
	
	private QueueingExceptionTracer exceptions;
	private final int forkFanout = 2;
	private final int forkLevel = 6;
	private BasicThreadingContext threading;
	private final long waitTimeout = 1000;
	
	public static final class Forker
			extends Object
			implements
				Runnable
	{
		public Forker (final ThreadFactory creator, final int level, final int fanout, final Runnable delegate)
		{
			super ();
			Preconditions.checkNotNull (creator);
			Preconditions.checkArgument (level >= 0);
			Preconditions.checkArgument (fanout > 0);
			this.forker = this;
			this.creator = creator;
			this.level = level;
			this.fanout = fanout;
			final int children = Forker.getCount (this.level, this.fanout, true);
			this.forked = new AtomicBoolean (false);
			this.running = new CountDownLatch (children);
			this.completed = new CountDownLatch (children);
			this.delegate = delegate;
			this.queue = new ArrayBlockingQueue<Thread> (children);
		}
		
		private Forker (final Forker forker)
		{
			super ();
			this.forker = forker.forker;
			this.level = forker.level - 1;
			this.fanout = forker.fanout;
			this.creator = null;
			this.forked = null;
			this.running = null;
			this.completed = null;
			this.delegate = null;
			this.queue = null;
		}
		
		public final boolean awaitCompleted (final long timeout)
		{
			Preconditions.checkState (this.forker == this);
			return (Threading.await (this.completed, timeout));
		}
		
		public final boolean awaitRunning (final long timeout)
		{
			Preconditions.checkState (this.forker == this);
			return (Threading.await (this.running, timeout));
		}
		
		public final void fork ()
		{
			Preconditions.checkState (this.forker == this);
			Preconditions.checkState (this.forked.compareAndSet (false, true));
			this.creator.newThread (this).start ();
		}
		
		@Override
		public final void run ()
		{
			Assert.assertTrue (this.forker.queue.offer (Threading.getCurrentThread ()));
			if (this.level >= 2)
				for (int index = 0; index < this.fanout; index++) {
					final Thread child = this.forker.creator.newThread (new Forker (this));
					child.start ();
				}
			this.forker.running.countDown ();
			if (this.forker.delegate != null)
				this.forker.delegate.run ();
			this.forker.completed.countDown ();
		}
		
		public final ArrayBlockingQueue<Thread> queue;
		private final CountDownLatch completed;
		private final ThreadFactory creator;
		private final Runnable delegate;
		private final int fanout;
		private final AtomicBoolean forked;
		private final Forker forker;
		private final int level;
		private final CountDownLatch running;
		
		public static final int getCount (final int level, final int fanout, final boolean cummulative)
		{
			Preconditions.checkArgument (level >= 1);
			Preconditions.checkArgument (fanout >= 1);
			final int count;
			if (fanout == 2) {
				if (cummulative)
					count = (int) Math.pow (fanout, level) - 1;
				else
					count = (int) Math.pow (fanout, level - 1);
			} else
				throw (new UnsupportedOperationException ());
			return (count);
		}
	}
	
	public static final class ManagedUnmanagedThreadFactory
			extends Object
			implements
				ThreadFactory
	{
		public ManagedUnmanagedThreadFactory (final ThreadingContext managedContext, final ThreadGroup unmanagedGroup, final ThreadConfiguration configuration, final int initialManagedCount)
		{
			super ();
			Preconditions.checkNotNull (managedContext);
			Preconditions.checkNotNull (configuration);
			Preconditions.checkNotNull (configuration.name);
			Preconditions.checkArgument (initialManagedCount > 0);
			this.managedContext = managedContext;
			this.unmanagedGroup = unmanagedGroup;
			this.configuration = configuration;
			this.initialManagedCounter = new AtomicInteger (initialManagedCount);
		}
		
		@Override
		public final Thread newThread (final Runnable runnable)
		{
			Preconditions.checkNotNull (runnable);
			final Thread thread;
			if (this.initialManagedCounter.decrementAndGet () >= 0)
				thread = Threading.createThread (this.managedContext, this.configuration, runnable);
			else {
				thread = new Thread (this.unmanagedGroup, runnable);
				thread.setName (this.configuration.name);
				thread.setDaemon (this.configuration.daemon);
				if (this.configuration.priority != -1)
					thread.setPriority (this.configuration.priority);
				if (this.configuration.classLoader != null)
					thread.setContextClassLoader (this.configuration.classLoader);
				thread.setName (BasicThreadingContext.buildThreadName (thread.getThreadGroup (), this.configuration, -1));
			}
			return (thread);
		}
		
		private final ThreadConfiguration configuration;
		private final AtomicInteger initialManagedCounter;
		private final ThreadingContext managedContext;
		private final ThreadGroup unmanagedGroup;
	}
	
	public static final class Sleeper
			extends Object
			implements
				Runnable
	{
		public Sleeper (final long timeout, final Runnable delegate)
		{
			super ();
			Preconditions.checkArgument (timeout >= 0);
			this.timeout = timeout;
			this.delegate = delegate;
		}
		
		@Override
		public final void run ()
		{
			Assert.assertTrue (Threading.sleep (this.timeout));
			if (this.delegate != null)
				this.delegate.run ();
		}
		
		private final Runnable delegate;
		private final long timeout;
	}
	
	public static final class ThreadingContextAsserter
			extends Object
			implements
				Runnable
	{
		public ThreadingContextAsserter (final ThreadingContext context, final Runnable delegate)
		{
			super ();
			this.context = context;
			this.delegate = delegate;
		}
		
		@Override
		public final void run ()
		{
			Assert.assertSame (this.context, Threading.getCurrentContext ());
			if (this.delegate != null)
				this.delegate.run ();
		}
		
		private final ThreadingContext context;
		private final Runnable delegate;
	}
	
	public static final class Waiter
			extends Object
			implements
				Runnable
	{
		public Waiter (final int count, final long timeout, final Runnable delegate)
		{
			super ();
			Preconditions.checkArgument (count >= 0);
			Preconditions.checkArgument (timeout >= 0);
			this.triggered = new CountDownLatch (count);
			this.completed = new CountDownLatch (count);
			this.delegate = delegate;
			this.timeout = timeout;
		}
		
		public final boolean awaitCompleted (final long timeout)
		{
			return (Threading.await (this.completed, timeout));
		}
		
		public final boolean awaitTriggered (final long timeout)
		{
			return (Threading.await (this.triggered, timeout));
		}
		
		@Override
		public final void run ()
		{
			Assert.assertTrue (Threading.await (this.triggered, this.timeout));
			if (this.delegate != null)
				this.delegate.run ();
			this.completed.countDown ();
		}
		
		public final void trigger ()
		{
			this.triggered.countDown ();
		}
		
		public final void trigger (final int count)
		{
			Preconditions.checkArgument (count > 0);
			for (int index = 0; index < count; index++)
				this.triggered.countDown ();
		}
		
		private final CountDownLatch completed;
		private final Runnable delegate;
		private final long timeout;
		private final CountDownLatch triggered;
	}
}
