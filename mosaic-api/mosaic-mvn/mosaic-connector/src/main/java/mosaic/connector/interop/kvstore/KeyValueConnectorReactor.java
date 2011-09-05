package mosaic.connector.interop.kvstore;

import java.io.IOException;
import java.util.List;

import mosaic.connector.interop.AbstractConnectorReactor;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.DataEncoder;
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

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.interoperability.core.Message;

/**
 * Implements a reactor for processing asynchronous requests issued by the
 * key-value store connector.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueConnectorReactor extends AbstractConnectorReactor {
	protected DataEncoder dataEncoder;

	/**
	 * Creates the reactor for the key-value store connector proxy.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 */
	protected KeyValueConnectorReactor(IConfiguration config,
			DataEncoder encoder) {
		super(config);
		this.dataEncoder = encoder;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void processResponse(Message message) throws IOException {
		Preconditions
				.checkArgument(message.specification instanceof KeyValueMessage);

		KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
		CompletionToken token = null;
		Object data;
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
				for (IOperationCompletionHandler<?> handler : handlers) {
					handler.onFailure(new Exception(errorPayload
							.getErrorMessage()));
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
			try {
				KeyValuePayloads.GetReply getPayload = (GetReply) message.payload;
				token = getPayload.getToken();
				handlers = getHandlers(token);
				if (handlers != null) {
					List<KVEntry> resultEntries = getPayload.getResultsList();
					if (resultEntries.size() > 0) {
						try {
							data = this.dataEncoder.decode(resultEntries.get(0)
									.getValue().toByteArray());
							for (IOperationCompletionHandler<?> handler : handlers) {
								((IOperationCompletionHandler<Object>) handler)
										.onSuccess(data);
							}
						} catch (Exception e) {
							ExceptionTracer.traceDeferred(e);
						}
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			break;
		case ACCESS:
		case ABORTED:
		case GET_REQUEST:
		case SET_REQUEST:
		case DELETE_REQUEST:
		case LIST_REQUEST:
			break;
		}
	}
}
