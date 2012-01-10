/*
 * #%L
 * mosaic-connector
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
package eu.mosaic_cloud.connector.interop.kvstore.memcached;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.google.common.base.Preconditions;

import eu.mosaic_cloud.connector.interop.kvstore.KeyValueConnectorReactor;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.utils.DataEncoder;
import eu.mosaic_cloud.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.interop.kvstore.KeyValueMessage;
import eu.mosaic_cloud.interoperability.core.Message;

/**
 * Implements a reactor for processing asynchronous requests issued by the
 * Key-Value store connector.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedConnectorReactor extends KeyValueConnectorReactor {

	/**
	 * Creates the reactor for the key-value store connector proxy.
	 * 
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @throws Throwable
	 */
	protected MemcachedConnectorReactor(DataEncoder<?> encoder)
			throws Throwable {
		super(encoder);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void processResponse(Message message) throws IOException {
		Preconditions
				.checkArgument(message.specification instanceof KeyValueMessage);

		KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
		CompletionToken token;
		Object data;
		List<IOperationCompletionHandler<?>> handlers;
		boolean handled = false; // NOPMD by georgiana on 10/13/11 12:48 PM
		if (kvMessage == KeyValueMessage.GET_REPLY) {
			KeyValuePayloads.GetReply getPayload = (GetReply) message.payload;
			List<KVEntry> resultEntries = getPayload.getResultsList();
			if (resultEntries.size() > 1) {
				token = getPayload.getToken();
				handlers = getHandlers(token);
				if (handlers != null) { // NOPMD by georgiana on 10/13/11 12:49 PM
					try {
						Map<String, Object> resMap = new HashMap<String, Object>(); // NOPMD 
						for (KVEntry entry : resultEntries) {
							data = this.dataEncoder.decode(entry.getValue()
									.toByteArray());
							resMap.put(entry.getKey(), data);
						}

						for (IOperationCompletionHandler<?> handler : handlers) {
							((IOperationCompletionHandler<Map<String, Object>>) handler)
									.onSuccess(resMap);
						}
						handled = true;
					} catch (Exception e) {
						ExceptionTracer.traceIgnored(e);
					}
				}
			}
		}

		if (!handled) {
			super.processResponse(message);
		}
	}
}
