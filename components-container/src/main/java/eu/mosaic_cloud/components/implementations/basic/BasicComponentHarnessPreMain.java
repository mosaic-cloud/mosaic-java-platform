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
import java.util.ArrayList;
import java.util.Arrays;

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
	
	public static final void main (final String callbacksClass, final String[] arguments, final int argumentsOffset)
			throws Throwable
	{
		Preconditions.checkArgument (arguments != null, "invalid arguments; expected arguments");
		Preconditions.checkArgument ((argumentsOffset >= 0) && (argumentsOffset <= arguments.length), "invalid arguments offset");
		final String[] finalArguments = new String[(arguments.length + 2) - argumentsOffset];
		finalArguments[0] = "--component-callbacks-class";
		finalArguments[1] = callbacksClass;
		System.arraycopy (arguments, argumentsOffset, finalArguments, 2, arguments.length - argumentsOffset);
		BasicComponentHarnessPreMain.main (finalArguments);
	}
	
	public static final void main (final String callbacksClass, final String[] prefixArguments, final String[] suffixArguments, final String[] arguments, final int argumentsOffset)
			throws Throwable
	{
		Preconditions.checkNotNull (prefixArguments);
		Preconditions.checkNotNull (suffixArguments);
		Preconditions.checkArgument (arguments != null, "invalid arguments; expected arguments");
		Preconditions.checkArgument ((argumentsOffset >= 0) && (argumentsOffset <= arguments.length), "invalid arguments offset");
		final String[] argumentsTrimmed = new String[arguments.length - argumentsOffset];
		System.arraycopy (arguments, argumentsOffset, argumentsTrimmed, 0, arguments.length - argumentsOffset);
		final ArrayList<String> finalArguments = new ArrayList<String> ();
		finalArguments.add ("--component-callbacks-class");
		finalArguments.add (callbacksClass);
		finalArguments.addAll (Arrays.asList (prefixArguments));
		finalArguments.addAll (Arrays.asList (argumentsTrimmed));
		finalArguments.addAll (Arrays.asList (suffixArguments));
		BasicComponentHarnessPreMain.main (finalArguments.toArray (new String[0]));
	}
	
	public static final void main (final String[] arguments)
			throws Throwable
	{
		final Class<?> mainClass = BasicComponentHarnessPreMain.class.getClassLoader ().loadClass (BasicComponentHarnessPreMain.class.getName ().replace ("PreMain", "Main"));
		final Method mainMethod = mainClass.getMethod ("main", String[].class);
		try {
			mainMethod.invoke (null, new Object[] {arguments});
		} catch (final InvocationTargetException wrapper) {
			throw (wrapper.getCause ());
		}
	}
	
	static final InputStream stdin;
	static final OutputStream stdout;
}
