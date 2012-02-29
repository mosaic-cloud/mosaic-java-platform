/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.runtime;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;


public final class CloudletEnvironment
{
	private CloudletEnvironment (final IConfiguration configuration, final Class<?> cloudletCallbackClass, final Class<?> cloudletContextClass, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final Map<String, Object> supplementary)
	{
		super ();
		Preconditions.checkNotNull (configuration);
		Preconditions.checkNotNull (cloudletCallbackClass);
		Preconditions.checkNotNull (cloudletContextClass);
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (reactor);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		Preconditions.checkNotNull (supplementary);
		this.configuration = configuration;
		this.cloudletCallbackClass = cloudletCallbackClass;
		this.cloudletContextClass = cloudletContextClass;
		this.classLoader = classLoader;
		this.reactor = reactor;
		this.threading = threading;
		this.exceptions = exceptions;
		this.supplementary = new Supplementary (supplementary);
	}
	
	public final ClassLoader classLoader;
	public final Class<?> cloudletCallbackClass;
	public final Class<?> cloudletContextClass;
	public final IConfiguration configuration;
	public final ExceptionTracer exceptions;
	public final CallbackReactor reactor;
	public final Supplementary supplementary;
	public final ThreadingContext threading;
	
	public static final CloudletEnvironment create (final IConfiguration configuration, final Class<?> cloudletCallbackClass, final Class<?> cloudletContextClass, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions)
	{
		return (new CloudletEnvironment (configuration, cloudletCallbackClass, cloudletContextClass, classLoader, reactor, threading, exceptions, new HashMap<String, Object> ()));
	}
	
	public static final CloudletEnvironment create (final IConfiguration configuration, final Class<?> cloudletCallbackClass, final Class<?> cloudletContextClass, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final Map<String, Object> supplementary)
	{
		return (new CloudletEnvironment (configuration, cloudletCallbackClass, cloudletContextClass, classLoader, reactor, threading, exceptions, supplementary));
	}
	
	public final class Supplementary
			extends Object
			implements
				Map<String, Object>
	{
		private Supplementary (final Map<String, Object> delegate)
		{
			super ();
			this.delegate = delegate;
			this.cache = new ConcurrentHashMap<String, Object> ();
		}
		
		@Override
		public final void clear ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final boolean containsKey (final Object key)
		{
			return (this.get (key) != null);
		}
		
		@Override
		public final boolean containsValue (final Object value)
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final Set<java.util.Map.Entry<String, Object>> entrySet ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final Object get (final Object key)
		{
			final Object cached = this.cache.get (key);
			if (cached != null)
				return (cached);
			final Object value;
			try {
				value = this.delegate.get (key);
			} catch (final Throwable exception) {
				CloudletEnvironment.this.exceptions.trace (ExceptionResolution.Ignored, exception);
				return (null);
			}
			this.cache.putIfAbsent ((String) key, value);
			return (this.cache.get (key));
		}
		
		public final <Value> Value get (final String key, final Class<Value> valueClass, final Value valueDefault)
		{
			final Object value = this.get (key);
			if (value == null)
				return (valueDefault);
			try {
				return (valueClass.cast (value));
			} catch (final ClassCastException exception) {
				CloudletEnvironment.this.exceptions.trace (ExceptionResolution.Ignored, exception);
				return (valueDefault);
			}
		}
		
		@Override
		public final boolean isEmpty ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final Set<String> keySet ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final Object put (final String key, final Object value)
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final void putAll (final Map<? extends String, ? extends Object> map)
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final Object remove (final Object key)
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final int size ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		@Override
		public final Collection<Object> values ()
		{
			throw (new UnsupportedOperationException ());
		}
		
		private final ConcurrentHashMap<String, Object> cache;
		private final Map<String, Object> delegate;
	}
}
