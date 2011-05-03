package mosaic.core.ops;

import java.util.concurrent.ExecutionException;

/**
 * Defines a generic result handle of asynchronous operation. It implements the
 * {@link IResult} interface.
 * <p>
 * If you would like to use a GenericResult for the sake of managing
 * asynchronous operation but not provide a usable result, you can declare types
 * of the form GenericResult<?> and return null as a result of the underlying
 * operation.
 * <p>
 * You should either use this class or extend it instead of implementing another
 * one directly from {@link IResult}.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            The type of the actual result of the asynchronous operation.
 */
public class GenericResult<T> implements IResult<T> {

	private GenericOperation<T> operation;

	public GenericResult(GenericOperation<T> operation) {
		this.operation = operation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.core.IResult#getResult()
	 */
	public T getResult() throws InterruptedException, ExecutionException {
		return this.operation.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.core.ops.IResult#isDone()
	 */
	public final boolean isDone() {
		return this.operation.isDone();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.core.ops.IResult#cancel()
	 */
	public final boolean cancel() {
		boolean done = false;

		// first test if it was not already cancelled
		done = this.operation.isCancelled();

		if (done == false) {
			// try to cancel the operation
			done = this.operation.cancel();

			// cancellation may have failed if the operation was
			// already finished
			if (done == false) {
				done = this.operation.isDone();
			}
		}
		return done;
	}
}
