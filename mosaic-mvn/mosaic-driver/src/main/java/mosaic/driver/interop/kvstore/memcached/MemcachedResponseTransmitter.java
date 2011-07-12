package mosaic.driver.interop.kvstore.memcached;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.kvstore.KeyValueResponseTransmitter;
import mosaic.driver.kvstore.KeyValueOperations;
import mosaic.interop.idl.IdlCommon;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.IdlCommon.Error.Builder;
import mosaic.interop.idl.IdlCommon.NotOk;
import mosaic.interop.idl.IdlCommon.Ok;
import mosaic.interop.kvstore.KeyValueMessage;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;

/**
 * Serializes responses for memcached operation requests and sends them to the
 * connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedResponseTransmitter extends KeyValueResponseTransmitter {

	/**
	 * Creates a new transmitter.
	 * 
	 * @param config
	 *            the configurations required to initialize the transmitter
	 */
	public MemcachedResponseTransmitter(IConfiguration config) {
		super(config);
	}

	@Override
	protected void packAndSend(Session session, CompletionToken token,
			KeyValueOperations op, Object result, boolean isError) {
		Message message = null;

		MosaicLogger.getLogger().trace(
				"MemcachedTransmitter: send response for " + op + " request "
						+ token.getMessageId() + " client id "
						+ token.getClientId());

		if (isError) {
			// create error message
			Builder errorPayload = IdlCommon.Error.newBuilder();
			errorPayload.setToken(token);
			errorPayload.setErrorMessage(result.toString());
			message = new Message(KeyValueMessage.ERROR, errorPayload.build());
		} else {
			switch (op) {
			case ADD:
			case APPEND:
			case REPLACE:
			case PREPEND:
			case CAS:
				boolean ok = (Boolean) result;
				if (ok) {
					Ok.Builder okPayload = IdlCommon.Ok.newBuilder();
					okPayload.setToken(token);
					message = new Message(KeyValueMessage.OK, okPayload.build());
				} else {
					NotOk.Builder nokPayload = IdlCommon.NotOk.newBuilder();
					nokPayload.setToken(token);
					message = new Message(KeyValueMessage.NOK,
							nokPayload.build());
				}
				break;
			default:
				message = super.buildKeyValueResponse(op, token, result);
				break;
			}
		}

		// send response
		publishResponse(session, message);
	}

}
