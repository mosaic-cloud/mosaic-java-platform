
package eu.mosaic_cloud.exceptions.core;


public class HandledException
		extends CaughtException
{
	public HandledException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		super (ExceptionResolution.Handled, exception, messageFormat, messageArguments);
	}
	
	private static final long serialVersionUID = 1L;
}
