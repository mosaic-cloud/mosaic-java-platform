
package eu.mosaic_cloud.transcript.core;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.transcript.implementations.slf4j.Slf4jTranscriptBackend;


public final class Transcript
		extends Object
		implements
			TranscriptBackend
{
	Transcript (final TranscriptBackend backend)
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
		} catch (final Throwable exception1) {}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		try {
			this.backend.trace (resolution, exception, message);
		} catch (final Throwable exception1) {}
	}
	
	@Override
	public final void trace (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		try {
			this.backend.trace (resolution, exception, format, tokens);
		} catch (final Throwable exception1) {}
	}
	
	@Override
	public final void trace (final TranscriptTraceType type, final String message)
	{
		try {
			this.backend.trace (type, message);
		} catch (final Throwable exception1) {}
	}
	
	@Override
	public final void trace (final TranscriptTraceType type, final String format, final Object ... tokens)
	{
		try {
			this.backend.trace (type, format, tokens);
		} catch (final Throwable exception1) {}
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
		return (new Transcript (Slf4jTranscriptBackend.create (owner)));
	}
	
	public static final Transcript create (final Object owner)
	{
		Preconditions.checkNotNull (owner);
		return (new Transcript (Slf4jTranscriptBackend.create (owner)));
	}
}
