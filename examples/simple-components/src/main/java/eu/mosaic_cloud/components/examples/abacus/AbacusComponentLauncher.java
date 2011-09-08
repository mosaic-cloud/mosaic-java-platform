
package eu.mosaic_cloud.components.examples.abacus;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.implementations.basic.MosBasicComponentLauncher;


public final class AbacusComponentLauncher
{
	private AbacusComponentLauncher ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length == 2), "invalid arguments: expected <ip> <mos-url>");
		MosBasicComponentLauncher.main (new String[] {AbacusComponentLauncher.class.getName ().replace ("Launcher", "Callbacks"), arguments[0], "29017", "29018", arguments[1]}, AbacusComponentLauncher.class.getClassLoader ());
	}
}
