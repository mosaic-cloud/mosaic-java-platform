
package eu.mosaic_cloud.components.core;


import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;


public final class ComponentContext
		extends Object
{
	private ComponentContext (final ComponentController component, final CallbackReactor reactor, final ThreadingContext threading)
	{
		super ();
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (reactor);
		Preconditions.checkNotNull (threading);
		this.component = component;
		this.reactor = reactor;
		this.threading = threading;
	}
	
	public final ComponentController component;
	public final CallbackReactor reactor;
	public final ThreadingContext threading;
	
	public static final ComponentContext create (final ComponentController component, final CallbackReactor reactor, final ThreadingContext threading)
	{
		return (new ComponentContext (component, reactor, threading));
	}
}
