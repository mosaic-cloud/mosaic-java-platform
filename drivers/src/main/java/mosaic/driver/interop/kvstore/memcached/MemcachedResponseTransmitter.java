package mosaic.driver.interop.kvstore.memcached;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mosaic.core.log.MosaicLogger;
import mosaic.driver.interop.kvstore.KeyValueResponseTransmitter;
import mosaic.driver.kvstore.KeyValueOperations;
import mosaic.interop.idl.IdlCommon;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.IdlCommon.Error.Builder;
import mosaic.interop.idl.IdlCommon.NotOk;
import mosaic.interop.idl.IdlCommon.Ok;
import mosaic.interop.idl.kvstore.KeyValuePayloads;
import mosaic.interop.idl.kvstore.KeyValuePayloads.GetReply;
import mosaic.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import mosaic.interop.kvstore.KeyValueMessage;

import com.google.protobuf.ByteString;

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

	@Override
	protected void packAndSend(Session session, CompletionToken token, // NOPMD by georgiana on 10/12/11 3:03 PM
			KeyValueOperations operation, Object result, boolean isError) {
		Message message;

		MosaicLogger.getLogger().trace(
				"MemcachedTransmitter: send response for " + operation
						+ " request " + token.getMessageId() + " client id "
						+ token.getClientId());
		if (isError) {
			// create error message
			Builder errorPayload = IdlCommon.Error.newBuilder();
			errorPayload.setToken(token);
			errorPayload.setErrorMessage(result.toString());
			message = new Message(KeyValueMessage.ERROR, errorPayload.build());
		} else {
			switch (operation) {
			case ADD:
			case APPEND:
			case REPLACE:
			case PREPEND:
			case CAS:
				boolean success = (Boolean) result;
				if (success) {
					Ok.Builder okPayload = IdlCommon.Ok.newBuilder();
					okPayload.setToken(token);
					message = new Message(KeyValueMessage.OK, okPayload.build()); // NOPMD by georgiana on 10/12/11 3:02 PM
				} else {
					NotOk.Builder nokPayload = IdlCommon.NotOk.newBuilder();
					nokPayload.setToken(token);
					message = new Message(KeyValueMessage.NOK, // NOPMD by georgiana on 10/12/11 3:02 PM
							nokPayload.build());
				}
				break;
			case GET_BULK:
				GetReply.Builder getPayload = KeyValuePayloads.GetReply
						.newBuilder();
				getPayload.setToken(token);

				@SuppressWarnings("unchecked")
				Map<String, byte[]> resMap = (Map<String, byte[]>) result;
				List<KVEntry> getResults = new ArrayList<KVEntry>();
				for (Map.Entry<String, byte[]> entry : resMap.entrySet()) {
					KVEntry.Builder kvEntry = KeyValuePayloads.KVEntry
							.newBuilder();
					kvEntry.setKey(entry.getKey());
					if (entry.getValue() == null) {
						kvEntry.setValue(ByteString.EMPTY);
					} else {
						kvEntry.setValue(ByteString.copyFrom(entry.getValue()));
					}
					getResults.add(kvEntry.build());
				}
				getPayload.addAllResults(getResults);
				message = new Message(KeyValueMessage.GET_REPLY,
						getPayload.build());
				break;
			default:
				message = super.buildKeyValueResponse(operation, token, result);
				break;
			}
		}

		// send response
		publishResponse(session, message);

	}

}
