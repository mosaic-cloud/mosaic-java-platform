
package eu.mosaic_cloud.interoperability.core;


public interface MessageCoder
{
	public abstract Object decodeMessage (final byte[] buffer)
			throws Throwable;
	
	public abstract byte[] encodeMessage (final Object object)
			throws Throwable;
}
