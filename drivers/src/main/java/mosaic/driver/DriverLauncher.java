package mosaic.driver;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.MosBasicComponentLauncher;

/**
 * Launches a driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class DriverLauncher {

	private DriverLauncher() {
		super();
		throw new UnsupportedOperationException();
	}

	public static void main(final String[] arguments) throws Throwable {
		Preconditions
				.checkArgument(
						(arguments != null) && (arguments.length == 4),
						"invalid arguments: expected <ip> <mos-url> <resource type: amqp | kv | memcached> <port>");
		String clasz = DriverCallbackType.valueOf(arguments[2].toUpperCase())
				.getCallbackClass();
		String port = Integer.toString(Integer.parseInt(arguments[3]) + 1);
		MosBasicComponentLauncher.main(new String[] { clasz, arguments[0],
				arguments[3], port, arguments[1] }, Thread.currentThread()
				.getContextClassLoader());
	}
}
