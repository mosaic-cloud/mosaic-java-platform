
package eu.mosaic_cloud.json.core;


import java.nio.ByteBuffer;


public interface JsonCoder
{
	public abstract Object decode (final ByteBuffer data)
			throws Throwable;
	
	public abstract ByteBuffer encode (final Object structure)
			throws Throwable;
}
