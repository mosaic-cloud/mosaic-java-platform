package mosaic.driver.queue.amqp;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.ConfigurationIdentifier;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.AbstractDriverComponentCallbacks;
import mosaic.driver.AbstractResourceDriver;
import mosaic.driver.ConfigProperties;
import mosaic.driver.interop.queue.amqp.AmqpStub;
import mosaic.interop.amqp.AmqpSession;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.tools.Monitor;

/**
 * This callback class enables the AMQP driver to be exposed as a component.
 * Upon initialization it will look for a RabbitMQ server and will create a
 * driver object for the server.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class AmqpDriverComponentCallbacks extends
		AbstractDriverComponentCallbacks {

	/**
	 * Creates a driver callback.
	 */
	public AmqpDriverComponentCallbacks() {
		super();
		this.monitor = Monitor.create(this);
		try {
			IConfiguration configuration = PropertyTypeConfiguration
					.create(AmqpDriverComponentCallbacks.class
							.getResourceAsStream("amqp.properties"));
			AbstractResourceDriver.driverConfiguration = configuration;
			this.resourceGroup = ComponentIdentifier
					.resolve(ConfigUtils
							.resolveParameter(
									AbstractResourceDriver.driverConfiguration,
									ConfigProperties
											.getString("AmqpDriverComponentCallbacks.0"), //$NON-NLS-1$
									String.class, "")); //$NON-NLS-1$
			this.selfGroup = ComponentIdentifier
					.resolve(ConfigUtils
							.resolveParameter(
									AbstractResourceDriver.driverConfiguration,
									ConfigProperties
											.getString("AmqpDriverComponentCallbacks.1"), //$NON-NLS-1$
									String.class, "")); //$NON-NLS-1$

			synchronized (this) {
				this.status = Status.Created;
			}
		} catch (Throwable e) {
			ExceptionTracer.traceDeferred(e);
		}
	}

	@Override
	public CallbackReference called(Component component,
			ComponentCallRequest request) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			Preconditions.checkState((this.status != Status.Terminated)
					&& (this.status != Status.Unregistered));
			if (this.status == Status.Registered) {
				if (request.operation.equals(ConfigProperties
						.getString("AmqpDriverComponentCallbacks.5"))) { //$NON-NLS-1$
					String channelEndpoint = ConfigUtils
							.resolveParameter(
									AbstractResourceDriver.driverConfiguration,
									ConfigProperties
											.getString("AmqpDriverComponentCallbacks.3"), //$NON-NLS-1$
									String.class, "");
					String channelId = ConfigUtils
							.resolveParameter(
									AbstractResourceDriver.driverConfiguration,
									ConfigProperties
											.getString("AmqpDriverComponentCallbacks.4"), //$NON-NLS-1$
									String.class, "");
					Map<String, String> outcome = new HashMap<String, String>();
					outcome.put("channelEndpoint", channelEndpoint);
					outcome.put("channelIdentifier", channelId);
					ComponentCallReply reply = ComponentCallReply.create(true,
							outcome, ByteBuffer.allocate(0), request.reference);
					component.reply(reply);
				} else
					throw new UnsupportedOperationException();
			} else
				throw new UnsupportedOperationException();
			return null;
		}
	}

	@Override
	public CallbackReference callReturned(Component component,
			ComponentCallReply reply) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			if (this.pendingReference == reply.reference)
				if (this.status == Status.WaitingResourceResolved) {
					this.pendingReference = null;
					String rabbitmqTransport;
					String brokerIp;
					Integer brokerPort;
					String user;
					String password;
					String virtualHost;
					try {
						Preconditions.checkArgument(reply.ok);
						Preconditions
								.checkArgument(reply.outputsOrError instanceof Map);
						final Map<?, ?> outputs = (Map<?, ?>) reply.outputsOrError;
						rabbitmqTransport = (String) outputs.get("transport"); //$NON-NLS-1$
						Preconditions.checkArgument("tcp" //$NON-NLS-1$
								.equals(rabbitmqTransport));
						brokerIp = (String) outputs.get("ip"); //$NON-NLS-1$
						Preconditions.checkNotNull(brokerIp);
						brokerPort = (Integer) outputs.get("port"); //$NON-NLS-1$
						Preconditions.checkNotNull(brokerPort);
						user = (String) outputs.get("username"); //$NON-NLS-1$
						if (user == null)
							user = ""; //$NON-NLS-1$
						password = (String) outputs.get("password"); //$NON-NLS-1$
						if (password == null)
							password = ""; //$NON-NLS-1$
						virtualHost = (String) outputs.get("virtualHost"); //$NON-NLS-1$
						if (virtualHost == null)
							virtualHost = ""; //$NON-NLS-1$
					} catch (final Throwable exception) {
						this.terminate();
						ExceptionTracer
								.traceIgnored(
										exception,
										"failed resolving RabbitMQ broker endpoint: `%s`; terminating!", //$NON-NLS-1$
										reply.outputsOrError);
						throw new IllegalStateException();
					}
					MosaicLogger.getLogger().trace(
							"Resolved RabbitMQ on " + brokerIp + ":" //$NON-NLS-1$ //$NON-NLS-2$
									+ brokerPort);
					this.configureDriver(brokerIp, brokerPort.toString(), user,
							password, virtualHost);
					if (this.selfGroup != null) {
						this.pendingReference = ComponentCallReference.create();
						this.status = Status.Unregistered;
						this.component.register(this.selfGroup,
								this.pendingReference);
					}
				} else
					throw (new IllegalStateException());
			else
				throw (new IllegalStateException());
		}
		return null;
	}

	private void configureDriver(String brokerIp, String port, String user,
			String pass, String vHost) {
		AbstractResourceDriver.driverConfiguration.addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("AmqpDriver.1")), brokerIp); //$NON-NLS-1$
		AbstractResourceDriver.driverConfiguration.addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("AmqpDriver.2")), port); //$NON-NLS-1$
		AbstractResourceDriver.driverConfiguration.addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("AmqpDriver.3")), user); //$NON-NLS-1$
		AbstractResourceDriver.driverConfiguration.addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("AmqpDriver.4")), pass); //$NON-NLS-1$
		AbstractResourceDriver.driverConfiguration.addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("AmqpDriver.5")), vHost); //$NON-NLS-1$
	}

	@Override
	public CallbackReference initialized(Component component) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == null);
			Preconditions.checkState(this.status == Status.Created);
			this.component = component;
			final ComponentCallReference callReference = ComponentCallReference
					.create();
			this.component.call(this.resourceGroup, ComponentCallRequest
					.create(ConfigProperties
							.getString("AmqpDriverComponentCallbacks.2"), null, //$NON-NLS-1$
							callReference));
			this.pendingReference = callReference;
			this.status = Status.WaitingResourceResolved;
			MosaicLogger.getLogger().trace("AMQP driver callback initialized.");
		}
		return null;
	}

	@Override
	public CallbackReference registerReturn(Component component,
			ComponentCallReference reference, boolean ok) {
		synchronized (this.monitor) {
			Preconditions.checkState(this.component == component);
			if (this.pendingReference == reference) {
				this.pendingReference = null;
				if (!ok) {
					ExceptionTracer.traceHandled(new Exception(
							"failed registering to group; terminating!")); //$NON-NLS-1$
					this.component.terminate();
					throw (new IllegalStateException());
				}
				MosaicLogger
						.getLogger()
						.info("AMQP driver callback registered to group " + this.selfGroup); //$NON-NLS-1$
				this.status = Status.Registered;

				// create stub and interop channel
				String channelId = ConfigProperties
						.getString("AmqpDriverComponentCallbacks.4");
				String channelEndpoint = ConfigProperties
						.getString("AmqpDriverComponentCallbacks.3");
				ZeroMqChannel driverChannel = createDriverChannel(channelId,
						channelEndpoint, AmqpSession.DRIVER);

				stub = AmqpStub.create(
						AbstractResourceDriver.driverConfiguration,
						driverChannel);
			} else
				throw (new IllegalStateException());
		}
		return null;
	}

}