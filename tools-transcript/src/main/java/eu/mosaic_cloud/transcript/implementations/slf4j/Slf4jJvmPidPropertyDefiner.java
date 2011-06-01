
package eu.mosaic_cloud.transcript.implementations.slf4j;


import java.lang.management.ManagementFactory;

import ch.qos.logback.core.PropertyDefinerBase;


public final class Slf4jJvmPidPropertyDefiner
		extends PropertyDefinerBase
{
	public Slf4jJvmPidPropertyDefiner ()
	{
		super ();
		final String vm = ManagementFactory.getRuntimeMXBean ().getName ();
		if (vm.matches ("^[0-9]+@.*"))
			this.pid = vm.substring (0, vm.indexOf ('@'));
		else
			this.pid = "?";
	}
	
	@Override
	public final String getPropertyValue ()
	{
		return (this.pid);
	}
	
	private final String pid;
	
	public static final Slf4jJvmPidPropertyDefiner defaultInstance = new Slf4jJvmPidPropertyDefiner ();
}
