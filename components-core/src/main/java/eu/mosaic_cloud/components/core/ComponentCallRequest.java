
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallRequest
		extends ComponentMessage
{
	private ComponentCallRequest (final Object metaData, final ByteBuffer data, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		Preconditions.checkNotNull (reference);
		this.metaData = metaData;
		this.data = data;
		this.reference = reference;
	}
	
	public final ByteBuffer data;
	public final Object metaData;
	public final ComponentCallReference reference;
	
	public static final ComponentCallRequest create (final Object metaData, final ByteBuffer data, final ComponentCallReference reference)
	{
		return (new ComponentCallRequest (metaData, data, reference));
	}
}
