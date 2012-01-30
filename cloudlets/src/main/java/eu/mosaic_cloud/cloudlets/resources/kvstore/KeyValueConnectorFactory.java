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

import eu.mosaic_cloud.cloudlets.resources.ConnectorNotFoundException;
import eu.mosaic_cloud.connectors.kvstore.IKeyValueStore;
import eu.mosaic_cloud.connectors.kvstore.KeyValueStoreConnector;
import eu.mosaic_cloud.connectors.kvstore.memcached.MemcachedStoreConnector;
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
		KVSTORE(KeyValueStoreConnector.class), MEMCACHED(
				MemcachedStoreConnector.class);

		private final Class<? extends IKeyValueStore> connectorClass;

		ConnectorType(Class<? extends IKeyValueStore> canonicalClassName) {
			this.connectorClass = canonicalClassName;
		}

		public Class<? extends IKeyValueStore> getConnectorClass() {
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
	 * @throws ConnectorNotFoundException
	 *             if driver cannot be instantiated for any reason
	 */
	public static IKeyValueStore createConnector(String connectorName,
			IConfiguration config, DataEncoder<?> encoder,
			ThreadingContext threadingContext)
			throws ConnectorNotFoundException {
		ConnectorType type = null;
		IKeyValueStore connector = null;

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
				connector = (IKeyValueStore) createMethod.invoke(null, config,
						encoder, threadingContext);
			} catch (Exception e) {
				ExceptionTracer.traceIgnored(e);
				ConnectorNotFoundException ex = new ConnectorNotFoundException(
						e);
				throw ex;
			}
		}
		return connector;
	}
}
