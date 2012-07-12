package eu.mosaic_cloud.platform.interop.specs.dfs;

import com.google.protobuf.GeneratedMessage;

import eu.mosaic_cloud.interoperability.core.MessageSpecification;
import eu.mosaic_cloud.interoperability.core.MessageType;
import eu.mosaic_cloud.interoperability.core.PayloadCoder;
import eu.mosaic_cloud.interoperability.tools.Identifiers;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.dfs.DFSPayloads;
import eu.mosaic_cloud.platform.interop.tools.DefaultPBPayloadCoder;

public enum DFSHandlerMessage 
implements
MessageSpecification
{
	SUCCESS (MessageType.Exchange, DFSPayloads.SuccessResponse.class),
	BYTES (MessageType.Exchange, DFSPayloads.FileRead.class),
	OK (MessageType.Exchange, IdlCommon.Ok.class),
	CLOSE (MessageType.Exchange, DFSPayloads.CloseFile.class),
	READ (MessageType.Exchange, DFSPayloads.ReadFile.class),
	WRITE (MessageType.Exchange, DFSPayloads.WriteFile.class),
	SEEK (MessageType.Exchange, DFSPayloads.SeekFile.class),
	FLUSH (MessageType.Exchange, DFSPayloads.FlushFile.class);

	public PayloadCoder coder = null;
	public final String identifier;
	public final MessageType type;
	
	DFSHandlerMessage (final MessageType type, final Class<? extends GeneratedMessage> clasz)
	{
		this.identifier = Identifiers.generate (this);
		this.type = type;
		if (clasz != null) {
			this.coder = new DefaultPBPayloadCoder (clasz, false);
		}
	}
	
	@Override
	public String getIdentifier ()
	{
		return this.identifier;
	}
	
	@Override
	public PayloadCoder getPayloadCoder ()
	{
		return this.coder;
	}
	
	@Override
	public String getQualifiedName ()
	{
		return (Identifiers.generateName (this));
	}
	
	@Override
	public MessageType getType ()
	{
		return this.type;
	}

}
