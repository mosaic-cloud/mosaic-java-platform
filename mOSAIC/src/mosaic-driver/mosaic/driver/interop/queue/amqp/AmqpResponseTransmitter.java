package mosaic.driver.interop.queue.amqp;

import java.io.IOException;
import java.nio.ByteBuffer;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.interop.ResponseTransmitter;
import mosaic.driver.queue.amqp.AmqpInboundMessage;
import mosaic.driver.queue.amqp.AmqpOperations;
import mosaic.interop.idl.amqp.AmqpError;
import mosaic.interop.idl.amqp.CancelMssg;
import mosaic.interop.idl.amqp.CancelOkMssg;
import mosaic.interop.idl.amqp.CompletionToken;
import mosaic.interop.idl.amqp.ConsumeOkMssg;
import mosaic.interop.idl.amqp.DeliveryMssg;
import mosaic.interop.idl.amqp.OperationNames;
import mosaic.interop.idl.amqp.OperationResponse;
import mosaic.interop.idl.amqp.Response;
import mosaic.interop.idl.amqp.ShutdownMssg;

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
		OperationResponse opResponse = new OperationResponse();
		MosaicLogger.getLogger().trace(
				"AmqpResponseTransmitter - Sending response for operation "
						+ op);
		try {
			opResponse.put(0, token);
			opResponse.put(1, convertOperationType(op));
			opResponse.put(2, isError);
			if (!isError) {
				if (result instanceof Boolean) {
					opResponse.put(3, (Boolean) result);
				} else {
					opResponse.put(3, (String) result);
				}
			} else {
				AmqpError error = new AmqpError();
				error.put(0, (String) result);
				opResponse.put(3, error);
			}
			Response response = new Response();
			response.put(0, opResponse);

			// send response
			message = SerDesUtils.serializeWithSchema(response);
			publishResponse(routingKey, message);
			MosaicLogger.getLogger().trace(
					"AmqpResponseTransmitter - Sent response for operation "
							+ op);
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		}

	}

	/**
	 * Builds the Cancel Ok message and sends it to the actual consumer.
	 * 
	 * @param callerId
	 *            the identifier of the consumer (connector)
	 * @param consumerTag
	 *            the tag of the consumer
	 */
	public void sendCancelOk(String callerId, String consumerTag) {
		byte[] message;

		CancelOkMssg cmessage = new CancelOkMssg();
		try {
			cmessage.put(0, consumerTag);
			Response response = new Response();
			response.put(0, cmessage);

			// send response
			message = SerDesUtils.serializeWithSchema(response);
			publishResponse(callerId, message);
			MosaicLogger.getLogger().trace(
					"AmqpResponseTransmitter - Sent CANCEL ok message");
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		}
	}

	/**
	 * Builds the Cancel message and sends it to the actual consumer.
	 * 
	 * @param callerId
	 *            the identifier of the consumer (connector)
	 * @param consumerTag
	 *            the tag of the consumer
	 */
	public void sendCancel(String callerId, String consumerTag) {
		byte[] message;

		CancelMssg cmessage = new CancelMssg();
		try {
			cmessage.put(0, consumerTag);
			Response response = new Response();
			response.put(0, cmessage);

			// send response
			message = SerDesUtils.serializeWithSchema(response);
			publishResponse(callerId, message);
			MosaicLogger.getLogger().trace(
					"AmqpResponseTransmitter - Sent CANCEL message");
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		}

	}

	/**
	 * Builds the Consume Ok message and sends it to the actual consumer.
	 * 
	 * @param callerId
	 *            the identifier of the consumer (connector)
	 * @param consumerTag
	 *            the tag of the consumer
	 */
	public void sendConsumeOk(String callerId, String consumerTag) {
		byte[] message;

		ConsumeOkMssg cmessage = new ConsumeOkMssg();
		try {
			cmessage.put(0, consumerTag);
			Response response = new Response();
			response.put(0, cmessage);

			// send response
			message = SerDesUtils.serializeWithSchema(response);
			publishResponse(callerId, message);
			MosaicLogger
					.getLogger()
					.trace("AmqpResponseTransmitter - Sent CONSUME Ok callback for consumer " + consumerTag //$NON-NLS-1$
							+ ".");
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		}
	}

	/**
	 * Delivers a message to its consumer
	 * 
	 * @param callerId
	 *            the identifier of the consumer (connector)
	 * @param message
	 *            the message contents and properties
	 */
	public void sendDelivery(String callerId, AmqpInboundMessage message) {
		byte[] mssg;

		DeliveryMssg dmessage = new DeliveryMssg();
		try {
			dmessage.put(0, message.getConsumer());
			dmessage.put(1, message.getDelivery());
			dmessage.put(2, message.getExchange());
			dmessage.put(3, message.getRoutingKey());
			// dmessage.put(4, message.getCallback());
			// dmessage.put(5, message.getContentEncoding());
			// dmessage.put(6, message.getContentType());
			// dmessage.put(7, message.getCorrelation());
			dmessage.put(4, message.isDurable() ? 2 : 1);
			// dmessage.put(9, message.getIdentifier());
			ByteBuffer buff = ByteBuffer.wrap(message.getData());
			dmessage.put(5, buff);
			Response response = new Response();
			response.put(0, dmessage);

			// send response
			mssg = SerDesUtils.serializeWithSchema(response);
			publishResponse(callerId, mssg);
			MosaicLogger.getLogger().trace(
					"AmqpResponseTransmitter - Delivered message");
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		}
	}

	/**
	 * Builds the Shutdown message and sends it to the actual consumer.
	 * 
	 * @param callerId
	 *            the identifier of the consumer (connector)
	 * @param consumerTag
	 *            the tag of the consumer
	 * @param errorMessage
	 *            a message about the shutdown cause
	 */
	public void sendShutdownSignal(String callerId, String consumerTag,
			String errorMessage) {
		byte[] message;

		ShutdownMssg smessage = new ShutdownMssg();
		try {
			smessage.put(0, consumerTag);
			smessage.put(1, errorMessage);
			Response response = new Response();
			response.put(0, smessage);

			// send response
			message = SerDesUtils.serializeWithSchema(response);
			MosaicLogger.getLogger().trace(
					"AmqpResponseTransmitter - Sent Shutdown message");
			publishResponse(callerId, message);
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
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
