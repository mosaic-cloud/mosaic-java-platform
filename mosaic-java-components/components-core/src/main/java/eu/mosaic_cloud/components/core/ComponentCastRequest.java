
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCastRequest
		extends ComponentMessage
{
	private ComponentCastRequest (final String operation, final Object inputs, final ByteBuffer data)
	{
		super ();
		Preconditions.checkNotNull (operation);
		Preconditions.checkNotNull (data);
		this.operation = operation;
		this.inputs = inputs;
		this.data = data;
	}
	
	public final ByteBuffer data;
	public final Object inputs;
	public final String operation;
	
	public static final ComponentCastRequest create (final String operation, final Object inputs)
	{
		return (new ComponentCastRequest (operation, inputs, ByteBuffer.allocate (0)));
	}
	
	public static final ComponentCastRequest create (final String operation, final Object inputs, final ByteBuffer data)
	{
		return (new ComponentCastRequest (operation, inputs, data));
	}
}
