/*
 * #%L
 * mosaic-connectors
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
package eu.mosaic_cloud.connectors.interop.kvstore;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.connectors.interop.AbstractConnectorReactor;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Error;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.NotOk;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.Ok;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetReply;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.ListReply;
import eu.mosaic_cloud.platform.interop.kvstore.KeyValueMessage;

/**
 * Implements a reactor for processing asynchronous requests issued by the
 * key-value store connector.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueConnectorReactor extends AbstractConnectorReactor { // NOPMD by georgiana on 10/13/11 12:41 PM

	protected DataEncoder<?> dataEncoder;

	/**
	 * Creates the reactor for the key-value store connector proxy.
	 * 
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 */
	protected KeyValueConnectorReactor(DataEncoder<?> encoder) {
		super();
		this.dataEncoder = encoder;
	}

	/**
	 * Destroys this reactor.
	 */
	@Override
	public void destroy() {
		// nothing to do here
		// if it does something don'y forget synchronized
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void processResponse(Message message) throws IOException { // NOPMD by georgiana on 10/13/11 12:41 PM
		Preconditions
				.checkArgument(message.specification instanceof KeyValueMessage);

		KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
		CompletionToken token;
		List<IOperationCompletionHandler<?>> handlers;
		switch (kvMessage) {
		case OK:
			IdlCommon.Ok okPayload = (Ok) message.payload;
			token = okPayload.getToken();
			handlers = getHandlers(token);
			if (handlers != null) {
				for (IOperationCompletionHandler<?> handler : handlers) {
					((IOperationCompletionHandler<Boolean>) handler)
							.onSuccess(true);
				}
			}
			break;
		case NOK:
			IdlCommon.NotOk nokPayload = (NotOk) message.payload;
			token = nokPayload.getToken();
			handlers = getHandlers(token);
			if (handlers != null) {
				for (IOperationCompletionHandler<?> handler : handlers) {
					((IOperationCompletionHandler<Boolean>) handler)
							.onSuccess(false);
				}
			}
			break;
		case ERROR:
			IdlCommon.Error errorPayload = (Error) message.payload;
			token = errorPayload.getToken();
			handlers = getHandlers(token);
			if (handlers != null) {
				Exception exception = new Exception(
						errorPayload.getErrorMessage()); // NOPMD by georgiana on 10/13/11 12:40 PM
				for (IOperationCompletionHandler<?> handler : handlers) {
					handler.onFailure(exception);
				}
			}
			break;
		case LIST_REPLY:
			KeyValuePayloads.ListReply listPayload = (ListReply) message.payload;
			token = listPayload.getToken();
			handlers = getHandlers(token);
			if (handlers != null) {
				for (IOperationCompletionHandler<?> handler : handlers) {
					((IOperationCompletionHandler<List<String>>) handler)
							.onSuccess(listPayload.getKeysList());
				}
			}
			break;
		case GET_REPLY:
			KeyValuePayloads.GetReply getPayload = (GetReply) message.payload;
			token = getPayload.getToken();
			handlers = getHandlers(token);
			if (handlers != null) {
				List<KVEntry> resultEntries = getPayload.getResultsList();
				if (!resultEntries.isEmpty()) {
					try {
						Object data = this.dataEncoder.decode(resultEntries // NOPMD by georgiana on 10/13/11 12:41 PM
								.get(0).getValue().toByteArray());
						for (IOperationCompletionHandler<?> handler : handlers) {
							((IOperationCompletionHandler<Object>) handler)
									.onSuccess(data);
						}
					} catch (Exception e) {
						ExceptionTracer.traceIgnored(e);
					}
				}
			}
			break;
		case ACCESS:
		case ABORTED:
		case GET_REQUEST:
		case SET_REQUEST:
		case DELETE_REQUEST:
		case LIST_REQUEST:
		default:
			break;
		}
	}
}
