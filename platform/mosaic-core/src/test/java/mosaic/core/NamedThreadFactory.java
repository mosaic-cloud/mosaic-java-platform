package mosaic.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class NamedThreadFactory implements ThreadFactory {

	private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final ThreadGroup group;

	NamedThreadFactory(final String poolName) {
		this.group = new ThreadGroup(poolName + "-"
				+ NamedThreadFactory.POOL_NUMBER.getAndIncrement());
	}

	@Override
	public Thread newThread(final Runnable runner) {
		return new Thread(this.group, runner, this.group.getName() + "-thread-"
				+ this.threadNumber.getAndIncrement(), 0);
	}
}