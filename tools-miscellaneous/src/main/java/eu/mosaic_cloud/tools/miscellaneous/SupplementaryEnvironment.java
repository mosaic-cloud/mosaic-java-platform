/*
 * #%L
 * mosaic-tools-miscellaneous
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
// $codepro.audit.disable emptyCatchClause
// $codepro.audit.disable logExceptions

package eu.mosaic_cloud.tools.miscellaneous;


import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;


public final class SupplementaryEnvironment
			extends Object
			implements
				Map<String, Object>
{
	private SupplementaryEnvironment (final Map<String, Object> delegate, final UncaughtExceptionHandler catcher) {
		super ();
		Preconditions.checkNotNull (delegate);
		Preconditions.checkNotNull (catcher);
		this.delegate = delegate;
		this.catcher = catcher;
		this.cache = new ConcurrentHashMap<String, Object> ();
	}
	
	@Override
	public final void clear () {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final boolean containsKey (final Object key) {
		return (this.get (key) != null);
	}
	
	@Override
	public final boolean containsValue (final Object value) {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final Set<java.util.Map.Entry<String, Object>> entrySet () {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final Object get (final Object key) {
		final Object cached = this.cache.get (key);
		if (cached != null)
			return ((cached != SupplementaryEnvironment.cacheNullValue) ? cached : null);
		final Object value;
		try {
			value = this.delegate.get (key);
		} catch (final Throwable exception) {
			this.handleException (exception);
			this.cache.putIfAbsent ((String) key, SupplementaryEnvironment.cacheNullValue);
			return (null);
		}
		this.cache.putIfAbsent ((String) key, (value != null) ? value : SupplementaryEnvironment.cacheNullValue);
		return (this.get (key));
	}
	
	public final <Value> Value get (final String key, final Class<Value> valueClass, final Value valueDefault) {
		final Object value = this.get (key);
		if (value == null)
			return (valueDefault);
		try {
			return (valueClass.cast (value));
		} catch (final ClassCastException exception) {
			this.handleException (exception);
			return (valueDefault);
		}
	}
	
	@Override
	public final boolean isEmpty () {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final Set<String> keySet () {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final Object put (final String key, final Object value) {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final void putAll (final Map<? extends String, ? extends Object> map) {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final Object remove (final Object key) {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final int size () {
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final Collection<Object> values () {
		throw (new UnsupportedOperationException ());
	}
	
	private final void handleException (final Throwable exception) {
		try {
			this.catcher.uncaughtException (Thread.currentThread (), exception);
		} catch (final Throwable exception1) {
			// NOTE: intentional
		}
	}
	
	private final ConcurrentHashMap<String, Object> cache;
	private final UncaughtExceptionHandler catcher;
	private final Map<String, Object> delegate;
	
	public static final SupplementaryEnvironment create (final Map<String, Object> delegate, final UncaughtExceptionHandler catcher) {
		return (new SupplementaryEnvironment (delegate, catcher));
	}
	
	private static final Object cacheNullValue = new Object ();
}
