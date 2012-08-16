
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
		this.ok = true;
		this.descriptor = descriptor;
		this.reference = reference;
		this.error = null;
	}
	
	private ComponentAcquireReply (final Object error, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (error);
		Preconditions.checkNotNull (reference);
		this.ok = false;
		this.error = error;
		this.reference = reference;
		this.descriptor = null;
	}
	
	public static final ComponentAcquireReply create (final ComponentResourceDescriptor descriptor, final ComponentCallReference reference)
	{
		return (new ComponentAcquireReply (descriptor, reference));
	}
	
	public static final ComponentAcquireReply create (final Object error, final ComponentCallReference reference)
	{
		return (new ComponentAcquireReply (error, reference));
	}
	
	public final ComponentResourceDescriptor descriptor;
	public final Object error;
	public final boolean ok;
	public final ComponentCallReference reference;
}
