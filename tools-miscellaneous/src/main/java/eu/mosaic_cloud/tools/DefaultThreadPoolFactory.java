/*
 * #%L
 * mosaic-tools-miscellaneous
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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

package eu.mosaic_cloud.tools;


import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


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
		return (new DefaultThreadPoolFactory (owner, true, Thread.NORM_PRIORITY, null));
	}
	
	public static final DefaultThreadPoolFactory create (final Object owner, final boolean daemon, final int priority, final ExceptionTracer exceptionTracer)
	{
		return (new DefaultThreadPoolFactory (owner, daemon, priority, eu.mosaic_cloud.exceptions.tools.UncaughtExceptionHandler.create (exceptionTracer)));
	}
	
	public static final DefaultThreadPoolFactory create (final Object owner, final boolean daemon, final int priority, final UncaughtExceptionHandler exceptionHandler)
	{
		return (new DefaultThreadPoolFactory (owner, daemon, priority, exceptionHandler));
	}
	
	public static final DefaultThreadPoolFactory create (final Object owner, final ExceptionTracer exceptionTracer)
	{
		return (new DefaultThreadPoolFactory (owner, true, Thread.NORM_PRIORITY, eu.mosaic_cloud.exceptions.tools.UncaughtExceptionHandler.create (exceptionTracer)));
	}
	
	public static final DefaultThreadPoolFactory create (final Object owner, final UncaughtExceptionHandler exceptionHandler)
	{
		return (new DefaultThreadPoolFactory (owner, true, Thread.NORM_PRIORITY, exceptionHandler));
	}
}
