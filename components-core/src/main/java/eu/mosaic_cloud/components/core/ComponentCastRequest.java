
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCastRequest
		extends ComponentMessage
{
	private ComponentCastRequest (final Object metaData, final ByteBuffer data)
	{
		super ();
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		this.metaData = metaData;
		this.data = data;
	}
	
	public final ByteBuffer data;
	public final Object metaData;
	
	public static final ComponentCastRequest create (final Object metaData, final ByteBuffer data)
	{
		return (new ComponentCastRequest (metaData, data));
	}
}
