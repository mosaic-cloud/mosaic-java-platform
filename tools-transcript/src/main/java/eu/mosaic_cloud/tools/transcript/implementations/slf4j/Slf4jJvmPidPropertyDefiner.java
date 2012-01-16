/*
 * #%L
 * mosaic-tools-transcript
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

package eu.mosaic_cloud.tools.transcript.implementations.slf4j;


import java.lang.management.ManagementFactory;

import ch.qos.logback.core.PropertyDefinerBase;


public final class Slf4jJvmPidPropertyDefiner
		extends PropertyDefinerBase
{
	public Slf4jJvmPidPropertyDefiner ()
	{
		super ();
		final String vm = ManagementFactory.getRuntimeMXBean ().getName ();
		if (vm.matches ("^[0-9]+@.*"))
			this.pid = vm.substring (0, vm.indexOf ('@'));
		else
			this.pid = "?";
	}
	
	@Override
	public final String getPropertyValue ()
	{
		return (this.pid);
	}
	
	private final String pid;
	public static final Slf4jJvmPidPropertyDefiner defaultInstance = new Slf4jJvmPidPropertyDefiner ();
}
