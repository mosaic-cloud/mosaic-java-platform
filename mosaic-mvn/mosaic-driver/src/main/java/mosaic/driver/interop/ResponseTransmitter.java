package mosaic.driver.interop;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;

/**
 * Base class for driver response transmitter.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResponseTransmitter {

	/**
	 * Creates a new transmitter.
	 * 
	 * @param config
	 *            the configurations required to initialize the transmitter
	 */
	public ResponseTransmitter(IConfiguration config) {
		super();

	}

	/**
	 * Destroys the transmitter.
	 */

	public synchronized void destroy() {
		MosaicLogger.getLogger().trace("ResponseTransmitter destroyed.");
	}

	/**
	 * Sends result to the connector's proxy.
	 * 
	 * @param session
	 *            the session to which the message belongs
	 * @param message
	 *            the message
	 */
	protected synchronized void publishResponse(Session session, Message message) {
		session.send(message);
	}

}