
package eu.mosaic_cloud.interoperability.core;


import com.google.common.base.Preconditions;


public final class Message
{
	public Message (final MessageSpecification specification)
	{
		this (specification, null);
	}
	
	public Message (final MessageSpecification specification, final Object payload)
	{
		super ();
		Preconditions.checkNotNull (specification);
		this.specification = specification;
		this.payload = payload;
	}
	
	public final Object payload;
	public final MessageSpecification specification;
}
