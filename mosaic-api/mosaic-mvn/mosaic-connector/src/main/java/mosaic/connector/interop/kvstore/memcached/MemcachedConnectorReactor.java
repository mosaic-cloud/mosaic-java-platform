package mosaic.connector.interop.kvstore.memcached;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mosaic.connector.interop.kvstore.KeyValueConnectorReactor;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.DataEncoder;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.kvstore.KeyValuePayloads;
import mosaic.interop.idl.kvstore.KeyValuePayloads.GetReply;
import mosaic.interop.idl.kvstore.KeyValuePayloads.KVEntry;
import mosaic.interop.kvstore.KeyValueMessage;

import com.google.common.base.Preconditions;

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
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param encoder
	 *            encoder used for serializing and deserializing data stored in
	 *            the key-value store
	 * @throws Throwable
	 */
	protected MemcachedConnectorReactor(IConfiguration config,
			DataEncoder encoder) throws Throwable {
		super(config, encoder);
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
		boolean handled = false;

		if (kvMessage == KeyValueMessage.GET_REPLY) {
			KeyValuePayloads.GetReply getPayload = (GetReply) message.payload;
			List<KVEntry> resultEntries = getPayload.getResultsList();
			if (resultEntries.size() > 1) {
				token = getPayload.getToken();
				handlers = getHandlers(token);
				if (handlers != null) {
					try {
						Map<String, Object> resMap = new HashMap<String, Object>();
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
						ExceptionTracer.traceDeferred(e);
					}
				}
			}
		}

		if (!handled) {
			super.processResponse(message);
		}
	}
}
