package mosaic.core;

import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;

public class TestLoggingHandler<T extends Object> implements IOperationCompletionHandler<T> {
	private static int testCounter = 0;

	@Override
	public void onSuccess(T result) {
		System.out.println("Test " + testCounter + " finished with result: "
				+ result);
		MosaicLogger.getLogger().trace(
				"Test " + testCounter + " finished with result: "
						+ result);
		testCounter++;
	}

	@Override
	public <E extends Throwable> void onFailure(E error) {
		System.out.println("Test " + testCounter + " finished with error: "
				+ error.getMessage());
		MosaicLogger.getLogger().error(
				"Test " + testCounter + " finished with error: "
						+ error.getMessage());
		testCounter++;
	}

}
