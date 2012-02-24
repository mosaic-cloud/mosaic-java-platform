
package eu.mosaic_cloud.connectors.queue.amqp;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.utils.DataEncoder;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public abstract class AmqpQueueConnectorProxy<Message>
		extends Object
		implements
			IAmqpQueueConnector
{
	protected AmqpQueueConnectorProxy (final AmqpQueueRawConnectorProxy raw, final IConfiguration config, final Class<Message> messageClass, final DataEncoder<Message> messageEncoder)
	{
		super ();
		Preconditions.checkNotNull (raw);
		Preconditions.checkNotNull (config);
		Preconditions.checkNotNull (messageClass);
		Preconditions.checkNotNull (messageEncoder);
		this.raw = raw;
		this.config = config;
		this.messageClass = messageClass;
		this.messageEncoder = messageEncoder;
	}
	
	@Override
	public abstract CallbackCompletion<Void> destroy ();
	
	protected final IConfiguration config;
	protected final Class<Message> messageClass;
	protected final DataEncoder<Message> messageEncoder;
	protected final AmqpQueueRawConnectorProxy raw;
}
