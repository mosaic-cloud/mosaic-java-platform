package mosaic.cloudlet.resources.kvstore;

import java.lang.reflect.Method;

import mosaic.cloudlet.resources.ConnectorNotFoundException;
import mosaic.connector.kvstore.IKeyValueStore;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.connector.kvstore.memcached.MemcachedStoreConnector;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;

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

		public Class<? extends IKeyValueStore> getDriverClass() {
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
	 * @return the connector
	 * @throws ConnectorNotFoundException
	 *             if driver cannot be instantiated for any reason
	 */
	public static IKeyValueStore createConnector(String connectorName,
			IConfiguration config) throws ConnectorNotFoundException {
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
				Class<?> connectorClass = type.getDriverClass();
				Method createMethod = connectorClass.getMethod("create",
						IConfiguration.class);
				connector = (IKeyValueStore) createMethod.invoke(null, config);
			} catch (Exception e) {
				ExceptionTracer.traceDeferred(e);
				ConnectorNotFoundException ex = new ConnectorNotFoundException(
						e);
				throw ex;
			}
		}
		return connector;
	}
}
