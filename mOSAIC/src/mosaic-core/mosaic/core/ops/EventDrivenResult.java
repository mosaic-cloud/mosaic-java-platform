package mosaic.core.ops;

import java.util.concurrent.ExecutionException;

public class EventDrivenResult<T> implements IResult<T> {
	private EventDrivenOperation<T> operation;

	public EventDrivenResult(EventDrivenOperation<T> operation) {
		super();
		this.operation = operation;
	}

	@Override
	public boolean isDone() {
		return this.operation.isDone();
	}

	@Override
	public boolean cancel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T getResult() throws InterruptedException, ExecutionException {
		return this.operation.get();
	}

}
