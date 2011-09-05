
package eu.mosaic_cloud.exceptions.core;


public class IgnoredException
		extends CaughtException
{
	public IgnoredException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		super (ExceptionResolution.Ignored, exception, messageFormat, messageArguments);
	}
	
	private static final long serialVersionUID = 1L;
}
