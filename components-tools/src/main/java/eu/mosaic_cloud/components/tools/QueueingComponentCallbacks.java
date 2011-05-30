
package eu.mosaic_cloud.components.tools;


import java.util.concurrent.BlockingQueue;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentMessage;


public class QueueingComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler<ComponentCallbacks>
{
	public QueueingComponentCallbacks (final Component component, final BlockingQueue<ComponentMessage> queue)
	{
		super ();
		Preconditions.checkNotNull (component);
		Preconditions.checkNotNull (queue);
		this.component = component;
		this.queue = queue;
	}
	
	@Override
	public CallbackReference called (final Component component, final ComponentCallRequest request)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (request);
		this.queue.add (request);
		return (null);
	}
	
	@Override
	public CallbackReference casted (final Component component, final ComponentCastRequest request)
	{
		Preconditions.checkArgument (this.component == component);
		Preconditions.checkNotNull (request);
		this.queue.add (request);
		return (null);
	}
	
	@Override
	public void deassigned (final ComponentCallbacks trigger, final ComponentCallbacks newCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public CallbackReference failed (final Component component, final Throwable exception)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	public void initialize ()
	{
		this.component.assign (this);
	}
	
	@Override
	public CallbackReference initialized (final Component component)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	@Override
	public void reassigned (final ComponentCallbacks trigger, final ComponentCallbacks oldCallbacks)
	{
		Preconditions.checkState (false);
	}
	
	@Override
	public void registered (final ComponentCallbacks trigger)
	{}
	
	@Override
	public CallbackReference replied (final Component component, final ComponentCallReply reply)
	{
		Preconditions.checkArgument (this.component == component);
		this.queue.add (reply);
		return (null);
	}
	
	@Override
	public CallbackReference terminated (final Component component)
	{
		Preconditions.checkArgument (this.component == component);
		return (null);
	}
	
	@Override
	public void unregistered (final ComponentCallbacks trigger)
	{}
	
	protected final Component component;
	protected final BlockingQueue<ComponentMessage> queue;
}
