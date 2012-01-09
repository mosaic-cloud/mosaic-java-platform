/*
 * #%L
 * tools-exceptions
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

package eu.mosaic_cloud.exceptions.tools;


import eu.mosaic_cloud.exceptions.core.ExceptionResolution;


public final class AbortingExceptionTracer
		extends BaseExceptionTracer
{
	private AbortingExceptionTracer ()
	{
		super ();
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception)
	{
		final boolean abort;
		switch (resolution) {
			case Handled :
				abort = false;
				break;
			case Deferred :
				abort = false;
				break;
			case Ignored :
				abort = true;
				break;
			default:
				abort = true;
				break;
		}
		if (abort) {
			try {
				exception.printStackTrace (System.err);
			} catch (final Throwable exception1) {
				// intentional
			}
			new Thread () {
				@Override
				public final void run ()
				{
					try {
						Thread.sleep (2000);
					} catch (final InterruptedException exception1) {
						// intentional
					}
					Runtime.getRuntime ().halt (1);
				}
			}.start ();
			System.exit (1);
		}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		this.trace (resolution, exception);
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.trace (resolution, exception);
	}
	
	public static final AbortingExceptionTracer defaultInstance = new AbortingExceptionTracer ();
}
