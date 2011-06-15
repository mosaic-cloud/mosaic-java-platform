
package eu.mosaic_cloud.components.jetty;


import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

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
		Preconditions.checkArgument (arguments != null);
		if (arguments.length != 1) {
			Preconditions.checkArgument (arguments.length == 2, "invalid arguments; expected: <component-identifier> <war-file>");
			JettyComponentContext.selfIdentifier = ComponentIdentifier.resolve (arguments[0]);
			JettyComponentContext.appWar = new File (arguments[1]);
			Preconditions.checkArgument (JettyComponentContext.appWar.isFile (), "invalid appWar file; (does not exist)");
			Preconditions.checkArgument (JettyComponentContext.appWar.canRead (), "invalid appWar file; (can not read)");
			BasicComponentHarnessPreMain.main (new String[] {JettyComponentPreMain.class.getName ().replace ("PreMain", "Callbacks")});
		} else {
			Preconditions.checkArgument (arguments.length == 1, "invalid arguments; expected: <war-file>");
			JettyComponentContext.selfIdentifier = ComponentIdentifier.resolve ("00000000" + UUID.randomUUID ().toString ().replace ("-", ""));
			JettyComponentContext.appWar = new File (arguments[0]);
			Preconditions.checkArgument (JettyComponentContext.appWar.isFile (), "invalid appWar file; (does not exist)");
			Preconditions.checkArgument (JettyComponentContext.appWar.canRead (), "invalid appWar file; (can not read)");
			final Class<?> mainClass = JettyComponentPreMain.class.getClassLoader ().loadClass ("eu.mosaic_cloud.jetty.connectors.httpg.ServerCommandLine");
			final Method mainMethod = mainClass.getMethod ("main", String[].class);
			mainMethod.invoke (null, new Object[] {new String[] {"--server", "127.0.0.1", "--port", "21688", "--auto-declare", "true", "--webapp", JettyComponentContext.appWar.getAbsolutePath ()}});
		}
	}
}
