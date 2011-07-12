package mosaic.interop.kvstore;

import mosaic.interop.idl.DefaultPBPayloadCoder;
import mosaic.interop.idl.kvstore.MemcachedPayloads;

import com.google.protobuf.GeneratedMessage;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;

public enum MemcachedMessage implements MessageSpecification {

	APPEND_REQUEST(MessageType.Exchange, MemcachedPayloads.AppendRequest.class), PREPEND_REQUEST(
			MessageType.Exchange, MemcachedPayloads.PrependRequest.class), ADD_REQUEST(
			MessageType.Exchange, MemcachedPayloads.AddRequest.class), REPLACE_REQUEST(
			MessageType.Exchange, MemcachedPayloads.ReplaceRequest.class), CAS_REQUEST(
			MessageType.Exchange, MemcachedPayloads.CasRequest.class);

	public final PayloadCoder coder;
	public final String identifier;
	public final MessageType type;

	MemcachedMessage(MessageType type, Class<? extends GeneratedMessage> clasz) {
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
