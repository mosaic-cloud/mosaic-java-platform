package mosaic.cloudlet.runtime;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import mosaic.cloudlet.core.CloudletException;
import mosaic.cloudlet.core.ICloudlet;
import mosaic.cloudlet.core.ICloudletRequest;
import mosaic.cloudlet.core.ICloudletResponse;
import mosaic.core.log.MosaicLogger;

public class CloudletHandler<T extends ICloudlet> {
	/**
	 * Lock held on updates to cloudlet poolSize, corePoolSize, maximumPoolSize,
	 * runState, and cloudlet instance set.
	 */
	private final ReentrantLock mainLock = new ReentrantLock();
	/**
	 * Pool containing all cloudlet instances of the cloudlet. Accessed only
	 * when holding mainLock.
	 */
	private Map<String, T> cloudletPool;
	/**
	 * Current pool size, updated only while holding mainLock but volatile to
	 * allow concurrent readability even during updates.
	 */
	private volatile int poolSize;

	/**
	 * Maximum pool size, updated only while holding mainLock but volatile to
	 * allow concurrent readability even during updates.
	 */
	private int maxPoolSize;

	/**
	 * Core pool size, updated only while holding mainLock, but volatile to
	 * allow concurrent readability even during updates.
	 */
	private volatile int corePoolSize;

	public void doHandle(String target, ICloudletRequest request,
			ICloudletResponse response) throws IOException, CloudletException {

		@SuppressWarnings("unchecked")
		CloudletExecutor<ICloudlet> servlet_holder = (CloudletExecutor<ICloudlet>) request
				.getCloudlet();

		try {
			if (servlet_holder == null) {
				notFound(request, response);
			} else {
				servlet_holder.handleRequest(request, response);
			}
		} catch (Exception e) {
			// TODO send error message
			MosaicLogger.getLogger().error(e.getMessage());
		}
	}

	private void notFound(ICloudletRequest request, ICloudletResponse response) {
		MosaicLogger.getLogger().debug("Not Found " + request.getRequestURI());
		response.sendError(ICloudletResponse.CLOUDLET_NOT_FOUND);

	}
}
