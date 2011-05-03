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

public class EventDrivenOperation<T> implements IOperation<T>,
		IOperationCompletionHandler {
	private CountDownLatch doneSignal;
	private AtomicReference<T> result;
	private AtomicReference<Throwable> exception;
	private List<IOperationCompletionHandler> completionHandlers;
	private Runnable operation = null;

	public EventDrivenOperation(List<IOperationCompletionHandler> complHandlers) {
		super();
		doneSignal = new CountDownLatch(1);
		result = new AtomicReference<T>();
		exception = new AtomicReference<Throwable>();
		completionHandlers = new ArrayList<IOperationCompletionHandler>();
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

	@SuppressWarnings("unchecked")
	@Override
	public void onSuccess(Object response) {
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

	public List<IOperationCompletionHandler> getCompletionHandlers() {
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
