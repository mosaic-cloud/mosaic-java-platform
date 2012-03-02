/*
 * #%L
 * mosaic-components-core
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

package eu.mosaic_cloud.components.core;


import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.SupplementaryEnvironment;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;


public final class ComponentEnvironment
		extends Object
{
	private ComponentEnvironment (final ComponentController component, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final Map<String, Object> supplementary)
	{
		super ();
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (reactor);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		Preconditions.checkNotNull (supplementary);
		this.component = component;
		this.classLoader = classLoader;
		this.reactor = reactor;
		this.threading = threading;
		this.exceptions = exceptions;
		this.supplementary = SupplementaryEnvironment.create (supplementary, new UncaughtExceptionHandler () {
			@Override
			public void uncaughtException (final Thread thread, final Throwable exception)
			{
				exceptions.trace (ExceptionResolution.Ignored, exception);
			}
		});
	}
	
	public static final ComponentEnvironment create (final ComponentController component, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new ComponentEnvironment (component, classLoader, reactor, threading, exceptions, new HashMap<String, Object> ()));
	}
	
	public static final ComponentEnvironment create (final ComponentController component, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final Map<String, Object> environment)
	{
		return (new ComponentEnvironment (component, classLoader, reactor, threading, exceptions, environment));
	}
	
	public final ClassLoader classLoader;
	public final ComponentController component;
	public final ExceptionTracer exceptions;
	public final CallbackReactor reactor;
	public final SupplementaryEnvironment supplementary;
	public final ThreadingContext threading;
}
