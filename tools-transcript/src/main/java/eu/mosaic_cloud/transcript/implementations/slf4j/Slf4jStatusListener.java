
package eu.mosaic_cloud.transcript.implementations.slf4j;


import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.util.StatusPrinter;


public final class Slf4jStatusListener
		extends Object
		implements
			StatusListener
{
	public Slf4jStatusListener ()
	{
		super ();
	}
	
	@Override
	public final void addStatusEvent (final Status status)
	{
		final StringBuilder builder = new StringBuilder ();
		builder.append (String.format ("[%5s][STAT ] ", Slf4jJvmPidPropertyDefiner.defaultInstance.getPropertyValue ()));
		StatusPrinter.buildStr (builder, "", status);
		System.err.print (builder.toString ());
	}
}
