
package eu.mosaic_cloud.platform.implementation.v2.connectors.core.tests;


import java.util.HashMap;

import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.ChannelFactory;
import eu.mosaic_cloud.interoperability.core.ChannelResolver;
import eu.mosaic_cloud.interoperability.core.ResolverCallbacks;
import eu.mosaic_cloud.platform.v2.connectors.component.ComponentConnector;
import eu.mosaic_cloud.platform.v2.connectors.core.ConnectorEnvironment;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.implementations.basic.BasicCallbackReactor;
import eu.mosaic_cloud.tools.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.tools.threading.implementations.basic.BasicThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import org.junit.Assert;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;


public abstract class BaseConnectorTestEnvironment
{
	protected BaseConnectorTestEnvironment () {
		super ();
		this.transcript = Transcript.create (this);
		this.exceptionsQueue = QueueingExceptionTracer.create (NullExceptionTracer.defaultInstance);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript, this.exceptionsQueue);
		this.threading = BasicThreadingContext.create (this, this.exceptions, this.exceptions.catcher);
		this.callbacks = BasicCallbackReactor.create (this.threading, this.exceptions);
	}
	
	public void destroy () {
		this.destroy_ ();
		Assert.assertTrue (this.callbacks.destroy (this.poolTimeout));
		Assert.assertTrue (this.threading.destroy (this.poolTimeout));
		Assert.assertNull (this.exceptionsQueue.queue.poll ());
	}
	
	public ChannelFactory getChannelFactory (final String descriptor) {
		Preconditions.checkNotNull (descriptor);
		final ChannelFactory existingFactory = this.channelFactories.get (descriptor);
		if (existingFactory != null)
			return (existingFactory);
		final ChannelFactory factory = this.provideChannelFactory (descriptor);
		Preconditions.checkState (factory != null);
		this.channelFactories.put (descriptor, factory);
		return (factory);
	}
	
	public ChannelResolver getChannelResolver (final String descriptor) {
		Preconditions.checkNotNull (descriptor);
		final ChannelResolver existingResolver = this.channelResolvers.get (descriptor);
		if (existingResolver != null)
			return (existingResolver);
		final ChannelResolver resolver = this.provideChannelResolver (descriptor);
		Preconditions.checkState (resolver != null);
		this.channelResolvers.put (descriptor, resolver);
		return (resolver);
	}
	
	public ComponentConnector getComponentConnector (final String descriptor) {
		Preconditions.checkNotNull (descriptor);
		final ComponentConnector existingConnector = this.componentConnectors.get (descriptor);
		if (existingConnector != null)
			return (existingConnector);
		final ComponentConnector connector = this.provideComponentConnector (descriptor);
		Preconditions.checkState (connector != null);
		this.componentConnectors.put (descriptor, connector);
		return (connector);
	}
	
	public ConnectorEnvironment getConnectorEnvironment (final String descriptor) {
		final ConnectorEnvironment existingEnvironment = this.connectorEnvironments.get (descriptor);
		if (existingEnvironment != null)
			return (existingEnvironment);
		final ChannelFactory channelFactory = this.getChannelFactory (descriptor);
		final ChannelResolver channelResolver = this.getChannelResolver (descriptor);
		final ComponentConnector componentConnector = this.getComponentConnector (descriptor);
		final ConnectorEnvironment environment = ConnectorEnvironment.create (this.callbacks, this.threading, this.exceptions, channelFactory, channelResolver, componentConnector);
		this.connectorEnvironments.put (descriptor, environment);
		return (environment);
	}
	
	public void initialize () {
		this.threading.initialize ();
		this.callbacks.initialize ();
		this.initialize_ ();
	}
	
	protected void destroy_ () {}
	
	protected void initialize_ () {}
	
	protected ChannelFactory provideChannelFactory (@SuppressWarnings ("unused") final String descriptor) {
		return (null);
	}
	
	protected ChannelResolver provideChannelResolver (@SuppressWarnings ("unused") final String descriptor) {
		return (null);
	}
	
	protected ComponentConnector provideComponentConnector (@SuppressWarnings ("unused") final String descriptor) {
		return (null);
	}
	
	protected void registerChannel (final String descriptor, final Channel channel) {
		Preconditions.checkNotNull (descriptor);
		Preconditions.checkNotNull (channel);
		Preconditions.checkState (!this.channelFactories.containsKey (descriptor));
		final ChannelFactory factory = new ChannelFactory () {
			@Override
			public final Channel create () {
				return (channel);
			}
		};
		this.channelFactories.put (descriptor, factory);
	}
	
	protected void registerChannelResolution (final String descriptor, final String expectedTarget, final String identity, final String endpoint) {
		Preconditions.checkNotNull (descriptor);
		Preconditions.checkNotNull (expectedTarget);
		Preconditions.checkNotNull (identity);
		Preconditions.checkNotNull (endpoint);
		Preconditions.checkState (!this.channelResolvers.containsKey (descriptor));
		final ChannelResolver resolver = new ChannelResolver () {
			@Override
			public final void resolve (final String target, final ResolverCallbacks callbacks) {
				Preconditions.checkArgument (Objects.equal (expectedTarget, target));
				callbacks.resolved (this, expectedTarget, identity, endpoint);
			}
		};
		this.channelResolvers.put (descriptor, resolver);
	}
	
	protected void registerComponentCallReturn (final String descriptor, final ComponentIdentifier expectedComponent, final String expectedOperation, final Object expectedInputs, final Object outputs) {
		Preconditions.checkNotNull (descriptor);
		Preconditions.checkNotNull (expectedComponent);
		Preconditions.checkNotNull (expectedOperation);
		Preconditions.checkState (!this.componentConnectors.containsKey (descriptor));
		final ComponentConnector connector = new ComponentConnector () {
			@Override
			public CallbackCompletion<ComponentResourceDescriptor> acquire (final ComponentResourceSpecification resource) {
				return (CallbackCompletion.createFailure (new UnsupportedOperationException ()));
			}
			
			@Override
			public <TInputs, TOutputs> CallbackCompletion<TOutputs> call (final ComponentIdentifier component, final String operation, final TInputs inputs, final Class<TOutputs> outputsClass) {
				if (Objects.equal (expectedComponent, component) && Objects.equal (expectedOperation, operation) && Objects.equal (expectedInputs, inputs))
					return (CallbackCompletion.createOutcome (outputsClass.cast (outputs)));
				return (CallbackCompletion.createFailure (new UnsupportedOperationException ()));
			}
			
			@Override
			public <TInputs> CallbackCompletion<Void> cast (final ComponentIdentifier component, final String operation, final TInputs inputs) {
				return (CallbackCompletion.createFailure (new UnsupportedOperationException ()));
			}
			
			@Override
			public CallbackCompletion<Void> destroy () {
				return (CallbackCompletion.createFailure (new UnsupportedOperationException ()));
			}
			
			@Override
			public CallbackCompletion<Void> initialize () {
				return (CallbackCompletion.createFailure (new UnsupportedOperationException ()));
			}
		};
		this.componentConnectors.put (descriptor, connector);
	}
	
	public final BasicCallbackReactor callbacks;
	public final TranscriptExceptionTracer exceptions;
	public final QueueingExceptionTracer exceptionsQueue;
	public final long poolTimeout = -1;
	public final BasicThreadingContext threading;
	public final Transcript transcript;
	private HashMap<String, ChannelFactory> channelFactories;
	private HashMap<String, ChannelResolver> channelResolvers;
	private HashMap<String, ComponentConnector> componentConnectors;
	private HashMap<String, ConnectorEnvironment> connectorEnvironments;
}
