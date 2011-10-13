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
public final class KeyValueDriverFactory {
	public enum DriverType {
		REDIS(RedisDriver.class), MEMCACHED(MemcachedDriver.class), RIAKREST(
				RiakRestDriver.class), RIAKPB(RiakPBDriver.class);

		private final Class<? extends AbstractKeyValueDriver> driverClass;

		DriverType(Class<? extends AbstractKeyValueDriver> canonicalClassName) {
			this.driverClass = canonicalClassName;
		}

		public Class<? extends AbstractKeyValueDriver> getDriverClass() {
			return this.driverClass;
		}
	}

	private KeyValueDriverFactory() {
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
	public static AbstractKeyValueDriver createDriver(String driverName,
			IConfiguration config) throws DriverNotFoundException {
		DriverType type = null;
		AbstractKeyValueDriver driver = null;

		for (DriverType t : DriverType.values()) {
			if (t.name().equalsIgnoreCase(driverName)) {
				type = t;
				break;
			}
		}
		if (type != null) {
			try {
				Class<?> driverClass = type.getDriverClass();
				Method createMethod = driverClass.getMethod("create",
						IConfiguration.class);
				driver = (AbstractKeyValueDriver) createMethod.invoke(null,
						config);
			} catch (Exception e) {
				ExceptionTracer.traceDeferred(e);
				DriverNotFoundException exception = new DriverNotFoundException(
						e);
				throw exception;
			}
		}
		return driver;
	}
}
