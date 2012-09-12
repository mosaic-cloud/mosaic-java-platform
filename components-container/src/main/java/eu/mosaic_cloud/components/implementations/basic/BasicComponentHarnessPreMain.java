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
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import eu.mosaic_cloud.tools.miscellaneous.BrokenInputStream;
import eu.mosaic_cloud.tools.miscellaneous.BrokenPrintStream;

import com.google.common.base.Preconditions;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;


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
	
	public static final void main (final String callbacksClass, final String callbacksConfiguration, final String[] arguments)
			throws Throwable
	{
		BasicComponentHarnessPreMain.main (callbacksClass, callbacksConfiguration, arguments, 0);
	}
	
	public static final void main (final String callbacksClass, final String callbacksConfiguration, final String[] arguments, final int argumentsOffset)
			throws Throwable
	{
		Preconditions.checkArgument (arguments != null, "invalid arguments; expected arguments");
		Preconditions.checkArgument ((argumentsOffset >= 0) && (argumentsOffset <= arguments.length), "invalid arguments offset");
		final String[] finalArguments = new String[(arguments.length + 4) - argumentsOffset];
		finalArguments[0] = "--component-callbacks-class";
		finalArguments[1] = callbacksClass;
		finalArguments[2] = "--component-callbacks-configuration";
		finalArguments[3] = callbacksConfiguration;
		System.arraycopy (arguments, argumentsOffset, finalArguments, 4, arguments.length - argumentsOffset);
		BasicComponentHarnessPreMain.main (finalArguments);
	}
	
	public static final void main (final String callbacksClass, final String[] arguments)
			throws Throwable
	{
		BasicComponentHarnessPreMain.main (callbacksClass, arguments, 0);
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
	
	public static final void main (final String callbacksClass, final String[] prefixArguments, final String[] suffixArguments, final String[] arguments)
			throws Throwable
	{
		BasicComponentHarnessPreMain.main (callbacksClass, prefixArguments, suffixArguments, arguments, 0);
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
		Preconditions.checkNotNull (arguments);
		if (arguments.length > 0) {
			boolean launch = false;
			for (final String argument : arguments)
				if ("--launch".equals (argument)) {
					launch = true;
					break;
				}
			if (launch) {
				BasicComponentHarnessPreMain.mainLocal (arguments);
				return;
			}
		}
		final Class<?> mainClass = ClassLoader.getSystemClassLoader ().loadClass (BasicComponentHarnessPreMain.class.getName ().replace ("PreMain", "Main"));
		final Method mainMethod = mainClass.getMethod ("main", String[].class);
		try {
			mainMethod.invoke (null, (Object) arguments);
		} catch (final InvocationTargetException wrapper) {
			throw (wrapper.getCause ());
		}
	}
	
	private static final void mainLocal (final String[] argumentsList)
			throws Throwable
	{
		final ArgumentsProvider arguments = CliFactory.parseArguments (ArgumentsProvider.class, argumentsList);
		final Class<?> mainClass = ClassLoader.getSystemClassLoader ().loadClass (BasicComponentHarnessPreMain.class.getName ().replace ("HarnessPreMain", "LocalLauncher"));
		final String componentCallbacks = arguments.getCallbacksClass ();
		final List<String> componentConfiguration = (arguments.getCallbacksOptions () != null) ? arguments.getCallbacksOptions () : new LinkedList<String> ();
		final URL controllerBaseUrl = new URL (arguments.getControllerUrl ());
		final InetSocketAddress channelAddress = new InetSocketAddress (arguments.getLocalAddress (), arguments.getLocalPort ());
		final Method mainMethod = mainClass.getMethod ("launch", URL.class, InetSocketAddress.class, String.class, List.class);
		try {
			mainMethod.invoke (null, controllerBaseUrl, channelAddress, componentCallbacks, componentConfiguration);
		} catch (final InvocationTargetException wrapper) {
			throw (wrapper.getCause ());
		}
	}
	
	static final InputStream stdin;
	static final OutputStream stdout;
	
	public static interface ArgumentsProvider
	{
		@Option (longName = "component-callbacks-class", exactly = 1, defaultToNull = false)
		public abstract String getCallbacksClass ();
		
		@Option (longName = "component-callbacks-configuration", minimum = 0, maximum = Integer.MAX_VALUE, defaultToNull = true)
		public abstract List<String> getCallbacksOptions ();
		
		@Option (longName = "controller-url", exactly = 1, defaultToNull = false)
		public abstract String getControllerUrl ();
		
		@Option (longName = "launch", exactly = 1, defaultToNull = false)
		public abstract boolean getLaunch ();
		
		@Option (longName = "local-address", exactly = 1, defaultToNull = false)
		public abstract String getLocalAddress ();
		
		@Option (longName = "local-port", exactly = 1, defaultToNull = false)
		public abstract int getLocalPort ();
	}
}
