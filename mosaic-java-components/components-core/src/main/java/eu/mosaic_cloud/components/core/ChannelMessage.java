
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;
import java.util.Map;

import com.google.common.base.Preconditions;


public final class ChannelMessage
		extends Object
{
	private ChannelMessage (final ChannelMessageType type, final Map<String, Object> metaData, final ByteBuffer data)
	{
		super ();
		Preconditions.checkNotNull (type);
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		this.type = type;
		this.metaData = metaData;
		this.data = data;
	}
	
	public final ByteBuffer data;
	public final Map<String, Object> metaData;
	public final ChannelMessageType type;
	
	public static final ChannelMessage create (final ChannelMessageType type, final Map<String, Object> metaData, final ByteBuffer data)
	{
		return (new ChannelMessage (type, metaData, data));
	}
}
