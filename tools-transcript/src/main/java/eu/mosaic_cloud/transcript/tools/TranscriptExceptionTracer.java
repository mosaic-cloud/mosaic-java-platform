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

package eu.mosaic_cloud.transcript.tools;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.exceptions.tools.InterceptingExceptionTracer;
import eu.mosaic_cloud.transcript.core.TranscriptBackend;


public final class TranscriptExceptionTracer
		extends InterceptingExceptionTracer
{
	private TranscriptExceptionTracer (final TranscriptBackend transcript, final ExceptionTracer delegate)
	{
		super (delegate);
		Preconditions.checkNotNull (transcript);
		this.transcript = transcript;
	}
	
	@Override
	protected final void trace_ (final ExceptionResolution resolution, final Throwable exception)
	{
		this.transcript.trace (resolution, exception);
	}
	
	@Override
	protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		this.transcript.trace (resolution, exception, message);
	}
	
	@Override
	protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.transcript.trace (resolution, exception, format, tokens);
	}
	
	private final TranscriptBackend transcript;
	
	public static final TranscriptExceptionTracer create (final TranscriptBackend transcript, final ExceptionTracer delegate)
	{
		return (new TranscriptExceptionTracer (transcript, delegate));
	}
}
