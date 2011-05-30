
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;
import java.util.Map;

import com.google.common.base.Preconditions;


public final class ComponentCastRequest
		extends ComponentMessage
{
	private ComponentCastRequest (final Map<String, ? extends Object> metaData, final ByteBuffer data)
	{
		super ();
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		this.metaData = metaData;
		this.data = data;
	}
	
	public final ByteBuffer data;
	public final Map<String, ? extends Object> metaData;
	
	public static final ComponentCastRequest create (final Map<String, ? extends Object> metaData, final ByteBuffer data)
	{
		return (new ComponentCastRequest (metaData, data));
	}
}
