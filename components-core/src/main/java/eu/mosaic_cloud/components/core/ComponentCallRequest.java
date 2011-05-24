
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallRequest
		extends Object
{
	public ComponentCallRequest (final Object metaData, final ByteBuffer data, final ComponentCallContinuation continuation)
	{
		super ();
		Preconditions.checkNotNull (metaData);
		Preconditions.checkNotNull (data);
		Preconditions.checkNotNull (continuation);
		this.metaData = metaData;
		this.data = data;
		this.continuation = continuation;
	}
	
	public ComponentCallContinuation continuation;
	public final ByteBuffer data;
	public final Object metaData;
}
