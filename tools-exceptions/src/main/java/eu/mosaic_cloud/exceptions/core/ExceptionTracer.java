/*
 * #%L
 * mosaic-tools-exceptions
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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

package eu.mosaic_cloud.exceptions.core;


import java.util.concurrent.atomic.AtomicReference;


public interface ExceptionTracer
{
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception);
	
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception, final String message);
	
	public abstract void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens);
	
	public static final AtomicReference<ExceptionTracer> defaultInstance = new AtomicReference<ExceptionTracer> (null);
}
