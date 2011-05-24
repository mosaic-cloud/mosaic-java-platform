
package eu.mosaic_cloud.components.core;


import java.nio.ByteBuffer;


public interface ComponentCallContinuation
{
	public abstract void reply (final Object metaData, final ByteBuffer data);
}
