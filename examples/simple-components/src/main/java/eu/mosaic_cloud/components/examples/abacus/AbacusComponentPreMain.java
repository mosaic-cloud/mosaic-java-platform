
package eu.mosaic_cloud.components.examples.abacus;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;


public final class AbacusComponentPreMain
		extends Object
{
	private AbacusComponentPreMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		Preconditions.checkArgument (arguments != null);
		Preconditions.checkArgument (arguments.length == 1, "invalid arguments; expected: <component-identifier>");
		ComponentIdentifier.resolve (arguments[0]);
		BasicComponentHarnessPreMain.main (new String[] {AbacusComponentPreMain.class.getName ().replace ("PreMain", "Callbacks")});
	}
}
