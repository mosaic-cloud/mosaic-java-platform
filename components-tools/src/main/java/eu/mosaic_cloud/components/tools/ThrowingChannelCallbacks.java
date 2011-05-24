
package eu.mosaic_cloud.components.tools;


import eu.mosaic_cloud.components.core.Channel;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelMessage;


public class ThrowingChannelCallbacks
		extends Object
		implements
			ChannelCallbacks
{
	@Override
	public void closed (final Channel channel)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void failed (final Channel channel, final Throwable exception)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void opened (final Channel channel)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void received (final Channel channel, final ChannelMessage message)
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final ThrowingChannelCallbacks defaultInstance = new ThrowingChannelCallbacks ();
}
