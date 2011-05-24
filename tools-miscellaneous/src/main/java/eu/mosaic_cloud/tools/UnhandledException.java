
package eu.mosaic_cloud.tools;


public class UnhandledException
		extends CaughtException
{
	public UnhandledException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		super (ExceptionResolution.Unhandled, exception, messageFormat, messageArguments);
	}
	
	private static final long serialVersionUID = 1L;
}
