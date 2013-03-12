/*
 * #%L
 * mosaic-components-launcher
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

package eu.mosaic_cloud.components.implementations.basic;


import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.AbortingExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingSecurityManager;

import com.google.common.base.Preconditions;


public final class BasicComponentLauncherMain
{
	private BasicComponentLauncherMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String componentCallbacks, final String componentConfiguration, final String[] arguments)
			throws Throwable
	{
		BasicComponentLauncherMain.main (componentCallbacks, componentConfiguration, arguments, 0);
	}
	
	public static final void main (final String componentCallbacks, final String componentConfiguration, final String[] arguments, final int argumentsOffset)
			throws Throwable
	{
		Preconditions.checkNotNull (componentCallbacks);
		Preconditions.checkNotNull (componentConfiguration);
		Preconditions.checkNotNull (arguments);
		Preconditions.checkArgument ((argumentsOffset >= 0) && (argumentsOffset <= arguments.length));
		final String[] finalArguments = new String[(arguments.length + 2) - argumentsOffset];
		finalArguments[0] = componentCallbacks;
		finalArguments[1] = componentConfiguration;
		System.arraycopy (arguments, argumentsOffset, finalArguments, 2, arguments.length - argumentsOffset);
		BasicComponentLauncherMain.main (finalArguments);
	}
	
	public static final void main (final String componentCallbacks, final String[] arguments)
			throws Throwable
	{
		BasicComponentLauncherMain.main (componentCallbacks, arguments, 0);
	}
	
	public static final void main (final String componentCallbacks, final String[] arguments, final int argumentsOffset)
			throws Throwable
	{
		BasicComponentLauncherMain.main (componentCallbacks, "null", arguments, argumentsOffset);
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		Preconditions.checkNotNull (arguments);
		BasicThreadingSecurityManager.initialize ();
		final BaseExceptionTracer exceptions = AbortingExceptionTracer.defaultInstance;
		final BasicThreadingContext threading = BasicThreadingContext.create (BasicComponentHarnessMain.class, exceptions, exceptions.catcher);
		threading.initialize ();
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader ();
		BasicComponentLauncherMain.main (arguments, classLoader, threading, exceptions);
		threading.destroy ();
	}
	
	public static final void main (final String[] arguments, final ClassLoader classLoader, final ThreadingContext threading, final ExceptionTracer exceptions)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length >= 3), "invalid arguments; expected `<component-callbacks> <component-configuration> <mode> ...`");
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		final String mode = arguments[2];
		final String[] finalArguments = new String[arguments.length - 1];
		System.arraycopy (arguments, 0, finalArguments, 0, 2);
		System.arraycopy (arguments, 3, finalArguments, 2, arguments.length - 3);
		if ("local".equals (mode))
			BasicComponentLocalLauncher.launch (finalArguments, classLoader, threading, exceptions);
		else if ("remote".equals (mode))
			BasicComponentRemoteLauncher.launch (finalArguments, classLoader, threading, exceptions);
		else
			throw (new IllegalArgumentException (String.format ("invalid mode `%s`", mode)));
	}
}
