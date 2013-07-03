/*
 * #%L
 * mosaic-drivers-stubs-kv-common
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

package eu.mosaic_cloud.drivers.kvstore.riak;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import eu.mosaic_cloud.drivers.DriverNotFoundException;
import eu.mosaic_cloud.platform.v1.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


/**
 * A factory for key-value drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class KeyValueDriverFactory
{
	private KeyValueDriverFactory ()
	{}
	
	/**
	 * Creates a driver of requested type with the specified configuration.
	 * 
	 * @param driverName
	 *            the name of the driver
	 * @param config
	 *            the configuration for the driver
	 * @param threadingContext
	 *            the context used for creating threads
	 * @return the driver
	 * @throws ConnectorNotFoundException
	 *             if driver cannot be instantiated for any reason
	 */
	public static AbstractKeyValueDriver createDriver (final String driverName, final IConfiguration config, final ThreadingContext threadingContext)
			throws DriverNotFoundException
	{
		DriverType type = null;
		AbstractKeyValueDriver driver = null;
		for (final DriverType t : DriverType.values ()) {
			if (t.name ().equalsIgnoreCase (driverName)) {
				type = t;
				break;
			}
		}
		if (type != null) {
			try {
				final Class<?> driverClass = type.getDriverClass ();
				final Method createMethod = driverClass.getMethod ("create", IConfiguration.class, ThreadingContext.class);
				try {
					driver = (AbstractKeyValueDriver) createMethod.invoke (null, config, threadingContext);
				} catch (final InvocationTargetException wrapper) {
					FallbackExceptionTracer.defaultInstance.traceHandledException (wrapper);
					throw wrapper.getCause ();
				}
			} catch (final Throwable e) {
				FallbackExceptionTracer.defaultInstance.traceIgnoredException (e);
				final DriverNotFoundException exception = new DriverNotFoundException (e);
				throw exception;
			}
		}
		return driver;
	}
	
	public enum DriverType
	{
		RIAKPB ("eu.mosaic_cloud.drivers.kvstore.riak.RiakDriver"),
		RIAKREST ("eu.mosaic_cloud.drivers.kvstore.riak.RiakDriver");
		DriverType (final String canonicalClassName)
		{
			this.driverClassName = canonicalClassName;
		}
		
		public Class<? extends AbstractKeyValueDriver> getDriverClass ()
		{
			return this.getDriverClass (DriverType.class.getClassLoader ());
		}
		
		private Class<? extends AbstractKeyValueDriver> getDriverClass (final ClassLoader loader)
		{
			try {
				return (Class<? extends AbstractKeyValueDriver>) loader.loadClass (this.driverClassName);
			} catch (final ClassNotFoundException e) {
				throw new Error (e);
			}
		}
		
		private final String driverClassName;
	}
}
