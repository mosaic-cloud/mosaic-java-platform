
package eu.mosaic_cloud.interoperability.core;


import java.nio.ByteBuffer;


public interface PayloadCoder
{
	public abstract Object decode (final ByteBuffer buffer)
			throws Throwable;
	
	public abstract ByteBuffer encode (final Object object)
			throws Throwable;
}
