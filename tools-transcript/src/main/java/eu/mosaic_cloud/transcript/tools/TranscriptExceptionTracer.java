
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
