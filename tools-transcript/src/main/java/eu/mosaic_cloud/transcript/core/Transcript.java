
package eu.mosaic_cloud.transcript.core;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.ExceptionResolution;
import eu.mosaic_cloud.tools.ExtendedFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Transcript
{
	Transcript (final Logger logger, final ExtendedFormatter formatter)
	{
		super ();
		Preconditions.checkNotNull (logger);
		Preconditions.checkNotNull (formatter);
		this.logger = logger;
		this.formatter = formatter;
	}
	
	public final void trace (final TranscriptTraceType type, final String message)
	{
		this.trace (type, message, null, null);
	}
	
	public final void trace (final TranscriptTraceType type, final String format, final Object ... tokens)
	{
		this.trace (type, format, tokens, null);
	}
	
	public final void traceDebugging (final String message)
	{
		this.trace (TranscriptTraceType.Debugging, message);
	}
	
	public final void traceDebugging (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Debugging, format, tokens);
	}
	
	public final void traceDeferredException (final Throwable exception)
	{
		this.traceException (ExceptionResolution.Deferred, exception);
	}
	
	public final void traceDeferredException (final Throwable exception, final String message)
	{
		this.traceException (ExceptionResolution.Deferred, exception, message);
	}
	
	public final void traceDeferredException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (ExceptionResolution.Deferred, exception, format, tokens);
	}
	
	public final void traceError (final String message)
	{
		this.trace (TranscriptTraceType.Error, message);
	}
	
	public final void traceError (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Error, format, tokens);
	}
	
	public final void traceException (final ExceptionResolution resolution, final Throwable exception)
	{
		this.trace (this.map (resolution), null, null, exception);
	}
	
	public final void traceException (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		this.trace (this.map (resolution), message, null, exception);
	}
	
	public final void traceException (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.trace (this.map (resolution), format, tokens, exception);
	}
	
	public final void traceHandledException (final Throwable exception)
	{
		this.traceException (ExceptionResolution.Handled, exception);
	}
	
	public final void traceHandledException (final Throwable exception, final String message)
	{
		this.traceException (ExceptionResolution.Handled, exception, message);
	}
	
	public final void traceHandledException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (ExceptionResolution.Handled, exception, format, tokens);
	}
	
	public final void traceIgnoredException (final Throwable exception)
	{
		this.traceException (ExceptionResolution.Ignored, exception);
	}
	
	public final void traceIgnoredException (final Throwable exception, final String message)
	{
		this.traceException (ExceptionResolution.Ignored, exception, message);
	}
	
	public final void traceIgnoredException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (ExceptionResolution.Ignored, exception, format, tokens);
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
	
	private final String format (final String format, final Object[] tokens)
	{
		if (format == null) {
			if (tokens != null)
				throw (new IllegalArgumentException ());
			return ("");
		}
		if (tokens == null)
			return (format);
		return (this.formatter.format (format, tokens));
	}
	
	private final TranscriptTraceType map (final ExceptionResolution resolution)
	{
		switch (resolution) {
			case Handled :
				return (TranscriptTraceType.Debugging);
			case Deferred :
				return (TranscriptTraceType.Debugging);
			case Ignored :
				return (TranscriptTraceType.Warning);
		}
		return (TranscriptTraceType.Error);
	}
	
	private final void trace (final TranscriptTraceType type, final String format, final Object[] tokens, final Throwable exception)
	{
		Preconditions.checkNotNull (type);
		switch (type) {
			case Information :
				if (this.logger.isInfoEnabled ()) {
					final String message = this.format (format, tokens);
					if (exception != null)
						this.logger.info (message, exception);
					else
						this.logger.info (message);
				}
				break;
			case Warning :
				if (this.logger.isWarnEnabled ()) {
					final String message = this.format (format, tokens);
					if (exception != null)
						this.logger.warn (message, exception);
					else
						this.logger.warn (message);
				}
				break;
			case Error :
				if (this.logger.isErrorEnabled ()) {
					final String message = this.format (format, tokens);
					if (exception != null)
						this.logger.error (message, exception);
					else
						this.logger.error (message);
				}
				break;
			case Debugging :
				if (this.logger.isDebugEnabled ()) {
					final String message = this.format (format, tokens);
					if (exception != null)
						this.logger.debug (message, exception);
					else
						this.logger.debug (message);
				}
				break;
		}
	}
	
	private final ExtendedFormatter formatter;
	private final Logger logger;
	
	public static final Transcript create (final Class<?> owner)
	{
		Preconditions.checkNotNull (owner);
		return (new Transcript (LoggerFactory.getLogger (owner), ExtendedFormatter.defaultInstance));
	}
	
	public static final Transcript create (final Object owner)
	{
		Preconditions.checkNotNull (owner);
		return (new Transcript (LoggerFactory.getLogger (owner.getClass ()), ExtendedFormatter.defaultInstance));
	}
	
	public static final Transcript defaultInstance = new Transcript (LoggerFactory.getLogger ("eu.mosaic_cloud.transcript.core"), ExtendedFormatter.defaultInstance);
}
