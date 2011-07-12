package mosaic.interop.amqp;

import mosaic.interop.idl.DefaultPBPayloadCoder;
import mosaic.interop.idl.IdlCommon;
import mosaic.interop.idl.amqp.AmqpPayloads;

import com.google.protobuf.GeneratedMessage;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

public enum AmqpMessage implements MessageSpecification {
	ABORTED(MessageType.Termination, null), ACCESS(MessageType.Initiation, null), ERROR(
			MessageType.Exchange, IdlCommon.Error.class), OK(
			MessageType.Exchange, IdlCommon.Ok.class), NOK(
			MessageType.Exchange, IdlCommon.NotOk.class), DECL_EXCHANGE_REQUEST(
			MessageType.Exchange, AmqpPayloads.DeclareExchangeRequest.class), DECL_QUEUE_REQUEST(
			MessageType.Exchange, AmqpPayloads.DeclareQueueRequest.class), BIND_QUEUE_REQUEST(
			MessageType.Exchange, AmqpPayloads.BindQueueRequest.class), CONSUME_REQUEST(
			MessageType.Exchange, AmqpPayloads.ConsumeRequest.class), CONSUME_REPLY(
			MessageType.Exchange, AmqpPayloads.ConsumeReply.class), PUBLISH_REQUEST(
			MessageType.Exchange, AmqpPayloads.PublishRequest.class), GET_REQUEST(
			MessageType.Exchange, AmqpPayloads.GetRequest.class), CANCEL_REQUEST(
			MessageType.Exchange, AmqpPayloads.CancelRequest.class), SERVER_CANCEL(
			MessageType.Exchange, AmqpPayloads.ServerCancelRequest.class), ACK(
			MessageType.Exchange, AmqpPayloads.Ack.class), DELIVERY(
			MessageType.Exchange, AmqpPayloads.DeliveryMessage.class), CONSUME_OK(
			MessageType.Exchange, AmqpPayloads.ConsumeOkMessage.class), CANCEL_OK(
			MessageType.Exchange, AmqpPayloads.CancelOkMessage.class), SHUTDOWN(
			MessageType.Exchange, AmqpPayloads.ShutdownMessage.class);

	public final PayloadCoder coder;
	public final String identifier;
	public final MessageType type;

	AmqpMessage(MessageType type, Class<? extends GeneratedMessage> clasz) {
		this.identifier = Identifiers.generate(this);
		this.type = type;
		if (clasz != null) {
			this.coder = new DefaultPBPayloadCoder(clasz, false);
		} else {
			this.coder = null;
		}
	}

	@Override
	public String getIdentifier() {
		return (this.identifier);
	}

	@Override
	public PayloadCoder getPayloadCoder() {
		return (this.coder);
	}

	@Override
	public MessageType getType() {
		return (this.type);
	}

}
