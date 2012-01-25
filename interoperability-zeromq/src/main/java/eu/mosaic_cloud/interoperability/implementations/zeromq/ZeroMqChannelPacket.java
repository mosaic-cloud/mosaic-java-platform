
package eu.mosaic_cloud.interoperability.implementations.zeromq;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ZeroMqChannelPacket
		extends Object
{
	ZeroMqChannelPacket (final String peer, final ByteBuffer header, final ByteBuffer payload)
	{
		super ();
		Preconditions.checkNotNull (peer);
		Preconditions.checkNotNull (header);
		this.peer = peer;
		this.header = header;
		this.payload = payload;
	}
	
	public final ByteBuffer header;
	public final ByteBuffer payload;
	public final String peer;
	
	public static final ZeroMqChannelPacket create (final String peer, final ByteBuffer header, final ByteBuffer payload)
	{
		return (new ZeroMqChannelPacket (peer, header, payload));
	}
}
