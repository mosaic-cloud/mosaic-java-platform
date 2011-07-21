package mosaic.driver.kvstore;

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
import mosaic.driver.interop.kvstore.KeyValueStub;
import mosaic.interop.kvstore.KeyValueSession;

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
 * This callback class enables the Key Value store driver to be exposed as a
 * component. Upon initialization it will look for a Key Value store server and
 * will create a driver object for the server.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class KVDriverComponentCallbacks extends
		AbstractDriverComponentCallbacks {

	/**
	 * Creates a driver callback.
	 */
	public KVDriverComponentCallbacks() {
		super();
		this.monitor = Monitor.create(this);
		try {
			IConfiguration configuration = PropertyTypeConfiguration
					.create(KVDriverComponentCallbacks.class
							.getResourceAsStream("kv.properties"));
			AbstractResourceDriver.driverConfiguration = configuration;
			this.resourceGroup = ComponentIdentifier.resolve(ConfigUtils
					.resolveParameter(
							AbstractResourceDriver.driverConfiguration,
							ConfigProperties
									.getString("KVDriverComponentCallbacks.0"), //$NON-NLS-1$
							String.class, "")); //$NON-NLS-1$
			this.selfGroup = ComponentIdentifier.resolve(ConfigUtils
					.resolveParameter(
							AbstractResourceDriver.driverConfiguration,
							ConfigProperties
									.getString("KVDriverComponentCallbacks.1"), //$NON-NLS-1$
							String.class, "")); //$NON-NLS-1$

			synchronized (this.monitor) {
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
			Preconditions
					.checkState((this.status != KVDriverComponentCallbacks.Status.Terminated)
							&& (this.status != KVDriverComponentCallbacks.Status.Unregistered));
			if (this.status == KVDriverComponentCallbacks.Status.Registered) {
				if (request.operation.equals(ConfigProperties
						.getString("KVDriverComponentCallbacks.5"))) { //$NON-NLS-1$
					String channelEndpoint = ConfigUtils.resolveParameter(
							AbstractResourceDriver.driverConfiguration,
							ConfigProperties
									.getString("KVDriverComponentCallbacks.3"), //$NON-NLS-1$
							String.class, "");
					String channelId = ConfigUtils.resolveParameter(
							AbstractResourceDriver.driverConfiguration,
							ConfigProperties
									.getString("KVDriverComponentCallbacks.4"), //$NON-NLS-1$
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
					String ip;
					Integer port;
					try {
						Preconditions.checkArgument(reply.ok);
						Preconditions
								.checkArgument(reply.outputsOrError instanceof Map);
						final Map<?, ?> outputs = (Map<?, ?>) reply.outputsOrError;
						MosaicLogger.getLogger().trace(
								"Resource search returned " + outputs);

						ip = (String) outputs.get("ip"); //$NON-NLS-1$
						Preconditions.checkNotNull(ip);
						port = (Integer) outputs.get("port"); //$NON-NLS-1$
						Preconditions.checkNotNull(port);
					} catch (final Throwable exception) {
						this.terminate();
						ExceptionTracer
								.traceIgnored(
										exception,
										"failed resolving Riak broker endpoint: `%s`; terminating!", //$NON-NLS-1$
										reply.outputsOrError);
						throw new IllegalStateException();
					}
					MosaicLogger.getLogger().trace(
							"Resolved Riak on " + ip + ":" //$NON-NLS-1$ //$NON-NLS-2$
									+ port);
					this.configureDriver(ip, port.toString());
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

	private void configureDriver(String brokerIp, String port) {
		AbstractResourceDriver.driverConfiguration.addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("KVStoreDriver.0")), brokerIp); //$NON-NLS-1$
		AbstractResourceDriver.driverConfiguration.addParameter(
				ConfigurationIdentifier.resolveRelative(ConfigProperties
						.getString("KVStoreDriver.1")), port); //$NON-NLS-1$

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
							.getString("KVDriverComponentCallbacks.2"), null, //$NON-NLS-1$
							callReference));
			this.pendingReference = callReference;
			this.status = Status.WaitingResourceResolved;
			MosaicLogger.getLogger().trace(
					"Key Value driver callback initialized.");
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
						.info("Key Value Store driver callback registered to group " + this.selfGroup); //$NON-NLS-1$
				this.status = Status.Registered;

				// create stub and interop channel
				ZeroMqChannel driverChannel = createDriverChannel(
						ConfigProperties
								.getString("KVDriverComponentCallbacks.4"),
						ConfigProperties
								.getString("KVDriverComponentCallbacks.3"),
						KeyValueSession.DRIVER);
				stub = KeyValueStub.create(
						AbstractResourceDriver.driverConfiguration,
						driverChannel);
			} else
				throw (new IllegalStateException());
		}
		return null;
	}

}