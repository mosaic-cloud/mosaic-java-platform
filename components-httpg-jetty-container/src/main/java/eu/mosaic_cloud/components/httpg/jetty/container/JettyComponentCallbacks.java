/*
 * #%L
 * mosaic-components-httpg-jetty-container
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.components.httpg.jetty.container;


import java.io.File;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.httpg.jetty.connector.ServerCommandLine;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture.Trigger;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.eclipse.jetty.server.Server;

import com.google.common.base.Preconditions;


public final class JettyComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler
{
	public JettyComponentCallbacks (final ComponentEnvironment context)
	{
		super ();
		this.monitor = Monitor.create (this);
		synchronized (this.monitor) {
			this.context = context;
			this.threading = this.context.threading;
			this.transcript = Transcript.create (this, true);
			this.exceptions = TranscriptExceptionTracer.create (this.transcript, this.context.exceptions);
			this.pendingCallReturnFutures = new IdentityHashMap<ComponentCallReference, DeferredFuture.Trigger<ComponentCallReply>> ();
			this.status = Status.WaitingRegistered;
			JettyComponentContext.selfIdentifier = this.context.identifier;
			JettyComponentContext.applicationWar = new File (this.context.supplementary.get ("war", String.class, "embedded"));
			Preconditions.checkArgument (JettyComponentContext.applicationWar != null, "missing `war` configuration...");
			if (!"embedded".equals (JettyComponentContext.applicationWar.getPath ())) {
				Preconditions.checkArgument (JettyComponentContext.applicationWar.isFile (), "invalid `war` file; (does not exist)");
				Preconditions.checkArgument (JettyComponentContext.applicationWar.canRead (), "invalid `war` file; (can not read)");
			}
			JettyComponentContext.callbacks = this;
			JettyComponent.create ();
		}
	}
	
	public final Future<ComponentCallReply> call (final ComponentIdentifier component, final ComponentCallRequest request)
	{
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (request);
		final DeferredFuture<ComponentCallReply> replyFuture = DeferredFuture.create (ComponentCallReply.class);
		synchronized (this.monitor) {
			Preconditions.checkState (this.component != null);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			Preconditions.checkArgument (!this.pendingCallReturnFutures.containsKey (request.reference));
			this.pendingCallReturnFutures.put (request.reference, replyFuture.trigger);
			this.component.call (component, request);
		}
		return (replyFuture);
	}
	
	@Override
	public final CallbackCompletion<Void> called (final ComponentController component, final ComponentCallRequest request)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			throw (new UnsupportedOperationException ());
		}
	}
	
	@Override
	public final CallbackCompletion<Void> callReturned (final ComponentController component, final ComponentCallReply reply)
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
							this.exceptions.traceIgnoredException (exception, "failed resolving RabbitMQ broker endpoint: `%s`; terminating!", reply.outputsOrError);
							this.terminate ();
							throw (new IllegalStateException ());
						}
						this.transcript.traceInformation ("resolved RabbitMQ on `%s:%d`", brokerIp, brokerPort);
						this.startJetty (brokerIp, brokerPort.intValue ());
						if (JettyComponentContext.selfGroup != null) {
							this.pendingReference = ComponentCallReference.create ();
							this.component.register (JettyComponentContext.selfGroup, this.pendingReference);
						}
					}
						break;
					default:
						throw (new IllegalStateException ());
				}
			else if (this.pendingCallReturnFutures.containsKey (reply.reference)) {
				final Trigger<ComponentCallReply> trigger = this.pendingCallReturnFutures.remove (reply.reference);
				trigger.triggerSucceeded (reply);
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
	public final CallbackCompletion<Void> casted (final ComponentController component, final ComponentCastRequest request)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
			throw (new UnsupportedOperationException ());
		}
	}
	
	@Override
	public final CallbackCompletion<Void> failed (final ComponentController component, final Throwable exception)
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
	public final void failedCallbacks (final Callbacks trigger, final Throwable exception)
	{
		this.failed (this.component, exception);
	}
	
	@Override
	public final CallbackCompletion<Void> initialized (final ComponentController component)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == null);
			Preconditions.checkState (this.status == Status.WaitingInitialized);
			this.component = component;
			final ComponentCallReference callReference = ComponentCallReference.create ();
			this.pendingReference = callReference;
			this.status = Status.WaitingRabbitmqResolveReturn;
			this.component.call (JettyComponentContext.brokerGroup, ComponentCallRequest.create ("mosaic-rabbitmq:get-broker-endpoint", null, callReference));
		}
		return (null);
	}
	
	@Override
	public final void registeredCallbacks (final Callbacks trigger, final CallbackIsolate isolate)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == null);
			Preconditions.checkState (this.status == Status.WaitingRegistered);
			this.status = Status.WaitingInitialized;
		}
	}
	
	@Override
	public final CallbackCompletion<Void> registerReturned (final ComponentController component, final ComponentCallReference reference, final boolean ok)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component == component);
			if (this.pendingReference == reference) {
				this.pendingReference = null;
				if (!ok) {
					this.transcript.traceError ("failed registering to group; terminating!");
					this.component.terminate ();
					throw (new IllegalStateException ());
				}
				this.transcript.traceInformation ("registered to group");
			} else
				throw (new IllegalStateException ());
		}
		return (null);
	}
	
	public final void terminate ()
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.component != null);
			this.component.terminate ();
		}
	}
	
	@Override
	public final CallbackCompletion<Void> terminated (final ComponentController component)
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
	public void unregisteredCallbacks (final Callbacks trigger)
	{
		synchronized (this.monitor) {
			Preconditions.checkState (this.status != Status.Unregistered);
			if (this.jettyServer != null)
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
			if (!"embedded".equals (JettyComponentContext.applicationWar.getPath ()))
				jettyProperties.setProperty ("webapp", JettyComponentContext.applicationWar.getAbsolutePath ());
			else
				jettyProperties.setProperty ("webapp", "embedded");
			jettyProperties.setProperty ("tmp", JettyComponentContext.applicationTemporary.getAbsolutePath ());
			jettyProperties.setProperty ("app-context", JettyComponentContext.applicationContextPath);
			jettyProperties.setProperty ("tmp", JettyComponentContext.applicationTemporary.getAbsolutePath ());
			final Server jettyServer;
			jettyServer = ServerCommandLine.createServer (jettyProperties, this.context.classLoader);
			this.jettyServer = jettyServer;
			this.jettyThread = Threading.createAndStartDaemonThread (this.threading, jettyServer, "jetty", new Runnable () {
				@Override
				public final void run ()
				{
					try {
						jettyServer.start ();
					} catch (final Throwable exception) {
						JettyComponentCallbacks.this.exceptions.traceIgnoredException (exception, "error encountered while starting Jetty; terminating!");
						JettyComponentCallbacks.this.terminate ();
					}
				}
			});
		}
	}
	
	final void stopJetty ()
	{
		synchronized (this.monitor) {
			try {
				if (this.jettyServer != null)
					this.jettyServer.stop ();
				Threading.join (this.jettyThread);
			} catch (final Throwable exception) {
				this.exceptions.traceIgnoredException (exception, "error encountered while stopping Jetty; ignoring!");
			}
			this.jettyServer = null;
			this.jettyThread = null;
		}
	}
	
	private ComponentController component;
	private ComponentEnvironment context;
	private final TranscriptExceptionTracer exceptions;
	private Server jettyServer;
	private Thread jettyThread;
	private final Monitor monitor;
	private final IdentityHashMap<ComponentCallReference, Trigger<ComponentCallReply>> pendingCallReturnFutures;
	private ComponentCallReference pendingReference;
	private Status status;
	private final ThreadingContext threading;
	private final Transcript transcript;
	
	private static enum Status
	{
		Terminated (),
		Unregistered (),
		WaitingInitialized (),
		WaitingRabbitmqResolveReturn (),
		WaitingRegistered ();
	}
}
