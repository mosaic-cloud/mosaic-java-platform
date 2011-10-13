package mosaic.driver.interop;

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
	 * Destroys the transmitter.
	 */
	public void destroy() {
		synchronized (this) {
			MosaicLogger.getLogger().trace("ResponseTransmitter destroyed.");
		}
	}

	/**
	 * Sends result to the connector's proxy.
	 * 
	 * @param session
	 *            the session to which the message belongs
	 * @param message
	 *            the message
	 */
	protected void publishResponse(Session session, Message message) {
		synchronized (this) {
			session.send(message);
		}
	}

}