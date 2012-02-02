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


import java.util.Map;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


public interface ComponentCallbacks
		extends
			Callbacks
{
	public abstract CallbackReference called (final ComponentController component, final ComponentCallRequest request);
	
	public abstract CallbackReference callReturned (final ComponentController component, final ComponentCallReply reply);
	
	public abstract CallbackReference casted (final ComponentController component, final ComponentCastRequest request);
	
	public abstract CallbackReference failed (final ComponentController component, final Throwable exception);
	
	public abstract CallbackReference initialized (final ComponentController component);
	
	public abstract CallbackReference registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok);
	
	public abstract CallbackReference terminated (final ComponentController component);
	
	public static final class Context
			extends Object
	{
		private Context (final ComponentController component, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final Map<String, Object> environment)
		{
			super ();
			Preconditions.checkNotNull (component);
			Preconditions.checkNotNull (classLoader);
			Preconditions.checkNotNull (reactor);
			Preconditions.checkNotNull (threading);
			Preconditions.checkNotNull (exceptions);
			Preconditions.checkNotNull (environment);
			this.component = component;
			this.classLoader = classLoader;
			this.reactor = reactor;
			this.threading = threading;
			this.exceptions = exceptions;
			this.environment = environment;
		}
		
		public final ClassLoader classLoader;
		public final ComponentController component;
		public final Map<String, Object> environment;
		public final ExceptionTracer exceptions;
		public final CallbackReactor reactor;
		public final ThreadingContext threading;
		
		public static final Context create (final ComponentController component, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final Map<String, Object> environment)
		{
			return (new Context (component, classLoader, reactor, threading, exceptions, environment));
		}
	}
	
	public static interface Provider
	{
		public abstract ComponentCallbacks provide (final Context context);
	}
}
