
package eu.mosaic_cloud.components.core;


public final class ComponentTcpSocketResourceSpecification
		extends ComponentResourceSpecification
{
	private ComponentTcpSocketResourceSpecification (final String identifier)
	{
		super (identifier);
	}
	
	public static final ComponentTcpSocketResourceSpecification create (final String identifier)
	{
		return (new ComponentTcpSocketResourceSpecification (identifier));
	}
}
