
package eu.mosaic_cloud.transcript.core;


import eu.mosaic_cloud.exceptions.core.ExceptionTracer;


public interface TranscriptBackend
		extends
			ExceptionTracer
{
	public abstract void trace (final TranscriptTraceType type, final String message);
	
	public abstract void trace (final TranscriptTraceType type, final String format, final Object ... tokens);
}
