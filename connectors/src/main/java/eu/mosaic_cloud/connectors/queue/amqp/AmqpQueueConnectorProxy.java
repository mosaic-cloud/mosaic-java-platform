
package eu.mosaic_cloud.connectors.queue.amqp;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public abstract class AmqpQueueConnectorProxy
		extends Object
{
	protected AmqpQueueConnectorProxy (final AmqpQueueRawConnectorProxy raw)
	{
		super ();
		Preconditions.checkNotNull (raw);
		this.raw = raw;
	}
	
	public CallbackCompletion<Void> destroy ()
	{
		return this.raw.destroy ();
	}
	
	public abstract CallbackCompletion<Void> unregister ();
	
	protected final AmqpQueueRawConnectorProxy raw;
}
