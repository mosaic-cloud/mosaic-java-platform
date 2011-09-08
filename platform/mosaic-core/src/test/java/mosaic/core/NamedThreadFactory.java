package mosaic.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class NamedThreadFactory implements ThreadFactory {
	static final AtomicInteger poolNumber = new AtomicInteger(1);
	final AtomicInteger threadNumber = new AtomicInteger(1);
	final ThreadGroup group;

	NamedThreadFactory(String poolName) {
		this.group = new ThreadGroup(poolName + "-"
				+ NamedThreadFactory.poolNumber.getAndIncrement());
	}

	@Override
	public Thread newThread(Runnable r) {
		return new Thread(this.group, r, this.group.getName() + "-thread-"
				+ this.threadNumber.getAndIncrement(), 0);
	}
}