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

import java.io.IOException;
import java.util.List;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;

/**
 * Base class for connector reactors.
 * 
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractConnectorReactor {

	private ResponseHandlerMap dispatcher;
	private Session session;
	protected MosaicLogger logger;

	/**
	 * Creates the reactor for the connector proxy.
	 * <p>
	 * Note: The response reactor will start only after the
	 * {@link AbstractConnectorReactor#setDispatcher(ResponseHandlerMap)} is
	 * called.
	 * 
	 */
	protected AbstractConnectorReactor() {
		super();
		logger = MosaicLogger.createLogger(this);
	}

	/**
	 * Destroys this reactor.
	 */
	public abstract void destroy();

	/**
	 * Returns the dispatcher which holds the mappings between the requests and
	 * the handlers for their responses.
	 * 
	 * @return the dispatcher
	 */
	protected ResponseHandlerMap getDispatcher() {
		return this.dispatcher;
	}

	/**
	 * Returns the session for the connector proxy.
	 * 
	 * @return the messaging session
	 */
	public Session getSession() {
		return this.session;
	}

	/**
	 * Process a received response for a previous submitted request.
	 * 
	 * @param message
	 *            the contents of the received message
	 * @throws IOException
	 */
	protected abstract void processResponse(Message message) throws IOException;

	/**
	 * Sets the dispatcher. The response reactor will start only after this
	 * method is called.
	 * 
	 * @param dispatcher
	 *            the dispatcher ( the map with handlers for response
	 *            processing)
	 */
	public void setDispatcher(ResponseHandlerMap dispatcher) {
		this.dispatcher = dispatcher;
	}

	/**
	 * Called after session was created.
	 * 
	 * @param session
	 *            the session
	 */
	public void sessionCreated(Session session) {
		Preconditions.checkState(this.session == null);
		this.session = session;
	}

	/**
	 * Called after session was destroyed
	 * 
	 * @param session
	 *            the session
	 */
	public void sessionDestroyed(Session session) {
		Preconditions.checkState(this.session == session);
		// cancel all pending requests
		this.dispatcher.cancelAllRequests();
	}

	/**
	 * Called when some operation on session failed.
	 * 
	 * @param session
	 *            the session
	 * @param exception
	 *            the exception
	 */
	public void sessionFailed(Session session, Throwable exception) {
		Preconditions.checkState(this.session == session);
	}

	/**
	 * Returns the handlers for a given request.
	 * 
	 * @param token
	 *            the token of the request
	 * @return the list of handlers
	 */
	protected List<IOperationCompletionHandler<?>> getHandlers(
			CompletionToken token) {
		String requestId;
		List<IOperationCompletionHandler<?>> handlers;
		requestId = token.getMessageId();
		handlers = this.getDispatcher().removeRequestHandlers(requestId);
		if (handlers == null) {
			this.logger.error(
					"No handler found for request token: " + requestId);
		}
		return handlers;
	}

}
