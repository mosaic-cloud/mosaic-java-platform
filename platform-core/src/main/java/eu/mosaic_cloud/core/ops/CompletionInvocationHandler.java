/*
 * #%L
 * mosaic-platform-core
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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
package eu.mosaic_cloud.core.ops;

import java.lang.reflect.InvocationHandler;

/**
 * A base class for invocation handlers to be used for creating dynamic proxies
 * which can be used for controlling the execution of the operation completion
 * handlers.
 * 
 * @author Georgiana Macariu
 * 
 * @param <T>
 *            the type of the response of an asynchronous event-driven operation
 */
public abstract class CompletionInvocationHandler<T> implements // NOPMD by georgiana on 10/12/11 5:00 PM
		InvocationHandler {

	protected final IOperationCompletionHandler<T> handler;

	protected CompletionInvocationHandler(IOperationCompletionHandler<T> handler) {
		super();
		this.handler = handler;
	}

	/**
	 * Creates an invocation handler.
	 * 
	 * @param handler
	 *            the operation completion handler
	 * @return the invocation handler
	 */
	public abstract CompletionInvocationHandler<T> createHandler(
			IOperationCompletionHandler<T> handler);
}
