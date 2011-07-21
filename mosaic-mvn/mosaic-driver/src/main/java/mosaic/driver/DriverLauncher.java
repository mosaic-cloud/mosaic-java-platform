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
		throw (new UnsupportedOperationException());
	}

	public static final void main(final String[] arguments) throws Throwable {
		Preconditions
				.checkArgument(
						(arguments != null) && (arguments.length == 3),
						"invalid arguments: expected <ip> <mos-url> <resource type: amqp | kv | memcached>");
		String clasz = DriverCallbackType.valueOf(arguments[2].toUpperCase())
				.getCallbackClass().getCanonicalName();
		MosBasicComponentLauncher.main(new String[] { clasz, arguments[0],
				"29017", "29018", arguments[1] },
				DriverLauncher.class.getClassLoader());
	}
}
