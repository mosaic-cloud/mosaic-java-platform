/*
 * #%L
 * mosaic-cloudlet
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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

package eu.mosaic_cloud.cloudlet;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.implementations.basic.MosBasicComponentLauncher;


/**
 * Launches a cloudlet.
 * 
 * @author Georgiana Macariu
 * 
 */
public class CloudletContainerLauncher
{
	private CloudletContainerLauncher ()
	{
		super ();
		throw new UnsupportedOperationException ();
	}
	
	public static void main (final String descriptor, final String[] arguments)
			throws Throwable
	{
		Preconditions.checkNotNull (descriptor);
		Preconditions.checkArgument (arguments != null);
		CloudletContainerPreMain.CloudletContainerParameters.classpath = null;
		CloudletContainerPreMain.CloudletContainerParameters.configFile = descriptor;
		final String[] finalArguments = new String[arguments.length + 1];
		finalArguments[0] = "eu.mosaic_cloud.cloudlet.runtime.ContainerComponentCallbacks";
		System.arraycopy (arguments, 0, finalArguments, 1, arguments.length);
		MosBasicComponentLauncher.main (finalArguments, null);
	}
	
	public static void main (final String[] arguments)
			throws Throwable
	{
		Preconditions.checkArgument ((arguments != null) && (arguments.length >= 1), "invalid arguments: expected `<descriptor> ...`");
		CloudletContainerPreMain.CloudletContainerParameters.classpath = null;
		CloudletContainerPreMain.CloudletContainerParameters.configFile = arguments[0];
		arguments[0] = "eu.mosaic_cloud.cloudlet.runtime.ContainerComponentCallbacks";
		MosBasicComponentLauncher.main (arguments);
	}
}
