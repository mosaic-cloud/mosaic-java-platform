
package eu.mosaic_cloud.tools;


public class UnexpectedException
		extends CaughtException
{
	public UnexpectedException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		super (ExceptionResolution.Unexpected, exception, messageFormat, messageArguments);
	}
	
	private static final long serialVersionUID = 1L;
}
