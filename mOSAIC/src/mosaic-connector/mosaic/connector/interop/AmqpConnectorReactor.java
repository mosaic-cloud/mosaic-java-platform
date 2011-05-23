package mosaic.connector.interop;

import java.io.IOException;
import java.util.List;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.interop.idl.amqp.AmqpError;
import mosaic.interop.idl.amqp.CompletionToken;
import mosaic.interop.idl.amqp.OperationNames;
import mosaic.interop.idl.amqp.OperationResponse;

/**
 * Implements a reactor for processing asynchronous requests issued by the AMQP
 * connector.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpConnectorReactor extends AbstractConnectorReactor {
	private static final String DEFAULT_QUEUE_NAME = "amqp_responses";
	private static final String DEFAULT_EXCHANGE_NAME = "amqp";

	/**
	 * Creates the reactor for the AMQP connector proxy.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param bindingKey
	 *            queue binding key
	 */
	public AmqpConnectorReactor(IConfiguration config, String bindingKey) {
		super(config, bindingKey, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.connector.interop.AbstractConnectorReactor#processResponse(byte[])
	 */
	@SuppressWarnings("unchecked")
	protected void processResponse(byte[] message) throws IOException {
		OperationResponse response = new OperationResponse();
		response = SerDesUtils.deserializeWithSchema(message, response);
		CompletionToken token = (CompletionToken) response.get(0);
		OperationNames op = (OperationNames) response.get(1);
		boolean isError = (Boolean) response.get(2);
		String id = ((CharSequence) token.get(0)).toString();

		List<IOperationCompletionHandler<?>> handlers = super.getDispatcher()
				.removeRequestHandlers(id);
		if (handlers == null) {
			MosaicLogger.getLogger().error(
					"No handler found for request token: " + id);
			return;
		}

		if (isError) {
			AmqpError error = (AmqpError) response.get(3);
			for (IOperationCompletionHandler<?> handler : handlers) {
				handler.onFailure(new Exception(((CharSequence) error.get(0))
						.toString()));
			}
			return;
		}

		switch (op) {
		case OPEN_CONNECTION:
		case CLOSE_CONNECTION:
		case DECLARE_EXCHANGE:
		case DECLARE_QUEUE:
		case BIND_QUEUE:
		case PUBLISH:
		case CANCEL:
		case ACK:
		case GET:
			Boolean resultB = (Boolean) response.get(3);
			for (IOperationCompletionHandler<?> handler : handlers) {
				((IOperationCompletionHandler<Boolean>) handler)
						.onSuccess(resultB);
			}
			break;
		case CONSUME:
			String resultStr = (String) response.get(3);
			for (IOperationCompletionHandler<?> handler : handlers) {
				((IOperationCompletionHandler<String>) handler)
						.onSuccess(resultStr);
			}

			break;
		default:
			break;
		}
	}
}
