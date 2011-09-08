package mosaic.core.ops;

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
public abstract class CompletionInvocationHandler<T> implements
		InvocationHandler {
	protected IOperationCompletionHandler<T> handler;

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
