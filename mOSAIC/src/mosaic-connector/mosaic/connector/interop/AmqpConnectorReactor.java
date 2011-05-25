package mosaic.connector.interop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import mosaic.connector.queue.AmqpCallbacksMap;
import mosaic.connector.queue.AmqpInboundMessage;
import mosaic.connector.queue.IAmqpConsumerCallback;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.utils.SerDesUtils;
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
	 */
	public AmqpConnectorReactor(IConfiguration config, String bindingKey) {
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
	protected void addCallback(String requestId, IAmqpConsumerCallback callback) {
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
				IAmqpConsumerCallback callback = this.callbacksMap
						.removeConsumerCallback(id);
				this.callbacksMap.addHandlers(resultStr, callback);
				break;
			default:
				break;
			}
		} else if (resp instanceof CancelOkMssg) {
			CancelOkMssg cancel = (CancelOkMssg) resp;
			consumerId = (String) cancel.get(0);
			IAmqpConsumerCallback callback = this.callbacksMap
					.getRequestHandlers(consumerId);
			callback.handleCancelOk(consumerId);
		} else if (resp instanceof ConsumeOkMssg) {
			ConsumeOkMssg consume = (ConsumeOkMssg) resp;
			consumerId = (String) consume.get(0);
			IAmqpConsumerCallback callback = this.callbacksMap
					.getRequestHandlers(consumerId);
			callback.handleConsumeOk(consumerId);
		} else if (resp instanceof ShutdownMssg) {
			ShutdownMssg shutdown = (ShutdownMssg) resp;
			consumerId = (String) shutdown.get(0);
			String signalMssg = (String) shutdown.get(1);
			IAmqpConsumerCallback callback = this.callbacksMap
					.getRequestHandlers(consumerId);
			callback.handleShutdownSignal(consumerId, signalMssg);
		} else if (resp instanceof DeliveryMssg) {
			DeliveryMssg delivery = (DeliveryMssg) resp;
			consumerId = (String) delivery.get(0);
			long deliveryTag = (Long) delivery.get(1);
			String exchange = (String) delivery.get(2);
			String routingKey = (String) delivery.get(3);
			String callback = (String) delivery.get(4);
			String contentEncoding = (String) delivery.get(5);
			String contentType = (String) delivery.get(6);
			String correlation = (String) delivery.get(7);
			int deliveryMode = (Integer) delivery.get(8);
			String identifier = (String) delivery.get(9);
			ByteBuffer buff = (ByteBuffer) delivery.get(10);
			AmqpInboundMessage mssg = new AmqpInboundMessage(consumerId,
					deliveryTag, exchange, routingKey, buff.array(),
					deliveryMode == 2, callback, contentEncoding, contentType,
					correlation, identifier);
			IAmqpConsumerCallback consCallback = this.callbacksMap
					.getRequestHandlers(consumerId);
			consCallback.handleDelivery(mssg);
		}
	}
}
