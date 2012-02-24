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

package eu.mosaic_cloud.tools.transcript.core;


import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.transcript.implementations.logback.LogbackTranscriptBackend;

import com.google.common.base.Preconditions;


public final class Transcript
		extends Object
		implements
			TranscriptBackend
{
	private Transcript (final TranscriptBackend backend)
	{
		super ();
		Preconditions.checkNotNull (backend);
		this.backend = backend;
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception)
	{
		try {
			this.backend.trace (resolution, exception);
		} catch (final Throwable exception1) {
			// NOTE: intentional
		}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		try {
			this.backend.trace (resolution, exception, message);
		} catch (final Throwable exception1) {
			// NOTE: intentional
		}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		try {
			this.backend.trace (resolution, exception, format, tokens);
		} catch (final Throwable exception1) {
			// NOTE: intentional
		}
	}
	
	@Override
	public final void trace (final TranscriptTraceType type, final String message)
	{
		try {
			this.backend.trace (type, message);
		} catch (final Throwable exception1) {
			// NOTE: intentional
		}
	}
	
	@Override
	public final void trace (final TranscriptTraceType type, final String format, final Object ... tokens)
	{
		try {
			this.backend.trace (type, format, tokens);
		} catch (final Throwable exception1) {
			// NOTE: intentional
		}
	}
	
	public final void traceDebugging (final String message)
	{
		this.trace (TranscriptTraceType.Debugging, message);
	}
	
	public final void traceDebugging (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Debugging, format, tokens);
	}
	
	public final void traceError (final String message)
	{
		this.trace (TranscriptTraceType.Error, message);
	}
	
	public final void traceError (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Error, format, tokens);
	}
	
	public final void traceInformation (final String message)
	{
		this.trace (TranscriptTraceType.Information, message);
	}
	
	public final void traceInformation (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Information, format, tokens);
	}
	
	public final void traceWarning (final String message)
	{
		this.trace (TranscriptTraceType.Warning, message);
	}
	
	public final void traceWarning (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Warning, format, tokens);
	}
	
	private final TranscriptBackend backend;
	
	public static final Transcript create (final Class<?> owner)
	{
		Preconditions.checkNotNull (owner);
		return (new Transcript (LogbackTranscriptBackend.create (owner)));
	}
	
	public static final Transcript create (final Object owner)
	{
		Preconditions.checkNotNull (owner);
		return (new Transcript (LogbackTranscriptBackend.create (owner)));
	}
}
