
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallReply
		extends ComponentMessage
{
	private ComponentCallReply (final boolean ok, final Object outputsOrError, final ByteBuffer data, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (data);
		Preconditions.checkNotNull (reference);
		this.ok = ok;
		this.outputsOrError = outputsOrError;
		this.data = data;
		this.reference = reference;
	}
	
	public final ByteBuffer data;
	public final boolean ok;
	public final Object outputsOrError;
	public final ComponentCallReference reference;
	
	public static final ComponentCallReply create (final boolean ok, final Object outputsOrError, final ByteBuffer data, final ComponentCallReference reference)
	{
		return (new ComponentCallReply (ok, outputsOrError, data, reference));
	}
}
