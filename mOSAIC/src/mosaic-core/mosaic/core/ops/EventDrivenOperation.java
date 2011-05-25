package mosaic.core.ops;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.exceptions.ResultSetException;

/**
 * Implementation of an asynchronous operation using only an event driven
 * approach. It is also possible for the caller of the operation to block until
 * its result becomes available in a manner similar to the one defined by the
 * Future pattern. However, this feature should be used only when it absolutely
 * necessary and not as a rule.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            The type of the actual result of the operation.
 */
public class EventDrivenOperation<T> implements IOperation<T>,
		IOperationCompletionHandler<T> {
	private CountDownLatch doneSignal;
	private AtomicReference<T> result;
	private AtomicReference<Throwable> exception;
	private List<IOperationCompletionHandler<T>> completionHandlers;
	private Runnable operation = null;

	/**
	 * Creates a new operation.
	 * 
	 * @param complHandlers
	 *            handlers to be called when the operation completes
	 */
	public EventDrivenOperation(
			List<IOperationCompletionHandler<T>> complHandlers) {
		super();
		doneSignal = new CountDownLatch(1);
		result = new AtomicReference<T>();
		exception = new AtomicReference<Throwable>();
		completionHandlers = new ArrayList<IOperationCompletionHandler<T>>();
		completionHandlers.add(this);
		completionHandlers.addAll(complHandlers);
		// this.operation = op;
	}

	@Override
	public boolean cancel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return (this.result.get() == null);
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		doneSignal.await();
		return this.result.get();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		doneSignal.await(timeout, unit);
		return this.result.get();
	}

	public Runnable getOperation() {
		return operation;
	}

	public void setOperation(Runnable operation) {
		if (this.operation == null)
			this.operation = operation;
	}

	@Override
	public void onSuccess(T response) {
		if (!result.compareAndSet(null, ((T) response))) {
			ExceptionTracer.traceRethrown(new ResultSetException(
					"Operation result cannot be set."));
		}
		doneSignal.countDown();
	}

	@Override
	public <E extends Throwable> void onFailure(E error) {
		if (!exception.compareAndSet(null, error)) {
			ExceptionTracer.traceRethrown(new ResultSetException(
					"Operation result cannot be set."));
		}
		doneSignal.countDown();

	}

	public List<IOperationCompletionHandler<T>> getCompletionHandlers() {
		return completionHandlers;
	}

	// private abstract class Task<V> implements Runnable {
	// private final Callable<V> callable;
	// private volatile Thread runner; // required when implementing cancel
	//
	// public Task(Callable<V> callable) {
	// this.callable = callable;
	// }
	//
	// void run() {
	// runner = Thread.currentThread();
	// V result;
	// try {
	// result = callable.call();
	// } catch (Throwable ex) {
	// setException(ex);
	// return;
	// }
	// finish(result);
	// }
	//
	// private void setException(Throwable ex) {
	// for (IOperationCompletionHandler handler :
	// EventDrivenOperation.this.completionHandlers) {
	// handler.onFailure(ex);
	// }
	//
	// }
	//
	// private void finish(V result) {
	// for (IOperationCompletionHandler handler :
	// EventDrivenOperation.this.completionHandlers) {
	// handler.onSuccess(result);
	// }
	// }
	//
	// }
}
