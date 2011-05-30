
package eu.mosaic_cloud.tools;


public abstract class CaughtException
		extends Exception
{
	public CaughtException (final ExceptionResolution resolution, final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		super (String.format (messageFormat, messageArguments), exception);
		this.resolution = resolution;
		this.messageFormat = messageFormat;
		this.messageArguments = messageArguments;
	}
	
	public Object[] getMessageArguments ()
	{
		return (this.messageArguments);
	}
	
	public String getMessageFormat ()
	{
		return (this.messageFormat);
	}
	
	public ExceptionResolution getResolution ()
	{
		return (this.resolution);
	}
	
	protected final Object[] messageArguments;
	protected final String messageFormat;
	protected final ExceptionResolution resolution;
	private static final long serialVersionUID = 1L;
}
