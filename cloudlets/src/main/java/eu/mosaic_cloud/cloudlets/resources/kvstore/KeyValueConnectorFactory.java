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
package eu.mosaic_cloud.cloudlets.resources.kvstore;

import java.lang.reflect.Method;

import eu.mosaic_cloud.cloudlets.resources.ResourceNotFoundException;
import eu.mosaic_cloud.connectors.kvstore.IKvStoreConnector;
import eu.mosaic_cloud.connectors.kvstore.generic.GenericKvStoreConnector;
import eu.mosaic_cloud.connectors.kvstore.memcache.MemcacheKvStoreConnector;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * A factory for key-value connectors.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueConnectorFactory {

	public enum ConnectorType {
		KVSTORE(GenericKvStoreConnector.class), MEMCACHED(
				MemcacheKvStoreConnector.class);

		private final Class<? extends IKvStoreConnector> connectorClass;

		ConnectorType(Class<? extends IKvStoreConnector> canonicalClassName) {
			this.connectorClass = canonicalClassName;
		}

		public Class<? extends IKvStoreConnector> getConnectorClass() {
			return this.connectorClass;
		}
	}

	/**
	 * Creates a connector of requested type with the specified configuration.
	 * 
	 * @param connectorName
	 *            the name of the connector
	 * @param config
	 *            the configuration for the connector
	 * @param encoder
	 *            encoder used for serializing data
	 * @param threadingContext
	 *            the context used for creating threads
	 * @return the connector
	 * @throws ResourceNotFoundException
	 *             if driver cannot be instantiated for any reason
	 */
	public static IKvStoreConnector createConnector(String connectorName,
			IConfiguration config, DataEncoder<?> encoder,
			ThreadingContext threadingContext)
			throws ResourceNotFoundException {
		ConnectorType type = null;
		IKvStoreConnector connector = null;

		for (ConnectorType t : ConnectorType.values()) {
			if (t.name().equalsIgnoreCase(connectorName)) {
				type = t;
				break;
			}
		}
		if (type != null) {
			try {
				Class<?> connectorClass = type.getConnectorClass();
				Method createMethod = connectorClass.getMethod("create",
						IConfiguration.class, DataEncoder.class,
						ThreadingContext.class);
				connector = (IKvStoreConnector) createMethod.invoke(null, config,
						encoder, threadingContext);
			} catch (Exception e) {
				ExceptionTracer.traceIgnored(e);
				ResourceNotFoundException ex = new ResourceNotFoundException(
						e);
				throw ex;
			}
		}
		return connector;
	}
}
