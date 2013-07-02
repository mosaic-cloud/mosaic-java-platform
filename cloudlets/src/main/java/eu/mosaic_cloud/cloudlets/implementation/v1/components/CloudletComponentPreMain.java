/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.cloudlets.implementation.v1.components;


import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;


public final class CloudletComponentPreMain
		extends Object
{
	private CloudletComponentPreMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	public static void main (final String descriptor, final String[] arguments)
			throws Throwable
	{
		final String configuration = String.format ("{\"%s\":\"%s\"}", "descriptor", descriptor);
		BasicComponentHarnessPreMain.main (CloudletComponentPreMain.class.getName ().replace ("PreMain", "$ComponentCallbacksProvider"), configuration, arguments);
	}
	
	public static void main (final String[] arguments)
			throws Throwable
	{
		BasicComponentHarnessPreMain.main (CloudletComponentPreMain.class.getName ().replace ("PreMain", "$ComponentCallbacksProvider"), arguments);
	}
}
