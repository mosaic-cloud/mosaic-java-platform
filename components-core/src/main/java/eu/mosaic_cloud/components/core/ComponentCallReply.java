
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallReply
		extends Object
{
	public ComponentCallReply (final Object metaData, final ByteBuffer data)
	{
		super ();
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		this.metaData = metaData;
		this.data = data;
	}
	
	public final ByteBuffer data;
	public final Object metaData;
}
