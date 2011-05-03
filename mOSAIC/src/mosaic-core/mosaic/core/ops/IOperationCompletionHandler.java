package mosaic.core.ops;

public interface IOperationCompletionHandler {
	void onSuccess(Object result);

	<E extends Throwable> void onFailure(E error);
}
