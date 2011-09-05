
package eu.mosaic_cloud.interoperability.core;


import java.util.concurrent.Executor;


public interface Session
{
	public abstract void continueDispatch ();
	
	public abstract void send (final Message message);
	
	public abstract void setCallbacks (final SessionCallbacks callbacks);
	
	public abstract void setExecutor (final Executor executor);
}
