/*
 * #%L
 * mosaic-cloudlets
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

package eu.mosaic_cloud.cloudlets.implementation.cloudlet;


import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.cloudlets.core.ICloudletCallback;
import eu.mosaic_cloud.cloudlets.implementation.container.IComponentConnector;
import eu.mosaic_cloud.cloudlets.tools.ConfigProperties;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.connectors.tools.DefaultConnectorsFactory;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.configuration.PropertyTypeConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.DelegatingExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


/**
 * Implements a container holding a list of cloudlet instances. All instances
 * have the same cloudlet type.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class CloudletManager
{
	private CloudletManager (final IConfiguration configuration, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final IComponentConnector componentConnector, final ChannelFactory channelFactory, final ChannelResolver channelResolver)
	{
		super ();
		Preconditions.checkNotNull (configuration);
		Preconditions.checkNotNull (classLoader);
		Preconditions.checkNotNull (reactor);
		Preconditions.checkNotNull (threading);
		Preconditions.checkNotNull (exceptions);
		Preconditions.checkNotNull (componentConnector);
		Preconditions.checkNotNull (channelFactory);
		Preconditions.checkNotNull (channelResolver);
		synchronized (this.monitor) {
			this.transcript = Transcript.create (this, true);
			this.exceptions = TranscriptExceptionTracer.create (this.transcript, exceptions);
			this.configuration = configuration;
			this.reactor = reactor;
			this.threading = threading;
			this.componentConnector = componentConnector;
			this.channelFactory = channelFactory;
			this.channelResolver = channelResolver;
			this.cloudlets = new ConcurrentHashMap<Cloudlet<?>, Cloudlet<?>> ();
			this.classLoader = classLoader;
		}
		this.transcript.traceDebugging ("created cloudlet manager.");
		this.transcript.traceDebugging ("using the class-loader `%{object}`...", this.classLoader);
		this.transcript.traceDebugging ("using the callbacks reactor `%{object}`...", this.reactor);
		this.transcript.traceDebugging ("using the threading context `%{object}`...", this.threading);
		this.transcript.traceDebugging ("using the interoperability channel factory `%{object}`...", this.channelFactory);
		this.transcript.traceDebugging ("using the interoperability channel resolver `%{object}`...", this.channelResolver);
	}
	
	public final boolean createInstance ()
	{
		this.transcript.traceDebugging ("creating a new cloudlet...");
		synchronized (this.monitor) {
			final Cloudlet<?> cloudlet = this.createCloudletInstance ();
			// FIXME: this should be done asynchronously and we should check the
			//-- outcome...
			cloudlet.initialize ();
			this.cloudlets.put (cloudlet, cloudlet);
			this.transcript.traceDebugging ("created the cloudlet `%{object:identity}`.", cloudlet);
			return (true);
		}
	}
	
	public final void destroy ()
	{
		this.transcript.traceDebugging ("destroying all cloudlets...");
		synchronized (this.monitor) {
			while (!this.cloudlets.isEmpty ()) {
				this.destroyInstance ();
			}
		}
	}
	
	public final boolean destroyInstance ()
	{
		this.transcript.traceDebugging ("destroying a cloudlet...");
		synchronized (this.monitor) {
			final Iterator<Cloudlet<?>> cloudletIterator = this.cloudlets.keySet ().iterator ();
			while (true) {
				if (!cloudletIterator.hasNext ()) {
					this.transcript.traceDebugging ("destroying a cloudlet failed: no cloudlet is available; ignoring!");
					return (false);
				}
				final Cloudlet<?> cloudlet = cloudletIterator.next ();
				// FIXME: we should have some cloudlet observers to manage
				//-- this...
				switch (cloudlet.getState ()) {
					case DESTROYED :
					case DESTROYING :
					case FAILED :
						this.cloudlets.remove (cloudlet);
						continue;
					case ACTIVE :
					case INITIALIZING :
						break;
					default:
						throw (new AssertionError ());
				}
				// FIXME: this should be done asynchronously and we should check
				//-- the outcome...
				cloudlet.destroy ();
				this.cloudlets.remove (cloudlet);
				this.transcript.traceDebugging ("destroyed the cloudlet `%{object:identity}`.", cloudlet);
				return (true);
			}
		}
	}
	
	private final Cloudlet<?> createCloudletInstance ()
	{
		final Class<?> cloudletCallbacksClass = this.resolveCloudletCallbacksClass ();
		final Class<?> cloudletContextClass = this.resolveCloudletStateClass ();
		final IConfiguration cloudletConfiguration = this.resolveCloudletConfiguration ();
		// FIXME: Currently exceptions from cloudlets are not deferred anywhere.
		//-- Thus any deferred exception should be treated as an ignored one.
		final ExceptionTracer exceptions = new CloudletExceptionTracer ();
		final ConnectorEnvironment connectorEnvironment = ConnectorEnvironment.create (this.reactor, this.threading, exceptions, this.channelFactory, this.channelResolver);
		final IConnectorsFactory connectorFactory = DefaultConnectorsFactory.create (null, connectorEnvironment);
		final CloudletEnvironment environment = CloudletEnvironment.create (cloudletConfiguration, cloudletCallbacksClass, cloudletContextClass, this.classLoader, connectorFactory, connectorEnvironment, this.componentConnector, this.reactor, this.threading, exceptions);
		final Cloudlet<?> cloudlet = Cloudlet.create (environment);
		return (cloudlet);
	}
	
	private final Class<?> resolveCloudletCallbacksClass ()
	{
		this.transcript.traceDebugging ("resolving the cloudlet callbacks class name...");
		final String className = ConfigUtils.resolveParameter (this.configuration, ConfigProperties.getString ("CloudletComponent.8"), String.class, null); // $NON-NLS-1$
		Preconditions.checkNotNull (className, "unknown cloudlet callbacks class");
		this.transcript.traceDebugging ("resolving the cloudlet callbacks class `%s`...", className);
		final Class<?> clasz;
		try {
			clasz = this.classLoader.loadClass (className);
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new IllegalArgumentException ("error encountered while loading cloudlet callbacks class", exception));
		}
		Preconditions.checkArgument (ICloudletCallback.class.isAssignableFrom (clasz), "invalid cloudlet callbacks class (must implement `ICloudletCallback`)");
		return (clasz);
	}
	
	private final IConfiguration resolveCloudletConfiguration ()
	{
		this.transcript.traceDebugging ("resolving the cloudlet configuration...");
		final String configurationDescriptor = ConfigUtils.resolveParameter (this.configuration, ConfigProperties.getString ("CloudletComponent.10"), String.class, null); // $NON-NLS-1$
		Preconditions.checkNotNull (configurationDescriptor, "unknown cloudlet configuration descriptor");
		this.transcript.traceDebugging ("resolving the cloudlet configuration `%s`...", configurationDescriptor);
		final IConfiguration configuration;
		try {
			configuration = PropertyTypeConfiguration.create (this.classLoader, configurationDescriptor);
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new IllegalArgumentException ("error encountered while loading cloudlet configuration", exception));
		}
		return (configuration);
	}
	
	private final Class<?> resolveCloudletStateClass ()
	{
		this.transcript.traceDebugging ("resolving the cloudlet state class name...");
		final String className = ConfigUtils.resolveParameter (this.configuration, ConfigProperties.getString ("CloudletComponent.9"), String.class, null); // $NON-NLS-1$
		Preconditions.checkNotNull (className, "unknown cloudlet context class");
		this.transcript.traceDebugging ("resolving the cloudlet state class `%s`...", className);
		final Class<?> clasz;
		try {
			clasz = this.classLoader.loadClass (className);
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new IllegalArgumentException ("error encountered while loading cloudlet context class", exception));
		}
		return (clasz);
	}
	
	public static final CloudletManager create (final IConfiguration configuration, final ClassLoader classLoader, final CallbackReactor reactor, final ThreadingContext threading, final ExceptionTracer exceptions, final IComponentConnector componentConnector, final ChannelFactory channelFactory, final ChannelResolver channelResolver)
	{
		return (new CloudletManager (configuration, classLoader, reactor, threading, exceptions, componentConnector, channelFactory, channelResolver));
	}
	
	private final ChannelFactory channelFactory;
	private final ChannelResolver channelResolver;
	private final ClassLoader classLoader;
	private final ConcurrentHashMap<Cloudlet<?>, Cloudlet<?>> cloudlets;
	private final IComponentConnector componentConnector;
	private final IConfiguration configuration;
	private final TranscriptExceptionTracer exceptions;
	private final Monitor monitor = Monitor.create (this);
	private final CallbackReactor reactor;
	private final ThreadingContext threading;
	private final Transcript transcript;
	
	final class CloudletExceptionTracer
			extends DelegatingExceptionTracer
	{
		CloudletExceptionTracer ()
		{
			super (CloudletManager.this.exceptions);
		}
		
		@Override
		protected void trace_ (final ExceptionResolution resolution, final Throwable exception)
		{
			switch (resolution) {
				case Deferred :
				case Ignored :
					CloudletManager.this.transcript.trace (ExceptionResolution.Ignored, exception);
					break;
				default:
					break;
			}
		}
		
		@Override
		protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String message)
		{
			switch (resolution) {
				case Deferred :
				case Ignored :
					CloudletManager.this.transcript.trace (ExceptionResolution.Ignored, exception, message);
					break;
				default:
					break;
			}
		}
		
		@Override
		protected void trace_ (final ExceptionResolution resolution, final Throwable exception, final String format, final Object ... tokens)
		{
			switch (resolution) {
				case Deferred :
				case Ignored :
					CloudletManager.this.transcript.trace (ExceptionResolution.Ignored, exception, format, tokens);
					break;
				default:
					break;
			}
		}
	}
}
