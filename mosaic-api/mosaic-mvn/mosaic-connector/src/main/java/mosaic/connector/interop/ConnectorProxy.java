package mosaic.connector.interop;

import java.io.IOException;
import java.util.List;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Base class for connector proxys.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ConnectorProxy implements SessionCallbacks {

	private IConfiguration configuration;
	private String connectorId;
	private ZeroMqChannel commChannel;

	private ResponseHandlerMap handlerMap;
	private AbstractConnectorReactor responseReactor;

	/**
	 * Creates a proxy for a resource.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param connectorId
	 *            identifier of this connector
	 * @param reactor
	 *            the response reactor
	 * @param channel
	 *            the channel on which to communicate with the driver
	 * @throws Throwable
	 */
	protected ConnectorProxy(IConfiguration config, String connectorId,
			AbstractConnectorReactor reactor, ZeroMqChannel channel)
			throws Throwable {
		this.configuration = config;
		this.connectorId = connectorId;
		this.commChannel = channel;

		// start also the response reactor for this proxy
		this.handlerMap = new ResponseHandlerMap();
		this.responseReactor = reactor;
		this.responseReactor.setDispatcher(this.handlerMap);
	}

	protected void connect(String driverIdentifier,
			SessionSpecification session, Message initMessage) {
		this.commChannel.connect(driverIdentifier, session, initMessage, this);
	}

	/**
	 * Destroys the proxy, freeing up any allocated resources.
	 * 
	 * @throws Throwable
	 */
	public synchronized void destroy() throws Throwable {
		this.responseReactor.destroy();
		this.commChannel.terminate(500);
		MosaicLogger.getLogger().trace("ConnectorProxy destroyed.");
	}

	/**
	 * Sends a request to the driver.
	 * 
	 * @param session
	 *            the session to which the request belongs
	 * @param request
	 *            the request
	 * @throws IOException
	 */
	protected synchronized void sendRequest(Session session, Message request)
			throws IOException {
		session.send(request);
	}

	/**
	 * Returns the response reactor for the connector proxy.
	 * 
	 * @param <T>
	 *            the type of the response reactor
	 * @param reactorClass
	 *            the class of the response reactor
	 * @return the response reactor
	 */
	public <T extends AbstractConnectorReactor> T getResponseReactor(
			Class<T> reactorClass) {
		return reactorClass.cast(this.responseReactor);
	}

	/**
	 * Returns the unique ID of the connector's proxy.
	 * 
	 * @return the ID of the connector's proxy
	 */
	protected String getConnectorId() {
		return this.connectorId;
	}

	/**
	 * Registers response handlers for a request.
	 * 
	 * @param <T>
	 *            the type of the result of the request
	 * @param requestId
	 *            the identifier for the request
	 * @param handlers
	 *            the list of the response handlers
	 */
	protected <T extends Object> void registerHandlers(String requestId,
			List<IOperationCompletionHandler<T>> handlers) {
		this.handlerMap.addHandlers(requestId, handlers);
	}

	@Override
	public CallbackReference created(Session session) {
		this.responseReactor.sessionCreated(session);
		return null;
	}

	@Override
	public CallbackReference destroyed(Session session) {
		this.responseReactor.sessionDestroyed(session);
		return null;
	}

	@Override
	public CallbackReference failed(Session session, Throwable exception) {
		this.responseReactor.sessionFailed(session, exception);
		return null;
	}

	@Override
	public CallbackReference received(Session session, Message message) {
		try {
			this.responseReactor.processResponse(message);
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		}
		return null;
	}
}
