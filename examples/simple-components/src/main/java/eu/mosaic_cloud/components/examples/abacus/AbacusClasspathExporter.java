
package eu.mosaic_cloud.components.examples.abacus;


import eu.mosaic_cloud.tools.ClasspathExporter;


public final class AbacusClasspathExporter
{
	private AbacusClasspathExporter ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
	{
		ClasspathExporter.main (arguments, AbacusClasspathExporter.class.getClassLoader ());
	}
}
