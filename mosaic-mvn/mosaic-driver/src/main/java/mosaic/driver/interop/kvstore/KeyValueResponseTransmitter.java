package mosaic.driver.interop.kvstore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationType;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.interop.ResponseTransmitter;
import mosaic.driver.kvstore.KeyValueOperations;
import mosaic.interop.idl.IdlCommon;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.IdlCommon.Error;
import mosaic.interop.idl.IdlCommon.NotOk;
import mosaic.interop.idl.IdlCommon.Ok;
import mosaic.interop.idl.kvstore.KeyValuePayloads;
import mosaic.interop.idl.kvstore.KeyValuePayloads.GetReply;
import mosaic.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import mosaic.interop.idl.kvstore.KeyValuePayloads.ListReply;
import mosaic.interop.kvstore.KeyValueMessage;

import com.google.protobuf.ByteString;

import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;

/**
 * Serializes responses for key-value stores operation requests and sends them
 * to the connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueResponseTransmitter extends ResponseTransmitter {

	/**
	 * Creates a new transmitter.
	 * 
	 * @param config
	 *            the configurations required to initialize the transmitter
	 */
	public KeyValueResponseTransmitter(IConfiguration config) {
		super(config);
	}

	/**
	 * Builds the result and sends it to the operation originator.
	 * 
	 * @param session
	 *            the session to which the response message belongs
	 * @param token
	 *            the token identifying the operation
	 * @param op
	 *            the identifier of the operation
	 * @param result
	 *            the result
	 * @param isError
	 *            <code>true</code> if the result is actual an error
	 */
	public void sendResponse(Session session, CompletionToken token,
			IOperationType op, Object result, boolean isError) {

		// if (!(op instanceof KeyValueOperations))
		// return;

		packAndSend(session, token, (KeyValueOperations) op, result, isError);
	}

	protected void packAndSend(Session session, CompletionToken token,
			KeyValueOperations op, Object result, boolean isError) {
		Message message = null;

		MosaicLogger.getLogger().trace(
				"KeyValueResponseTransmitter: send response for " + op
						+ " request " + token.getMessageId() + " client id "
						+ token.getClientId());

		if (isError) {
			// create error message
			Error.Builder errorPayload = IdlCommon.Error.newBuilder();
			errorPayload.setToken(token);
			errorPayload.setErrorMessage(result.toString());
			message = new Message(KeyValueMessage.ERROR, errorPayload.build());
		} else {
			message = buildKeyValueResponse(op, token, result);
		}

		// send response
		publishResponse(session, message);
	}

	/**
	 * Builds responses for the basic key-value store operaions.
	 * 
	 * @param op
	 *            the operation
	 * @param token
	 *            the token of the request
	 * @param result
	 *            the result of the operation
	 * @return the message
	 */
	protected Message buildKeyValueResponse(KeyValueOperations op,
			CompletionToken token, Object result) {
		Message message = null;
		byte[] dataBytes;

		switch (op) {
		case SET:
		case DELETE:
			boolean ok = (Boolean) result;
			if (ok) {
				Ok.Builder okPayload = IdlCommon.Ok.newBuilder();
				okPayload.setToken(token);
				message = new Message(KeyValueMessage.OK, okPayload.build());
			} else {
				NotOk.Builder nokPayload = IdlCommon.NotOk.newBuilder();
				nokPayload.setToken(token);
				message = new Message(KeyValueMessage.NOK, nokPayload.build());
			}
			break;
		case LIST:
			ListReply.Builder listPayload = KeyValuePayloads.ListReply
					.newBuilder();
			listPayload.setToken(token);
			@SuppressWarnings("unchecked")
			List<String> resList = (List<String>) result;
			listPayload.addAllKeys(resList);
			message = new Message(KeyValueMessage.LIST_REPLY,
					listPayload.build());
			break;
		case GET:
			GetReply.Builder getPayload = KeyValuePayloads.GetReply
					.newBuilder();
			getPayload.setToken(token);

			@SuppressWarnings("unchecked")
			Map<String, Object> resMap = (Map<String, Object>) result;
			List<KVEntry> getResults = new ArrayList<KVEntry>();
			for (Map.Entry<String, Object> entry : resMap.entrySet()) {
				KVEntry.Builder kvEntry = KeyValuePayloads.KVEntry.newBuilder();
				kvEntry.setKey(entry.getKey());
				try {
					if (entry.getValue() instanceof ByteString) 
						kvEntry.setValue((ByteString) entry.getValue());
					else {
						dataBytes = SerDesUtils.toBytes(entry.getValue());
						kvEntry.setValue(ByteString.copyFrom(dataBytes));
					}
					getResults.add(kvEntry.build());
				} catch (IOException e) {
					MosaicLogger.getLogger().error(e.getMessage());
				}

			}
			getPayload.addAllResults(getResults);
			message = new Message(KeyValueMessage.GET_REPLY, getPayload.build());
			break;
		default:
			break;
		}
		return message;
	}

}
