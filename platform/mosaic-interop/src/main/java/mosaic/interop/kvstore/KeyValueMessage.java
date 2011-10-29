package mosaic.interop.kvstore;

import mosaic.interop.idl.DefaultPBPayloadCoder;
import mosaic.interop.idl.IdlCommon;
import mosaic.interop.idl.kvstore.KeyValuePayloads;

import com.google.protobuf.GeneratedMessage;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

/**
 * Enum containing all possible AMQP connector-driver messages.
 * 
 * @author Georgiana Macariu
 * 
 */
public enum KeyValueMessage implements MessageSpecification {
	ABORTED(MessageType.Termination, IdlCommon.AbortRequest.class), ACCESS(
			MessageType.Initiation, KeyValuePayloads.InitRequest.class), ERROR(
			MessageType.Exchange, IdlCommon.Error.class), OK(
			MessageType.Exchange, IdlCommon.Ok.class), NOK(
			MessageType.Exchange, IdlCommon.NotOk.class), GET_REQUEST(
			MessageType.Exchange, KeyValuePayloads.GetRequest.class), GET_REPLY(
			MessageType.Exchange, KeyValuePayloads.GetReply.class), SET_REQUEST(
			MessageType.Exchange, KeyValuePayloads.SetRequest.class), DELETE_REQUEST(
			MessageType.Exchange, KeyValuePayloads.DeleteRequest.class), LIST_REQUEST(
			MessageType.Exchange, KeyValuePayloads.ListRequest.class), LIST_REPLY(
			MessageType.Exchange, KeyValuePayloads.ListReply.class);

	public PayloadCoder coder = null;
	public final String identifier;
	public final MessageType type;

	/**
	 * Creates a new Key-Value message.
	 * 
	 * @param type
	 *            the type of the message (initiation, exchange or termination)
	 * @param clasz
	 *            the class containing the payload of the message
	 */
	KeyValueMessage(MessageType type, Class<? extends GeneratedMessage> clasz) {
		this.identifier = Identifiers.generate(this);
		this.type = type;
		if (clasz != null) {
			// this.coder = new DefaultJavaSerializationPayloadCoder(clasz,
			// false);
			this.coder = new DefaultPBPayloadCoder(clasz, false);
		}
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public PayloadCoder getPayloadCoder() {
		return this.coder;
	}

	@Override
	public MessageType getType() {
		return this.type;
	}

	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
}
