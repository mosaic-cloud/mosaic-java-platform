/*
 * #%L
 * mosaic-driver
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package eu.mosaic_cloud.driver.interop.kvstore.memcached;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.google.protobuf.ByteString;

import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.driver.interop.kvstore.KeyValueResponseTransmitter;
import eu.mosaic_cloud.driver.kvstore.KeyValueOperations;
import eu.mosaic_cloud.interop.idl.IdlCommon;
import eu.mosaic_cloud.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.interop.idl.IdlCommon.NotOk;
import eu.mosaic_cloud.interop.idl.IdlCommon.Ok;
import eu.mosaic_cloud.interop.idl.IdlCommon.Error.Builder;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.interop.kvstore.KeyValueMessage;
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
