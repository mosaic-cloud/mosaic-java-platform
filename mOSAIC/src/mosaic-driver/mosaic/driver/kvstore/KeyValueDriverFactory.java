package mosaic.driver.kvstore;

import java.lang.reflect.Method;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.driver.DriverNotFoundException;
import mosaic.driver.kvstore.memcached.MemcachedDriver;

/**
 * A factory for key-value drivers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueDriverFactory {
	public enum DriverType {
		REDIS(RedisDriver.class), MEMCACHED(
				MemcachedDriver.class);

		private final Class<? extends BaseKeyValueDriver> driverClass;

		DriverType(Class<? extends BaseKeyValueDriver> canonicalClassName) {
			this.driverClass = canonicalClassName;
		}

		public Class<? extends BaseKeyValueDriver> getDriverClass() {
			return this.driverClass;
		}
	}

	/**
	 * Creates a driver of requested type with the specified configuration.
	 * 
	 * @param driverName
	 *            the name of the driver
	 * @param config
	 *            the configuration for the driver
	 * @return the driver
	 * @throws ConnectorNotFoundException
	 *             if driver cannot be instantiated for any reason
	 */
	public static BaseKeyValueDriver createDriver(String driverName,
			IConfiguration config) throws DriverNotFoundException  {
		DriverType type = null;
		BaseKeyValueDriver driver = null;

		for (DriverType t : DriverType.values()) {
			if (t.name().equalsIgnoreCase(driverName)) {
				type = t;
				break;
			}
		}
		if (type != null) {
			try {
				Class<?> driverClass = 
								type.getDriverClass();
				Method createMethod = driverClass.getMethod("create",
						IConfiguration.class);
				driver = (BaseKeyValueDriver) createMethod.invoke(null, config);
			} catch (Exception e) {
				ExceptionTracer.traceDeferred(e);
				DriverNotFoundException ex=new DriverNotFoundException(e);
				throw ex;
			} 
		}
		return driver;
	}
}
