/*
 * #%L
 * mosaic-components-container
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

package eu.mosaic_cloud.components.implementations.basic;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public final class BasicComponentHarnessPreMain
		extends Object
{
	private BasicComponentHarnessPreMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	static {
		stdin = System.in;
		stdout = System.out;
		System.setIn (new ByteArrayInputStream (new byte[0]));
		System.setOut (System.err);
	}
	
	public static final void main (final String[] arguments)
			throws ClassNotFoundException,
				SecurityException,
				NoSuchMethodException,
				IllegalArgumentException,
				IllegalAccessException,
				InvocationTargetException
	{
		final Class<?> mainClass = BasicComponentHarnessPreMain.class.getClassLoader ().loadClass (BasicComponentHarnessPreMain.class.getName ().replace ("PreMain", "Main"));
		final Method mainMethod = mainClass.getMethod ("main", String[].class);
		mainMethod.invoke (null, new Object[] {arguments});
	}
	
	static final InputStream stdin;
	static final OutputStream stdout;
}
