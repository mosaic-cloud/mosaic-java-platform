
package eu.mosaic_cloud.exceptions.core;


public class CaughtException
		extends Exception
{
	public CaughtException (final ExceptionResolution resolution, final Throwable exception)
	{
		super (exception);
		this.resolution = resolution;
		this.messageFormat = null;
		this.messageArguments = null;
	}
	
	public CaughtException (final ExceptionResolution resolution, final Throwable exception, final String message)
	{
		super (message, exception);
		this.resolution = resolution;
		this.messageFormat = null;
		this.messageArguments = null;
	}
	
	public CaughtException (final ExceptionResolution resolution, final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		super (String.format (messageFormat, messageArguments), exception);
		this.resolution = resolution;
		this.messageFormat = messageFormat;
		this.messageArguments = messageArguments;
	}
	
	public final Object[] getMessageArguments ()
	{
		return (this.messageArguments);
	}
	
	public final String getMessageFormat ()
	{
		return (this.messageFormat);
	}
	
	public final ExceptionResolution getResolution ()
	{
		return (this.resolution);
	}
	
	public final void trace (final ExceptionTracer tracer)
	{
		final Throwable cause = this.getCause ();
		if (this.messageFormat != null) {
			final String message = this.getMessage ();
			if (message != null)
				tracer.trace (this.resolution, cause, message);
			else
				tracer.trace (this.resolution, cause);
		} else
			tracer.trace (this.resolution, cause, this.messageFormat, this.messageArguments);
	}
	
	protected final Object[] messageArguments;
	protected final String messageFormat;
	protected final ExceptionResolution resolution;
	private static final long serialVersionUID = 1L;
}
