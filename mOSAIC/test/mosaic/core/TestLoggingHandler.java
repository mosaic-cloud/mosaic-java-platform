package mosaic.core;

import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;

public class TestLoggingHandler<T extends Object> implements
		IOperationCompletionHandler<T> {
	private String testName="";
	
	public TestLoggingHandler(String testName) {
		super();
		this.testName = testName;
	}


	@Override
	public void onSuccess(T result) {
		MosaicLogger.getLogger().trace(
				"Test " + testName + " finished with result: " + result);
	}

	@Override
	public <E extends Throwable> void onFailure(E error) {
		MosaicLogger.getLogger().error(
				"Test " + testName+ " finished with error: "
						+ error.getMessage());
	}

	
}
