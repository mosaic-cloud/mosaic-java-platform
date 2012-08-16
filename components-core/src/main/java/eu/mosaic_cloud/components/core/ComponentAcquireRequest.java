
package eu.mosaic_cloud.components.core;


import com.google.common.base.Preconditions;


public final class ComponentAcquireRequest
		extends ComponentMessage
{
	private ComponentAcquireRequest (final ComponentResourceSpecification specification, final ComponentCallReference reference)
	{
		super ();
		Preconditions.checkNotNull (specification);
		Preconditions.checkNotNull (reference);
		this.specification = specification;
		this.reference = reference;
	}
	
	public static final ComponentAcquireRequest create (final ComponentResourceSpecification specification, final ComponentCallReference reference)
	{
		return (new ComponentAcquireRequest (specification, reference));
	}
	
	public final ComponentCallReference reference;
	public final ComponentResourceSpecification specification;
}
