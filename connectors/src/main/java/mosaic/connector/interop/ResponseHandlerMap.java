package mosaic.connector.interop;

import java.util.ArrayList;
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

	private final Map<String, List<IOperationCompletionHandler<? extends Object>>> handlerMap = new HashMap<String, List<IOperationCompletionHandler<?>>>();

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
	public <T extends Object> void addHandlers(String requestId,
			List<IOperationCompletionHandler<T>> handlers) {
		synchronized (this) {
			List<IOperationCompletionHandler<?>> eHandlers = this.handlerMap
					.get(requestId);
			if (eHandlers == null) {
				eHandlers = new ArrayList<IOperationCompletionHandler<?>>();
				for (IOperationCompletionHandler<T> handler : handlers) {
					eHandlers.add(handler);
				}
				this.handlerMap.put(requestId, eHandlers);
			} else {
				eHandlers.addAll(handlers);
			}
		}
	}

	/**
	 * Removes from the map the handlers for a request and the actual request.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @return the list of response handlers
	 */
	public List<IOperationCompletionHandler<?>> removeRequestHandlers(
			String requestId) {
		synchronized (this) {
			List<IOperationCompletionHandler<?>> handlers = this.handlerMap
					.remove(requestId);
			return handlers;
		}
	}

	/**
	 * Returns the handlers for a request and the actual request.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @return the list of response handlers
	 */
	public List<IOperationCompletionHandler<?>> getRequestHandlers(
			String requestId) {
		synchronized (this) {
			return this.handlerMap.get(requestId);
		}
	}

	/**
	 * Removes from the handler map all pending requests.
	 */
	public void cancelAllRequests() {
		synchronized (this) {
			this.handlerMap.clear();
		}
	}
}
