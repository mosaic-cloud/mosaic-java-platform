package mosaic.driver.interop;

import java.io.IOException;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.queue.AmqpOperations;
import mosaic.interop.idl.amqp.AmqpError;
import mosaic.interop.idl.amqp.CompletionToken;
import mosaic.interop.idl.amqp.OperationNames;
import mosaic.interop.idl.amqp.OperationResponse;

/**
 * Serializes responses for AMQP operation requests and sends them to the
 * connector proxy which requested the operations.
 * 
 * @author Georgiana Macariu
 * 
 */
public class AmqpResponseTransmitter extends ResponseTransmitter {
	// private static final String DEFAULT_QUEUE_NAME = "amqp_responses";

	/**
	 * Creates a new transmitter.
	 * 
	 * @param config
	 *            the configurations required to initialize the transmitter
	 */
	public AmqpResponseTransmitter(IConfiguration config) {
		super(config);
	}

	/**
	 * Builds the result and sends it to the operation originator.
	 * 
	 * @param token
	 *            the token identifying the operation
	 * @param op
	 *            the identifier of the operation
	 * @param result
	 *            the result
	 * @param isError
	 *            <code>true</code> if the result is actual an error
	 */
	public void sendResponse(CompletionToken token, AmqpOperations op,
			Object result, boolean isError) {
		byte[] message;
		String routingKey = ((CharSequence) token.get(1)).toString();

		OperationResponse response = new OperationResponse();
		try {
			response.put(0, token);
			response.put(1, convertOperationType(op));
			response.put(2, isError);
			if (!isError) {
				if (result instanceof Boolean) {
					response.put(3, (Boolean) result);
				} else {
					response.put(3, (String) result);
				}
			} else {
				AmqpError error = new AmqpError();
				error.put(0, (String) result);
				response.put(3, error);
			}
			// send response
			message = SerDesUtils.serializeWithSchema(response);
			publishResponse(routingKey, message);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private OperationNames convertOperationType(AmqpOperations op) {
		OperationNames cOp = null;
		switch (op) {
		case DECLARE_EXCHANGE:
			cOp = OperationNames.DECLARE_EXCHANGE;
			break;
		case DECLARE_QUEUE:
			cOp = OperationNames.DECLARE_QUEUE;
			break;
		case BIND_QUEUE:
			cOp = OperationNames.BIND_QUEUE;
			break;
		case OPEN_CONNECTION:
			cOp = OperationNames.OPEN_CONNECTION;
			break;
		case CLOSE_CONNECTION:
			cOp = OperationNames.CLOSE_CONNECTION;
			break;
		case PUBLISH:
			cOp = OperationNames.PUBLISH;
			break;
		case CONSUME:
			cOp = OperationNames.CONSUME;
			break;
		case CANCEL:
			cOp = OperationNames.CANCEL;
			break;
		case GET:
			cOp = OperationNames.GET;
			break;
		case ACK:
			cOp = OperationNames.ACK;
			break;
		}
		return cOp;
	}
}
