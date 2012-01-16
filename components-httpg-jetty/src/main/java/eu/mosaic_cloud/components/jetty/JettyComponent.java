
package eu.mosaic_cloud.components.jetty;


import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;


public final class JettyComponent
		extends Object
{
	private JettyComponent ()
	{
		super ();
	}
	
	public final Future<ComponentCallReply> call (final ComponentIdentifier component, final ComponentCallRequest request)
	{
		final JettyComponentCallbacks callbacks = JettyComponentContext.callbacks;
		Preconditions.checkState (callbacks != null);
		return (callbacks.call (component, request));
	}
	
	public final Future<ComponentCallReply> call (final String component, final String operation, final Object inputs)
	{
		return (this.call (ComponentIdentifier.resolve (component), ComponentCallRequest.create (operation, inputs, ComponentCallReference.create ())));
	}
	
	public final void cast (final ComponentIdentifier component, final ComponentCastRequest request)
	{
		final JettyComponentCallbacks callbacks = JettyComponentContext.callbacks;
		Preconditions.checkState (callbacks != null);
		callbacks.cast (component, request);
	}
	
	public final void cast (final String component, final String operation, final Object inputs)
	{
		this.cast (ComponentIdentifier.resolve (component), ComponentCastRequest.create (operation, inputs));
	}
	
	public final ComponentIdentifier getSelf ()
	{
		final ComponentIdentifier identifier = JettyComponentContext.selfIdentifier;
		Preconditions.checkState (identifier != null);
		return (identifier);
	}
	
	public final boolean isActive ()
	{
		return (JettyComponentContext.callbacks != null);
	}
	
	public final void terminate ()
	{
		final JettyComponentCallbacks callbacks = JettyComponentContext.callbacks;
		Preconditions.checkState (callbacks != null);
		callbacks.terminate ();
	}
	
	public static final JettyComponent component = new JettyComponent ();
}
