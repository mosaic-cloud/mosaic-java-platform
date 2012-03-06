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

package eu.mosaic_cloud.tools.threading.core;


import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.regex.Pattern;

import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;

import com.google.common.base.Preconditions;


public final class ThreadConfiguration
		extends Object
{
	private ThreadConfiguration (final Reference<Object> owner, final String name, final boolean daemon, final int priority, final ExceptionTracer exceptions, final Thread.UncaughtExceptionHandler catcher, final ClassLoader classLoader)
	{
		super ();
		Preconditions.checkNotNull (owner);
		Preconditions.checkArgument ((name == null) || ThreadConfiguration.namePattern.matcher (name).matches ());
		Preconditions.checkArgument ((priority == -1) || ((priority >= Thread.MIN_PRIORITY) && (priority <= Thread.MAX_PRIORITY)));
		this.owner = owner;
		this.name = name;
		this.daemon = daemon;
		this.priority = priority;
		this.exceptions = exceptions;
		this.catcher = catcher;
		this.classLoader = classLoader;
	}
	
	public final ThreadConfiguration setClassLoader (final ClassLoader classLoader)
	{
		return (new ThreadConfiguration (this.owner, this.name, this.daemon, this.priority, this.exceptions, this.catcher, classLoader));
	}
	
	public final ThreadConfiguration setName (final String name)
	{
		return (new ThreadConfiguration (this.owner, name, this.daemon, this.priority, this.exceptions, this.catcher, this.classLoader));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon)
	{
		return (ThreadConfiguration.create (owner, name, daemon, -1, null, null));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon, final ExceptionTracer exceptions, final Thread.UncaughtExceptionHandler catcher)
	{
		return (ThreadConfiguration.create (owner, name, daemon, -1, exceptions, catcher));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon, final int priority)
	{
		return (ThreadConfiguration.create (owner, name, daemon, priority, null, null));
	}
	
	public static final ThreadConfiguration create (final Object owner, final String name, final boolean daemon, final int priority, final ExceptionTracer exceptions, final Thread.UncaughtExceptionHandler catcher)
	{
		return (new ThreadConfiguration (new WeakReference<Object> (owner), name, daemon, priority, exceptions, catcher, null));
	}
	
	public final Thread.UncaughtExceptionHandler catcher;
	public final ClassLoader classLoader;
	public final boolean daemon;
	public final ExceptionTracer exceptions;
	public final String name;
	public final Reference<Object> owner;
	public final int priority;
	public static final Pattern namePattern = Pattern.compile ("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");
}
