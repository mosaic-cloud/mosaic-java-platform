/*
 * #%L
 * mosaic-drivers
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
package eu.mosaic_cloud.drivers.interop.kvstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;

import com.google.protobuf.ByteString;
import eu.mosaic_cloud.drivers.interop.ResponseTransmitter;
import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.platform.core.ops.IOperationType;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Error;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.NotOk;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Ok;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.ListReply;

/**
 * Serializes responses for key-value stores operation requests and sends them
 * to the connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueResponseTransmitter extends ResponseTransmitter {

	/**
	 * Builds the result and sends it to the operation originator.
	 * 
	 * @param session
	 *            the session to which the response message belongs
	 * @param token
	 *            the token identifying the operation
	 * @param operation
	 *            the identifier of the operation
	 * @param result
	 *            the result
	 * @param isError
	 *            <code>true</code> if the result is actual an error
	 */
	public void sendResponse(Session session, CompletionToken token,
			IOperationType operation, Object result, boolean isError) {
		packAndSend(session, token, (KeyValueOperations) operation, result,
				isError);
	}

	protected void packAndSend(Session session, CompletionToken token,
			KeyValueOperations operation, Object result, boolean isError) {
		Message message;

		this.logger.trace(
				"KeyValueResponseTransmitter: send response for " + operation
						+ " request " + token.getMessageId() + " client id "
						+ token.getClientId());

		if (isError) {
			// create error message
			Error.Builder errorPayload = IdlCommon.Error.newBuilder();
			errorPayload.setToken(token);
			errorPayload.setErrorMessage(result.toString());
			message = new Message(KeyValueMessage.ERROR, errorPayload.build());
		} else {
			message = buildKeyValueResponse(operation, token, result);
		}

		// send response
		publishResponse(session, message);
	}

	/**
	 * Builds responses for the basic key-value store operaions.
	 * 
	 * @param operation
	 *            the operation
	 * @param token
	 *            the token of the request
	 * @param result
	 *            the result of the operation
	 * @return the message
	 */
	protected Message buildKeyValueResponse(KeyValueOperations operation, // NOPMD by georgiana on 10/12/11 2:18 PM
			CompletionToken token, Object result) {
		Message message = null; // NOPMD by georgiana on 10/12/11 2:15 PM
		switch (operation) {
		case SET:
		case DELETE:
			boolean success = (Boolean) result;
			if (success) {
				Ok.Builder okPayload = IdlCommon.Ok.newBuilder();
				okPayload.setToken(token);
				message = new Message(KeyValueMessage.OK, okPayload.build()); // NOPMD by georgiana on 10/12/11 2:16 PM
			} else {
				NotOk.Builder nokPayload = IdlCommon.NotOk.newBuilder();
				nokPayload.setToken(token);
				message = new Message(KeyValueMessage.NOK, nokPayload.build()); // NOPMD by georgiana on 10/12/11 2:16 PM
			}
			break;
		case LIST:
			ListReply.Builder listPayload = KeyValuePayloads.ListReply
					.newBuilder();
			listPayload.setToken(token);
			@SuppressWarnings("unchecked")
			List<String> resList = (List<String>) result;
			listPayload.addAllKeys(resList);
			message = new Message(KeyValueMessage.LIST_REPLY, // NOPMD by georgiana on 10/12/11 2:16 PM
					listPayload.build());
			break;
		case GET:
			GetReply.Builder getPayload = KeyValuePayloads.GetReply
					.newBuilder();
			getPayload.setToken(token);

			@SuppressWarnings("unchecked")
			Map<String, byte[]> resMap = (Map<String, byte[]>) result;
			List<KVEntry> getResults = new ArrayList<KVEntry>();
			for (Map.Entry<String, byte[]> entry : resMap.entrySet()) {
				KVEntry.Builder kvEntry = KeyValuePayloads.KVEntry.newBuilder();
				kvEntry.setKey(entry.getKey());
				if (entry.getValue() == null) {
					kvEntry.setValue(ByteString.EMPTY);
				} else {
					kvEntry.setValue(ByteString.copyFrom(entry.getValue()));
				}
				getResults.add(kvEntry.build());
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
