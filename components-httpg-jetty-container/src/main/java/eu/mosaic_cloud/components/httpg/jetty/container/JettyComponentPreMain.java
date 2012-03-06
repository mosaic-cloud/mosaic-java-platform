/*
 * #%L
 * mosaic-components-httpg-jetty-container
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.components.httpg.jetty.container;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

import com.google.common.base.Preconditions;


public final class JettyComponentPreMain
		extends Object
{
	private JettyComponentPreMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		Preconditions.checkArgument (arguments != null);
		if ((arguments.length == 2) && !"00000000cf14614e8810102fa887b6bc90dc2a40".equals (arguments[0])) {
			Preconditions.checkArgument (arguments.length == 2, "invalid arguments; expected: <component-identifier> <war-file>");
			JettyComponentContext.selfIdentifier = ComponentIdentifier.resolve (arguments[0]);
			JettyComponentContext.appWar = new File (arguments[1]);
			Preconditions.checkArgument (JettyComponentContext.appWar.isFile (), "invalid appWar file; (does not exist)");
			Preconditions.checkArgument (JettyComponentContext.appWar.canRead (), "invalid appWar file; (can not read)");
			BasicComponentHarnessPreMain.main (new String[] {JettyComponentPreMain.class.getName ().replace ("PreMain", "Callbacks")});
		} else if ((arguments.length == 1) || "00000000cf14614e8810102fa887b6bc90dc2a40".equals (arguments[0])) {
			final String temporary;
			if (arguments.length == 2) {
				JettyComponentContext.selfIdentifier = ComponentIdentifier.resolve (arguments[0]);
				JettyComponentContext.appWar = new File (arguments[1]);
				temporary = "./temporary";
			} else {
				JettyComponentContext.selfIdentifier = ComponentIdentifier.resolve ("ffffffff" + UUID.randomUUID ().toString ().replace ("-", ""));
				JettyComponentContext.appWar = new File (arguments[0]);
				temporary = System.getProperty ("java.io.tmpdir") + "/jetty-" + JettyComponentContext.selfIdentifier.string;
			}
			Preconditions.checkArgument (JettyComponentContext.appWar.isFile (), "invalid appWar file; (does not exist)");
			Preconditions.checkArgument (JettyComponentContext.appWar.canRead (), "invalid appWar file; (can not read)");
			final Class<?> mainClass = JettyComponentPreMain.class.getClassLoader ().loadClass ("eu.mosaic_cloud.components.httpg.jetty.connector.ServerCommandLine");
			final Method mainMethod = mainClass.getMethod ("main", String[].class);
			try {
				mainMethod.invoke (null, new Object[] {new String[] {"--server", "127.0.0.1", "--port", "21688", "--auto-declare", "true", "--webapp", JettyComponentContext.appWar.getAbsolutePath (), "--tmp", temporary}});
			} catch (final InvocationTargetException exception) {
				throw (exception.getCause ());
			}
		} else
			throw (new IllegalArgumentException ("invalid arguments; expected: <component-identifier> <war-file>"));
	}
}
