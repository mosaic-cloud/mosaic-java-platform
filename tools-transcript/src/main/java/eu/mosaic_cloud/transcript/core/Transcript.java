
package eu.mosaic_cloud.transcript.core;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.ExceptionResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Transcript
{
	Transcript (final boolean useLogger)
	{
		super ();
		if (useLogger)
			this.logger = LoggerFactory.getLogger ("eu.mosaic_cloud.transcript.core");
		else
			this.logger = null;
	}
	
	Transcript (final Logger logger)
	{
		super ();
		Preconditions.checkNotNull (logger);
		this.logger = logger;
	}
	
	public final void trace (final TranscriptTraceType type, final String message)
	{
		Preconditions.checkNotNull (type);
		Preconditions.checkNotNull (message);
		if (this.logger != null) {
			switch (type) {
				case Information :
					if (this.logger.isInfoEnabled ())
						this.logger.info (message);
					break;
				case Warning :
					if (this.logger.isWarnEnabled ())
						this.logger.warn (message);
					break;
				case Error :
					if (this.logger.isErrorEnabled ())
						this.logger.error (message);
					break;
				case Debugging :
					if (this.logger.isDebugEnabled ())
						this.logger.debug (message);
					break;
				case Input :
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message);
					break;
				case Output :
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message);
					break;
				case Event :
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message);
					break;
				default:
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message);
					break;
			}
		} else {
			synchronized (System.err) {
				switch (type) {
					case Information :
						System.err.println ("[ii] " + message);
						break;
					case Warning :
						System.err.println ("[ww] " + message);
						break;
					case Error :
						System.err.println ("[ee] " + message);
						break;
					case Input :
						System.err.println ("[<<] " + message);
						break;
					case Output :
						System.err.println ("[>>] " + message);
						break;
					case Event :
						System.err.println ("[--] " + message);
						break;
					case Debugging :
						System.err.println ("[  ] " + message);
						break;
					default:
						System.err.println ("[??] " + message);
						break;
				}
			}
		}
	}
	
	public final void trace (final TranscriptTraceType type, final String format, final Object ... tokens)
	{
		this.trace (type, String.format (format, tokens));
	}
	
	public final void traceDebugging (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Debugging, format, tokens);
	}
	
	public final void traceDeferredException (final Throwable exception)
	{
		this.traceException (ExceptionResolution.Deferred, exception, null);
	}
	
	public final void traceDeferredException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (ExceptionResolution.Deferred, exception, format, tokens);
	}
	
	public final void traceError (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Error, format, tokens);
	}
	
	public final void traceEvent (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Event, format, tokens);
	}
	
	public final void traceException (final ExceptionResolution resolution, final Throwable exception, final String message_)
	{
		final String message = message_ != null ? message_ : "encountered exception";
		if (this.logger != null) {
			switch (resolution) {
				case Handled :
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message, exception);
					break;
				case Rethrown :
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message, exception);
					break;
				case Ignored :
					if (this.logger.isWarnEnabled ())
						this.logger.warn (message, exception);
					break;
				case Deferred :
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message, exception);
					break;
				default:
					if (this.logger.isTraceEnabled ())
						this.logger.trace (message, exception);
					break;
			}
		} else {
			synchronized (System.err) {
				switch (resolution) {
					case Handled :
						System.err.println ("[  ] " + message + " / " + exception.toString ());
						break;
					case Rethrown :
						System.err.println ("[  ] " + message + " / " + exception.toString ());
						break;
					case Ignored :
						System.err.println ("[ww] " + message + " / " + exception.toString ());
						break;
					case Deferred :
						System.err.println ("[  ] " + message + " / " + exception.toString ());
						break;
					default:
						System.err.println ("[??] " + message + " / " + exception.toString ());
						break;
				}
				exception.printStackTrace (System.err);
			}
		}
	}
	
	public final void traceException (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (resolution, exception, String.format (format, tokens));
	}
	
	public final void traceHandledException (final Throwable exception)
	{
		this.traceException (ExceptionResolution.Handled, exception, null);
	}
	
	public final void traceHandledException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (ExceptionResolution.Handled, exception, format, tokens);
	}
	
	public final void traceIgnoredException (final Throwable exception)
	{
		this.traceException (ExceptionResolution.Ignored, exception, null);
	}
	
	public final void traceIgnoredException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (ExceptionResolution.Ignored, exception, format, tokens);
	}
	
	public final void traceInformation (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Information, format, tokens);
	}
	
	public final void traceInput (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Input, format, tokens);
	}
	
	public final void traceOutput (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Output, format, tokens);
	}
	
	public final void traceRethrownException (final Throwable exception)
	{
		this.traceException (ExceptionResolution.Rethrown, exception, null);
	}
	
	public final void traceRethrownException (final Throwable exception, final String format, final Object ... tokens)
	{
		this.traceException (ExceptionResolution.Rethrown, exception, format, tokens);
	}
	
	public final void traceWarning (final String format, final Object ... tokens)
	{
		this.trace (TranscriptTraceType.Warning, format, tokens);
	}
	
	private final Logger logger;
	
	public static final Transcript create (final Object owner)
	{
		Preconditions.checkNotNull (owner);
		return (new Transcript (LoggerFactory.getLogger (owner.getClass ())));
	}
	
	public static final Transcript defaultInstance = new Transcript (Transcript.defaultInstanceUseLogger);
	private static final boolean defaultInstanceUseLogger = true;
}
