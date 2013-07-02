/*
 * #%L
 * mosaic-drivers-stubs-kv-common
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.drivers.kvstore.interop;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.drivers.interop.ResponseTransmitter;
import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.drivers.ops.IOperationType;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Error;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.NotOk;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Ok;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.ListReply;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;

import com.google.protobuf.ByteString;


/**
 * Serializes responses for key-value stores operation requests and sends them
 * to the connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueResponseTransmitter
		extends ResponseTransmitter
{
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
	public void sendResponse (final Session session, final CompletionToken token, final IOperationType operation, final Object result, final boolean isError)
	{
		this.packAndSend (session, token, (KeyValueOperations) operation, result, isError);
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
	protected Message buildKeyValueResponse (final KeyValueOperations operation, final CompletionToken token, final Object result)
	{
		Message message = null;
		switch (operation) {
			case SET :
			case DELETE :
				final boolean success = (Boolean) result;
				if (success) {
					final Ok.Builder okPayload = IdlCommon.Ok.newBuilder ();
					okPayload.setToken (token);
					message = new Message (KeyValueMessage.OK, okPayload.build ());
				} else {
					final NotOk.Builder nokPayload = IdlCommon.NotOk.newBuilder ();
					nokPayload.setToken (token);
					message = new Message (KeyValueMessage.NOK, nokPayload.build ());
				}
				break;
			case LIST :
				final ListReply.Builder listPayload = KeyValuePayloads.ListReply.newBuilder ();
				listPayload.setToken (token);
				@SuppressWarnings ("unchecked") final List<String> resList = (List<String>) result;
				listPayload.addAllKeys (resList);
				message = new Message (KeyValueMessage.LIST_REPLY, listPayload.build ());
				break;
			case GET :
				final GetReply.Builder getPayload = KeyValuePayloads.GetReply.newBuilder ();
				getPayload.setToken (token);
				@SuppressWarnings ("unchecked") final Map<String, eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage> resMap = (Map<String, eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage>) result;
				final List<KVEntry> getResults = new ArrayList<KVEntry> ();
				for (final Map.Entry<String, eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage> entry : resMap.entrySet ()) {
					final KVEntry.Builder kvEntry = KeyValuePayloads.KVEntry.newBuilder ();
					kvEntry.setKey (entry.getKey ());
					final IdlCommon.Envelope.Builder envelope = IdlCommon.Envelope.newBuilder ();
					if (null != entry.getValue ().getContentEncoding ())
						envelope.setContentEncoding (entry.getValue ().getContentEncoding ());
					else
						envelope.setContentEncoding ("");
					if (null != entry.getValue ().getContentType ())
						envelope.setContentType (entry.getValue ().getContentType ());
					else
						envelope.setContentType ("");
					kvEntry.setEnvelope (envelope.build ());
					if (entry.getValue ().getData () == null) {
						kvEntry.setValue (ByteString.EMPTY);
					} else {
						kvEntry.setValue (ByteString.copyFrom (entry.getValue ().getData ()));
					}
					getResults.add (kvEntry.build ());
				}
				getPayload.addAllResults (getResults);
				message = new Message (KeyValueMessage.GET_REPLY, getPayload.build ());
				break;
			default:
				break;
		}
		return message;
	}
	
	protected void packAndSend (final Session session, final CompletionToken token, final KeyValueOperations operation, final Object result, final boolean isError)
	{
		Message message;
		this.logger.trace ("KeyValueResponseTransmitter: send response for " + operation + " request " + token.getMessageId () + " client id " + token.getClientId ());
		if (isError) {
			// NOTE: create error message
			final Error.Builder errorPayload = IdlCommon.Error.newBuilder ();
			errorPayload.setToken (token);
			errorPayload.setErrorMessage (result.toString ());
			message = new Message (KeyValueMessage.ERROR, errorPayload.build ());
		} else {
			message = this.buildKeyValueResponse (operation, token, result);
		}
		// NOTE: send response
		this.publishResponse (session, message);
	}
}
