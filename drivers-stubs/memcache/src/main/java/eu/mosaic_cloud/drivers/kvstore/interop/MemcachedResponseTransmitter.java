/*
 * #%L
 * mosaic-drivers-stubs-memcache
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

package eu.mosaic_cloud.drivers.kvstore.interop;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Error.Builder;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.NotOk;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Ok;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;

import com.google.protobuf.ByteString;


/**
 * Serializes responses for memcached operation requests and sends them to the
 * connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedResponseTransmitter
		extends KeyValueResponseTransmitter
{
	@Override
	protected void packAndSend (final Session session, final CompletionToken token, final KeyValueOperations operation, final Object result, final boolean isError)
	{
		Message message;
		this.logger.trace ("MemcachedTransmitter: send response for " + operation + " request " + token.getMessageId () + " client id " + token.getClientId ());
		if (isError) {
			// NOTE: create error message
			final Builder errorPayload = IdlCommon.Error.newBuilder ();
			errorPayload.setToken (token);
			errorPayload.setErrorMessage (result.toString ());
			message = new Message (KeyValueMessage.ERROR, errorPayload.build ());
		} else {
			switch (operation) {
				case ADD :
				case APPEND :
				case REPLACE :
				case PREPEND :
				case CAS :
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
				case GET_BULK :
					final GetReply.Builder getPayload = KeyValuePayloads.GetReply.newBuilder ();
					getPayload.setToken (token);
					@SuppressWarnings ("unchecked") final Map<String, eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage> resMap = (Map<String, eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage>) result;
					final List<KVEntry> getResults = new ArrayList<KVEntry> ();
					for (final Entry<String, eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage> entry : resMap.entrySet ()) {
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
					message = super.buildKeyValueResponse (operation, token, result);
					break;
			}
		}
		// NOTE: send response
		this.publishResponse (session, message);
	}
}
