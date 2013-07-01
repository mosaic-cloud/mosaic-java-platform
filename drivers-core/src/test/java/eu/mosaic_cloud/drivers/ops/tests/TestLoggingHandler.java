/*
 * #%L
 * mosaic-platform-core
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

package eu.mosaic_cloud.drivers.ops.tests;


import eu.mosaic_cloud.drivers.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.transcript.core.Transcript;


public class TestLoggingHandler<T extends Object>
		implements
			IOperationCompletionHandler<T>
{
	public TestLoggingHandler (final String testName)
	{
		super ();
		this.name = testName;
		this.transcript = Transcript.create (this, true);
	}
	
	@Override
	public void onFailure (final Throwable error)
	{
		this.transcript.traceError ("Test `%s` finished with error `%{object:class}`: `%s`.", this.name, error, error.getMessage ());
		this.transcript.trace (ExceptionResolution.Ignored, error);
	}
	
	@Override
	public void onSuccess (final T result)
	{
		this.transcript.traceDebugging ("Test `%s` finished with result `%{object:class}`: `%s`.", result, result);
	}
	
	private final String name;
	private final Transcript transcript;
}
