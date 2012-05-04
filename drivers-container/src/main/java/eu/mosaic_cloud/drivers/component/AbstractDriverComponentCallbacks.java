/*
 * #%L
 * mosaic-drivers-container
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

package eu.mosaic_cloud.drivers.component;


import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentController;
import eu.mosaic_cloud.components.core.ComponentEnvironment;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.interoperability.core.SessionSpecification;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.tools.callbacks.core.CallbackIsolate;
import eu.mosaic_cloud.tools.callbacks.core.Callbacks;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;


/**
 * This callback class enables a resource driver to be exposed as a component.
 * Upon initialization it will look for the resource and will create a driver
 * object for the resource.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractDriverComponentCallbacks
		implements
			ComponentCallbacks,
			CallbackHandler
{
	protected AbstractDriverComponentCallbacks (final ComponentEnvironment context)
	{
		this.threading = context.threading;
		this.exceptions = context.exceptions;
		this.logger = Transcript.create (this, true).adaptAs (Logger.class);
	}
	
	@Override
	public CallbackCompletion<Void> casted (final ComponentController component, final ComponentCastRequest request)
	{
		Preconditions.checkState (this.component == component);
		Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
		throw new UnsupportedOperationException ();
	}
	
	@Override
	public CallbackCompletion<Void> failed (final ComponentController component, final Throwable exception)
	{
		Preconditions.checkState (this.component == component);
		Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
		if (this.stub != null) {
			this.stub.destroy ();
		}
		this.component = null; // NOPMD
		this.status = Status.Terminated;
		this.exceptions.trace (ExceptionResolution.Ignored, exception);
		return null;
	}
	
	@Override
	public final void failedCallbacks (final Callbacks trigger, final Throwable exception)
	{
		this.failed (this.component, exception);
	}
	
	@Override
	public final void registeredCallbacks (final Callbacks trigger, final CallbackIsolate isolate)
	{}
	
	public void terminate ()
	{
		Preconditions.checkState (this.component != null);
		this.component.terminate ();
	}
	
	@Override
	public CallbackCompletion<Void> terminated (final ComponentController component)
	{
		Preconditions.checkState (this.component == component);
		Preconditions.checkState ((this.status != Status.Terminated) && (this.status != Status.Unregistered));
		if (this.stub != null) {
			this.stub.destroy ();
			this.logger.trace ("Driver callbacks terminated.");
		}
		this.component = null; // NOPMD
		this.status = Status.Terminated;
		return null;
	}
	
	@Override
	public final void unregisteredCallbacks (final Callbacks trigger)
	{}
	
	protected ZeroMqChannel createDriverChannel (final String channelIdentifierProp, final String channelEndpointProp, final SessionSpecification role)
	{
		// NOTE: create stub and interop channel
		Preconditions.checkNotNull (this.driverConfiguration);
		final ZeroMqChannel driverChannel = ZeroMqChannel.create (ConfigUtils.resolveParameter (this.driverConfiguration, channelIdentifierProp, String.class, ""), this.threading, this.exceptions);
		driverChannel.register (role);
		driverChannel.accept (ConfigUtils.resolveParameter (this.driverConfiguration, channelEndpointProp, String.class, ""));
		return driverChannel;
	}
	
	protected IConfiguration getDriverConfiguration ()
	{
		return this.driverConfiguration;
	}
	
	protected void setDriverConfiguration (final IConfiguration driverConfiguration)
	{
		this.driverConfiguration = driverConfiguration;
	}
	
	protected ComponentController component;
	protected IConfiguration driverConfiguration;
	protected ExceptionTracer exceptions;
	protected Logger logger;
	protected ComponentCallReference pendingReference;
	protected ComponentIdentifier resourceGroup;
	protected ComponentIdentifier selfGroup;
	protected Status status;
	protected AbstractDriverStub stub;
	protected ThreadingContext threading;
	
	protected static enum Status
	{
		Created,
		Registered,
		Terminated,
		Unregistered,
		WaitingResourceResolved;
	}
}
