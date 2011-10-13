package mosaic.core.ops;

import java.util.concurrent.ExecutionException;

/**
 * Defines a result handle of an event-driven asynchronous operation. The
 * asynchronous operation is implemented using events in this case. It
 * implements the {@link IResult} interface.
 * <p>
 * If you would like to use a EventDrivenResult for the sake of managing
 * asynchronous operation but not provide a usable result, you can declare types
 * of the form EventDrivenResult<?> and return null as a result of the
 * underlying operation.
 * <p>
 * For working with {@link EventDrivenOperation} types, you should either use
 * this class or extend it instead of implementing another one directly from
 * {@link IResult}.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            The type of the actual result of the asynchronous operation.
 */
public class EventDrivenResult<T> implements IResult<T> {

	private final EventDrivenOperation<T> operation;

	public EventDrivenResult(final EventDrivenOperation<T> operation) {
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
