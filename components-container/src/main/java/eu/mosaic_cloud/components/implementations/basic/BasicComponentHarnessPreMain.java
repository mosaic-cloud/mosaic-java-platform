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


import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import eu.mosaic_cloud.tools.miscellaneous.BrokenInputStream;
import eu.mosaic_cloud.tools.miscellaneous.BrokenPrintStream;

import com.google.common.base.Preconditions;


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
		System.setIn (BrokenInputStream.create ());
		System.setOut (BrokenPrintStream.create ());
	}
	
	public static final void main (final String callbacksClass, final String[] arguments)
			throws Throwable
	{
		final Class<?> mainClass = BasicComponentHarnessPreMain.class.getClassLoader ().loadClass (BasicComponentHarnessPreMain.class.getName ().replace ("PreMain", "Main"));
		final Method mainMethod = mainClass.getMethod ("main", String.class, String[].class);
		try {
			mainMethod.invoke (null, new Object[] {callbacksClass, arguments});
		} catch (final InvocationTargetException exception) {
			throw (exception.getCause ());
		}
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length > 0) && (arguments[0] != null), "invalid arguments; expected: `<component-callbacks-class> ...`");
		final String[] finalArguments = new String[arguments.length - 1];
		System.arraycopy (arguments, 1, finalArguments, 0, arguments.length - 1);
		BasicComponentHarnessPreMain.main (arguments[0], finalArguments);
	}
	
	static final InputStream stdin;
	static final OutputStream stdout;
}
