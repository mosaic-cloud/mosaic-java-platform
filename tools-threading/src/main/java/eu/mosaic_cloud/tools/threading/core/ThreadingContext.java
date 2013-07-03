/*
 * #%L
 * mosaic-tools-threading
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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
	
	public abstract ThreadConfiguration getThreadConfiguration ();
	
	public abstract boolean isActive ();
	
	public abstract boolean isManaged (final Thread thread);
	
	public abstract boolean isManaged (final ThreadGroup group);
	
	public abstract boolean isSealed ();
	
	public abstract void registerThread (final Thread thread);
	
	public interface ManagedThread
				extends
					Joinable
	{
		public abstract ThreadingContext getContext ();
	}
	
	public interface ManagedThreadGroup
	{
		public abstract ThreadingContext getContext ();
	}
}
