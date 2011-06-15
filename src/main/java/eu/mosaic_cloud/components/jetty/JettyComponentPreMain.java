
package eu.mosaic_cloud.components.jetty;


import java.io.File;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;


public final class JettyComponentPreMain
		extends Object
{
	private JettyComponentPreMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length == 2), "invalid arguments; expected: <component-identifier> <war-file>");
		JettyComponentContext.selfIdentifier = ComponentIdentifier.resolve (arguments[0]);
		JettyComponentContext.appWar = new File (arguments[1]);
		Preconditions.checkArgument (JettyComponentContext.appWar.isFile (), "invalid appWar file; (does not exist)");
		Preconditions.checkArgument (JettyComponentContext.appWar.canRead (), "invalid appWar file; (can not read)");
		BasicComponentHarnessPreMain.main (new String[] {JettyComponentPreMain.class.getName ().replace ("PreMain", "Callbacks")});
	}
}
