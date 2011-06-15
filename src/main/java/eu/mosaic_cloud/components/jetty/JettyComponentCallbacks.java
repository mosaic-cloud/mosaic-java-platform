
package eu.mosaic_cloud.components.jetty;


import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.jetty.connectors.httpg.ServerCommandLine;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.tools.OutcomeFuture;
import eu.mosaic_cloud.tools.OutcomeFuture.OutcomeTrigger;
import eu.mosaic_cloud.transcript.core.Transcript;
import eu.mosaic_cloud.transcript.tools.TranscriptExceptionTracer;
import org.eclipse.jetty.server.Server;


public final class JettyComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler<ComponentCallbacks>
{
	public JettyComponentCallbacks ()
	{
		super ();
		this.monitor = Monitor.create (this);
		synchronized (this) {
			this.transcript = Transcript.create (this);
			this.exceptions = TranscriptExceptionTracer.create (this.transcript);
			JettyComponentContext.callbacks = this;
			this.status = Status.WaitingRegistered;
		}
	}
	
	public final Future<ComponentCallReply> call (final ComponentIdentifier component, final ComponentCallRequest request)
	{
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (request);
		final OutcomeFuture<ComponentCallReply> replyFuture = OutcomeFuture.create ();
		synchronized (this.monitor) {
			Preconditions.checkState (this.component != null);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			Preconditions.checkArgument (this.pendingCallReturnFutures.containsKey (request.reference));
			this.pendingCallReturnFutures.put (request.reference, replyFuture.trigger);
			this.component.call (component, request);
		}
		return (replyFuture);
	}
	
	@Override
	public CallbackReference called (final Component component, final ComponentCallRequest request)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			throw (new UnsupportedOperationException ());
		}
	}
	
	@Override
	public CallbackReference callReturned (final Component component, final ComponentCallReply reply)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			if (this.pendingReference == reply.reference)
				switch (this.status) {
					case WaitingRabbitmqResolveReturn : {
						this.pendingReference = null;
						final String rabbitmqTransport;
						final String brokerIp;
						final Integer brokerPort;
						try {
							Preconditions.checkArgument (reply.ok);
							Preconditions.checkArgument (reply.outputsOrError instanceof Map);
							final Map<?, ?> outputs = (Map<?, ?>) reply.outputsOrError;
							rabbitmqTransport = (String) outputs.get ("transport");
							Preconditions.checkArgument ("tcp".equals (rabbitmqTransport));
							brokerIp = (String) outputs.get ("ip");
							Preconditions.checkNotNull (brokerIp);
							brokerPort = (Integer) outputs.get ("port");
							Preconditions.checkNotNull (brokerPort);
						} catch (final Throwable exception) {
							this.component.terminate ();
							this.exceptions.traceIgnoredException (exception, "failed resolving RabbitMQ broker endpoint: `%s`; terminating!", reply.outputsOrError);
							throw (new IllegalStateException ());
						}
						this.startJetty (brokerIp, brokerPort.intValue ());
					}
						break;
					default:
						throw (new IllegalStateException ());
				}
			else if (this.pendingCallReturnFutures.containsKey (reply.reference)) {
				final OutcomeTrigger<ComponentCallReply> trigger = this.pendingCallReturnFutures.remove (reply.reference);
				trigger.succeeded (reply);
			} else
				throw (new IllegalStateException ());
		}
		return (null);
	}
	
	public final void cast (final ComponentIdentifier component, final ComponentCastRequest request)
	{
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (request);
		synchronized (this.monitor) {
			Preconditions.checkState (this.component != null);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			this.component.cast (component, request);
		}
	}
	
	@Override
	public CallbackReference casted (final Component component, final ComponentCastRequest request)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			throw (new UnsupportedOperationException ());
		}
	}
	
	@Override
	public void deassigned (final ComponentCallbacks trigger, final ComponentCallbacks newCallbacks)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public CallbackReference failed (final Component component, final Throwable exception)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			if (this.jettyServer != null)
				this.stopJetty ();
			this.component = null;
			this.status = Status.Terminated;
			this.exceptions.traceIgnoredException (exception);
		}
		return (null);
	}
	
	@Override
	public CallbackReference initialized (final Component component)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == null);
			Preconditions.checkState (this.status == Status.WaitingInitialized);
			this.component = component;
			final ComponentCallReference callReference = ComponentCallReference.create ();
			this.component.call (JettyComponentContext.brokerGroup, ComponentCallRequest.create ("mosaic-rabbitmq:get-broker-endpoint", null, callReference));
			this.pendingReference = callReference;
			this.status = Status.WaitingRabbitmqResolveReturn;
		}
		return (null);
	}
	
	@Override
	public void reassigned (final ComponentCallbacks trigger, final ComponentCallbacks oldCallbacks)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public void registered (final ComponentCallbacks trigger)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == null);
			Preconditions.checkState (this.status == Status.WaitingRegistered);
			this.status = Status.WaitingInitialized;
		}
	}
	
	@Override
	public CallbackReference terminated (final Component component)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			if (this.jettyServer != null)
				this.stopJetty ();
			this.component = null;
			this.status = Status.Terminated;
		}
		return (null);
	}
	
	@Override
	public void unregistered (final ComponentCallbacks trigger)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.status != Status.Unregistered);
			if (this.jettyServer != null)
				this.stopJetty ();
			this.stopJetty ();
			this.component = null;
			this.status = Status.Unregistered;
		}
	}
	
	final void startJetty (final String brokerIp, final int brokerPort)
	{
		synchronized (this.monitor) {
			final Properties jettyProperties = new Properties ();
			jettyProperties.setProperty ("server", brokerIp);
			jettyProperties.setProperty ("port", Integer.toString (brokerPort));
			jettyProperties.setProperty ("virtual-host", JettyComponentContext.brokerVirtualHost);
			jettyProperties.setProperty ("username", JettyComponentContext.brokerUsername);
			jettyProperties.setProperty ("password", JettyComponentContext.brokerPassword);
			jettyProperties.setProperty ("exchange", JettyComponentContext.httpgRequestsExchange);
			jettyProperties.setProperty ("routing-key", JettyComponentContext.httpgRequestsRoutingKey);
			jettyProperties.setProperty ("queue", JettyComponentContext.httpgRequestsQueue);
			if (JettyComponentContext.httpgRequestsAutodeclare)
				jettyProperties.setProperty ("auto-declare", "true");
			jettyProperties.setProperty ("webapp", JettyComponentContext.appWar.getAbsolutePath ());
			jettyProperties.setProperty ("app-context", JettyComponentContext.appContextPath);
			final Server jettyServer;
			try {
				jettyServer = ServerCommandLine.startServer (jettyProperties);
			} catch (final Throwable exception) {
				this.component.terminate ();
				this.exceptions.traceIgnoredException (exception, "error encountered while starting Jetty; terminating!");
				throw (new IllegalStateException ());
			}
			this.jettyServer = jettyServer;
		}
	}
	
	final void stopJetty ()
	{
		synchronized (this.monitor) {
			try {
				this.jettyServer.stop ();
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "error encountered while stopping Jetty; ignoring!");
			}
			this.jettyServer = null;
		}
	}
	
	private Component component;
	private final TranscriptExceptionTracer exceptions;
	private Server jettyServer;
	private final Monitor monitor;
	private final IdentityHashMap<ComponentCallReference, OutcomeTrigger<ComponentCallReply>> pendingCallReturnFutures = new IdentityHashMap<ComponentCallReference, OutcomeFuture.OutcomeTrigger<ComponentCallReply>> ();
	private ComponentCallReference pendingReference;
	private Status status;
	private final Transcript transcript;
	
	private static enum Status
	{
		Terminated,
		Unregistered,
		WaitingInitialized,
		WaitingRabbitmqResolveReturn,
		WaitingRegistered;
	}
}
