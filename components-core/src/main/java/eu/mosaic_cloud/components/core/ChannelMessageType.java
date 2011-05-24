
package eu.mosaic_cloud.components.core;


import com.google.common.base.Preconditions;


public enum ChannelMessageType
{
	Exchange ("exchange");
	
	ChannelMessageType (final String identifier)
	{
		Preconditions.checkNotNull (identifier);
		this.identifier = identifier;
	}
	
	public final String identifier;
}
