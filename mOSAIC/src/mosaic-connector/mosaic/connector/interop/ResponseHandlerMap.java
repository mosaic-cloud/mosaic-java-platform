package mosaic.connector.interop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mosaic.core.ops.IOperationCompletionHandler;

/**
 * Implements a Map between request (response) identifier and response handlers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResponseHandlerMap {

	private Map<String, List<IOperationCompletionHandler>> handlerMap = new HashMap<String, List<IOperationCompletionHandler>>();

	public ResponseHandlerMap() {
		super();
	}

	/**
	 * Add handlers for the response of the request with the given identifier.
	 * If other handlers have been added previously for the request, these
	 * handlers will be appended to the existing ones.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @param handlers
	 *            the list of handlers to add
	 */
	public synchronized void addHandlers(String requestId,
			List<IOperationCompletionHandler> handlers) {
		List<IOperationCompletionHandler> oldHandlers = this.handlerMap
				.get(requestId);
		if (oldHandlers != null) {
			oldHandlers.addAll(handlers);
		} else {
			this.handlerMap.put(requestId, handlers);
		}
	}

	/**
	 * Removes from the map the handlers for a request and the actual request.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @return the list of response handlers
	 */
	public synchronized List<IOperationCompletionHandler> removeRequestHandlers(
			String requestId) {
		return this.handlerMap.remove(requestId);
	}

	/**
	 * Returns the handlers for a request and the actual request.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @return the list of response handlers
	 */
	public synchronized List<IOperationCompletionHandler> getRequestHandlers(
			String requestId) {
		return this.handlerMap.get(requestId);
	}
}
