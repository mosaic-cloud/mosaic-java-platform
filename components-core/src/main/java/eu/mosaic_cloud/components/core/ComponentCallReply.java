
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallReply
		extends ComponentMessage
{
	private ComponentCallReply (final Object metaData, final ByteBuffer data, final ComponentCallReference reference)
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
	
	public static final ComponentCallReply create (final Object metaData, final ByteBuffer data, final ComponentCallReference reference)
	{
		return (new ComponentCallReply (metaData, data, reference));
	}
}
