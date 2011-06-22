package mosaic.connector.interop.queue.amqp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import mosaic.connector.interop.AbstractConnectorReactor;
import mosaic.connector.queue.amqp.AmqpCallbacksMap;
import mosaic.connector.queue.amqp.IAmqpConsumerCallback;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.queue.amqp.AmqpInboundMessage;
import mosaic.interop.idl.amqp.AmqpError;
import mosaic.interop.idl.amqp.CancelOkMssg;
import mosaic.interop.idl.amqp.CompletionToken;
import mosaic.interop.idl.amqp.ConsumeOkMssg;
import mosaic.interop.idl.amqp.DeliveryMssg;
import mosaic.interop.idl.amqp.OperationNames;
import mosaic.interop.idl.amqp.OperationResponse;
import mosaic.interop.idl.amqp.Response;
import mosaic.interop.idl.amqp.ShutdownMssg;

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

	private AmqpCallbacksMap callbacksMap;

	/**
	 * Creates the reactor for the AMQP connector proxy.
	 * 
	 * @param config
	 *            the configurations required to initialize the proxy
	 * @param bindingKey
	 *            queue binding key
	 * @throws Throwable
	 */
	public AmqpConnectorReactor(IConfiguration config, String bindingKey)
			throws Throwable {
		super(config, bindingKey, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME);
		this.callbacksMap = new AmqpCallbacksMap();
	}

	/**
	 * Maps the consume callback to the Consume request. When the response from
	 * the Consume request arrives this entry in the map will be replaced with
	 * another, mapping the consumerTag to the callback.
	 * 
	 * @param requestId
	 *            the request identifier
	 * @param callback
	 *            the callback
	 */
	protected synchronized void addCallback(String requestId,
			IAmqpConsumerCallback callback) {
		this.callbacksMap.addHandlers(requestId, callback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mosaic.connector.interop.AbstractConnectorReactor#processResponse(byte[])
	 */
	@SuppressWarnings("unchecked")
	protected void processResponse(byte[] message) throws IOException {
		Response enclosingResponse = new Response();
		enclosingResponse = SerDesUtils.deserializeWithSchema(message,
				enclosingResponse);
		Object resp = enclosingResponse.get(0);
		String consumerId;

		if (resp instanceof OperationResponse) {
			OperationResponse response = (OperationResponse) resp;

			CompletionToken token = (CompletionToken) response.get(0);
			OperationNames op = (OperationNames) response.get(1);
			boolean isError = (Boolean) response.get(2);
			String id = ((CharSequence) token.get(0)).toString();
			MosaicLogger.getLogger().trace(
					"AmqpConnectorReactor - Received response for op " + op
							+ " for request id " + id);
			List<IOperationCompletionHandler<?>> handlers = super
					.getDispatcher().removeRequestHandlers(id);
			if (handlers == null) {
				MosaicLogger.getLogger().error(
						"No handler found for request token: " + id);
				return;
			}

			if (isError) {
				AmqpError error = (AmqpError) response.get(3);
				for (IOperationCompletionHandler<?> handler : handlers) {
					handler.onFailure(new Exception(((CharSequence) error
							.get(0)).toString()));
				}
				return;
			}

			switch (op) {
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
				String resultStr = response.get(3).toString();
				for (IOperationCompletionHandler<?> handler : handlers) {
					((IOperationCompletionHandler<String>) handler)
							.onSuccess(resultStr);
				}
				MosaicLogger.getLogger().trace(
						"AmqpConnectorReactor - Received CONSUME "
								+ " for consumer " + resultStr);
				break;
			default:
				break;
			}
		} else if (resp instanceof CancelOkMssg) {
			CancelOkMssg cancel = (CancelOkMssg) resp;
			consumerId = cancel.get(0).toString();
			MosaicLogger.getLogger().trace(
					"AmqpConnectorReactor - Received CANCEL Ok "
							+ " for consumer " + consumerId);
			IAmqpConsumerCallback callback = this.callbacksMap
					.removeConsumerCallback(consumerId);
			callback.handleCancelOk(consumerId);
		} else if (resp instanceof ConsumeOkMssg) {
			ConsumeOkMssg consume = (ConsumeOkMssg) resp;
			consumerId = consume.get(0).toString();
			MosaicLogger.getLogger().trace(
					"AmqpConnectorReactor - Received CONSUME Ok "
							+ " for consumer " + consumerId);
			IAmqpConsumerCallback callback = this.callbacksMap
					.getRequestHandlers(consumerId);
			callback.handleConsumeOk(consumerId);
		} else if (resp instanceof ShutdownMssg) {
			ShutdownMssg shutdown = (ShutdownMssg) resp;
			consumerId = shutdown.get(0).toString();
			String signalMssg = shutdown.get(1).toString();
			IAmqpConsumerCallback callback = this.callbacksMap
					.getRequestHandlers(consumerId);
			callback.handleShutdownSignal(consumerId, signalMssg);
		} else if (resp instanceof DeliveryMssg) {
			DeliveryMssg delivery = (DeliveryMssg) resp;
			consumerId = delivery.get(0).toString();
			MosaicLogger.getLogger().trace(
					"AmqpConnectorReactor - Received delivery "
							+ " for consumer " + consumerId);
			long deliveryTag = (Long) delivery.get(1);
			String exchange = delivery.get(2).toString();
			String routingKey = delivery.get(3).toString();
			// String callback = delivery.get(4).toString();
			// String contentEncoding = delivery.get(5).toString();
			// String contentType = delivery.get(6).toString();
			// String correlation = delivery.get(7).toString();
			int deliveryMode = (Integer) delivery.get(4);
			// String identifier = delivery.get(9).toString();
			ByteBuffer buff = (ByteBuffer) delivery.get(5);
			AmqpInboundMessage mssg = new AmqpInboundMessage(consumerId,
					deliveryTag, exchange, routingKey, buff.array(),
					deliveryMode == 2);
			IAmqpConsumerCallback consCallback = this.callbacksMap
					.getRequestHandlers(consumerId);
			consCallback.handleDelivery(mssg);
		}
	}
}
