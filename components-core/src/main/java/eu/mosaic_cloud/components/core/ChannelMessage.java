
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;


public final class ChannelMessage
		extends Object
{
	public ChannelMessage (final ChannelMessageType type, final Object metaData, final ByteBuffer data)
	{
		super ();
		Preconditions.checkNotNull (type);
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		this.type = type;
		this.metaData = metaData;
		this.data = data;
	}
	
	@Override
	public final boolean equals (final Object object)
	{
		if (object == null)
			return (false);
		if (this == object)
			return (true);
		if (!(object instanceof ChannelMessage))
			return (false);
		final ChannelMessage other = (ChannelMessage) object;
		if (this.type != other.type)
			return (false);
		if (!Objects.equal (this.metaData, other.metaData))
			return (false);
		if (!Objects.equal (this.data, other.data))
			return (false);
		return (true);
	}
	
	@Override
	public final int hashCode ()
	{
		return (Objects.hashCode (this.type, this.metaData, this.data));
	}
	
	public final ByteBuffer data;
	public final Object metaData;
	public final ChannelMessageType type;
}
