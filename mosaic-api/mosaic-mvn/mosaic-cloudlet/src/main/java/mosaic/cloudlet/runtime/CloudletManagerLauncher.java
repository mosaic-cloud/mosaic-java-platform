package mosaic.cloudlet.runtime;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.MosBasicComponentLauncher;

/**
 * Launches a driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class CloudletManagerLauncher {

	private CloudletManagerLauncher() {
		super();
		throw (new UnsupportedOperationException());
	}

	public static final void main(final String[] arguments) throws Throwable {
		Preconditions
				.checkArgument(
						(arguments != null) && (arguments.length == 2),
						"invalid arguments: expected <ip> <mos-url>");
		String clasz = ContainerComponentCallbacks.class.getCanonicalName();
		MosBasicComponentLauncher.main(new String[] { clasz, arguments[0],
				"29027", "29028", arguments[1] },
				CloudletManagerLauncher.class.getClassLoader());
	}
}
