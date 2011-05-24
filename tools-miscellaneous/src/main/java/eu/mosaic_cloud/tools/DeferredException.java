
package eu.mosaic_cloud.tools;


public class DeferredException
		extends CaughtException
{
	public DeferredException (final Throwable exception, final String messageFormat, final Object ... messageArguments)
	{
		super (ExceptionResolution.Deferred, exception, messageFormat, messageArguments);
	}
	
	private static final long serialVersionUID = 1L;
}
