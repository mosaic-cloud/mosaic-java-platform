/*
 * #%L
 * mosaic-connectors
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
package eu.mosaic_cloud.connectors.interop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;

/**
 * Implements a Map between request (response) identifier and response handlers.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResponseHandlerMap {

	private final Map<String, List<IOperationCompletionHandler<? extends Object>>> handlerMap = Collections.synchronizedMap(new HashMap<String, List<IOperationCompletionHandler<?>>>());

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
		synchronized (this.handlerMap) {
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
		return this.handlerMap.remove(requestId);
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
		return this.handlerMap.get(requestId);
	}

	/**
	 * Removes from the handler map all pending requests.
	 */
	public void cancelAllRequests() {
		this.handlerMap.clear();
	}
}
