
package eu.mosaic_cloud.components.core;


import com.google.common.base.Preconditions;


public final class ComponentAcquireReply
		extends ComponentMessage
{
	private ComponentAcquireReply (final ComponentResourceDescriptor descriptor, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (descriptor);
		Preconditions.checkNotNull (reference);
		this.descriptor = descriptor;
		this.reference = reference;
	}
	
	public static final ComponentAcquireReply create (final ComponentResourceDescriptor descriptor, final ComponentCallReference reference)
	{
		return (new ComponentAcquireReply (descriptor, reference));
	}
	
	public final ComponentResourceDescriptor descriptor;
	public final ComponentCallReference reference;
}
