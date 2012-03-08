
package eu.mosaic_cloud.connectors.tests;

import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.Assert;

public class BaseScenario {

    private BasicCallbackReactor callbacks;
    private ChannelFactory channelFactory;
    private ChannelResolver channelResolver;
    private IConfiguration configuration;
    private ZeroMqChannel connectorChannel;
    private ZeroMqChannel driverChannel;
    private ConnectorEnvironment environment;
    private TranscriptExceptionTracer exceptions;
    private QueueingExceptionTracer exceptionsQueue;
    private long poolTimeout = 1000;
    private BasicThreadingContext threading;
    private Transcript transcript;

    public BaseScenario(
            final Class<? extends BaseConnectorTest<?, ? extends BaseScenario>> owner,
            final String configuration) {
        this.configuration = PropertyTypeConfiguration.create(
                owner.getClassLoader(), configuration);

        transcript = Transcript.create(owner);
        exceptionsQueue = QueueingExceptionTracer
                .create(NullExceptionTracer.defaultInstance);
        exceptions = TranscriptExceptionTracer.create(transcript,
                exceptionsQueue);

        // create threading context for connector and driver
        threading = BasicThreadingContext.create(owner, exceptions,
                exceptions.catcher);
        threading.initialize();

        // create callback reactor
        callbacks = BasicCallbackReactor.create(threading, exceptions);
        callbacks.initialize();

        // set-up communication channel with the driver
        final String driverIdentity = ConfigUtils.resolveParameter(
                this.configuration, "interop.driver.identifier", String.class,
                "");
        final String driverEndpoint = ConfigUtils
                .resolveParameter(this.configuration,
                        "interop.channel.address", String.class, "");
        connectorChannel = ZeroMqChannel.create(
                BaseConnectorTest.connectorIdentity, threading, exceptions);
        driverChannel = ZeroMqChannel.create(driverIdentity, threading,
                exceptions);
        driverChannel.accept(driverEndpoint);
        channelFactory = new ChannelFactory() {

            @Override
            public final Channel create() {
                return connectorChannel;
            }
        };
        channelResolver = new ChannelResolver() {

            @Override
            public final void resolve(String target, ResolverCallbacks callbacks) {
                Assert.assertEquals(driverIdentity, target);
                callbacks
                        .resolved(this, target, driverIdentity, driverEndpoint);
            }
        };
        environment = ConnectorEnvironment.create(callbacks, threading,
                exceptions, channelFactory, channelResolver);

        driverChannel.register(KeyValueSession.DRIVER);
    }

    public void destroy() {
        Assert.assertTrue(driverChannel.terminate(poolTimeout));
        Assert.assertTrue(connectorChannel.terminate(poolTimeout));
        Assert.assertTrue(callbacks.destroy(poolTimeout));
        Assert.assertTrue(threading.destroy(poolTimeout));
        Assert.assertNull(exceptionsQueue.queue.poll());
    }

    public ChannelResolver getChannelResolver() {
        return channelResolver;
    }

    public void registerDriverRole(SessionSpecification sessionRole) {
        driverChannel.register(sessionRole);
    }

    public long getPoolTimeout() {
        return poolTimeout;
    }

    public IConfiguration getConfiguration() {
        return configuration;
    }

    public ConnectorEnvironment getEnvironment() {
        return environment;
    }

    public BasicThreadingContext getThreading() {
        return threading;
    }

    public ZeroMqChannel getDriverChannel() {
        return driverChannel;
    }
}
