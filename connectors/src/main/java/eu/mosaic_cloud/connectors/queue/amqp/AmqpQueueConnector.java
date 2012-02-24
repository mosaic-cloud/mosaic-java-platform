
package eu.mosaic_cloud.connectors.queue.amqp;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

import com.google.common.base.Preconditions;

public abstract class AmqpQueueConnector<P extends AmqpQueueConnectorProxy<?>> implements
        IAmqpQueueConnector {

    protected final P proxy;

    protected AmqpQueueConnector(final P proxy) {
        super();
        Preconditions.checkNotNull(proxy);
        this.proxy = proxy;
    }

    @Override
    public CallbackCompletion<Void> destroy() {
        return this.proxy.destroy();
    }

    @Override
    public CallbackCompletion<Void> initialize() {
        return CallbackCompletion.createOutcome();
    }
}
