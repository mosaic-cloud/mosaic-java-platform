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

package eu.mosaic_cloud.transcript.implementations.slf4j;


import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.util.StatusPrinter;


public final class Slf4jStatusListener
		extends Object
		implements
			StatusListener
{
	public Slf4jStatusListener ()
	{
		super ();
	}
	
	@Override
	public final void addStatusEvent (final Status status)
	{
		final StringBuilder builder = new StringBuilder ();
		builder.append (String.format ("[%5s][STAT ] ", Slf4jJvmPidPropertyDefiner.defaultInstance.getPropertyValue ()));
		StatusPrinter.buildStr (builder, "", status);
		System.err.print (builder.toString ());
	}
}
