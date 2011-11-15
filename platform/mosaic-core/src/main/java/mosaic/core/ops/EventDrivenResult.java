/*
 * #%L
 * mosaic-core
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package mosaic.core.ops;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

	@Override
	public T getResult(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.operation.get (timeout, unit);
	}
}
