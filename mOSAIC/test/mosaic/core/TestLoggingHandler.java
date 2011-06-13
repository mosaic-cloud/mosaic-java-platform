package mosaic.core;

import java.util.concurrent.locks.ReentrantLock;

import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;

public class TestLoggingHandler<T extends Object> implements
		IOperationCompletionHandler<T> {
	private static int testCounter = 0;
	private static ReentrantLock lock = new ReentrantLock();

	@Override
	public void onSuccess(T result) {
		MosaicLogger.getLogger().trace(
				"Test " + testCounter + " finished with result: " + result);
		lock.lock();
		testCounter++;
		lock.unlock();
	}

	@Override
	public <E extends Throwable> void onFailure(E error) {
		MosaicLogger.getLogger().error(
				"Test " + testCounter + " finished with error: "
						+ error.getMessage());
		lock.lock();
		testCounter++;
		lock.unlock();
	}

}
