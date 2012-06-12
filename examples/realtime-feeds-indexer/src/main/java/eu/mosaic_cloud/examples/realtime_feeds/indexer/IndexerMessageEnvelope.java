
package eu.mosaic_cloud.examples.realtime_feeds.indexer;


import java.util.UUID;

import eu.mosaic_cloud.platform.core.utils.MessageEnvelope;


public class IndexerMessageEnvelope
		extends MessageEnvelope
{
	public IndexerMessageEnvelope (final UUID correlation)
	{
		super ();
		this.correlation = correlation;
	}
	
	public UUID getCorrelation ()
	{
		return this.correlation;
	}
	
	public void setCorrelation (final UUID correlation)
	{
		this.correlation = correlation;
	}
	
	private UUID correlation;
}
