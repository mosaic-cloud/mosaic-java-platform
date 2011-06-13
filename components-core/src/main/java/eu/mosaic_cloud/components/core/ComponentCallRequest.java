
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;


public final class ComponentCallRequest
		extends ComponentMessage
{
	private ComponentCallRequest (final String operation, final Object inputs, final ByteBuffer data, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (operation);
		Preconditions.checkNotNull (data);
		Preconditions.checkNotNull (reference);
		this.operation = operation;
		this.inputs = inputs;
		this.data = data;
		this.reference = reference;
	}
	
	public final ByteBuffer data;
	public final Object inputs;
	public final String operation;
	public final ComponentCallReference reference;
	
	public static final ComponentCallRequest create (final String operation, final Object inputs, final ByteBuffer data, final ComponentCallReference reference)
	{
		return (new ComponentCallRequest (operation, inputs, data, reference));
	}
}
