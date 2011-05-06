package mosaic.core.ops;

public interface IOperationCompletionHandler<T> {
	void onSuccess(T result);

	<E extends Throwable> void onFailure(E error);
}
