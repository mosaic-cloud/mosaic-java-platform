
package eu.mosaic_cloud.tools.miscellaneous;


import java.io.OutputStream;


public final class BrokenOutputStream
		extends OutputStream
{
	private BrokenOutputStream ()
	{
		super ();
	}
	
	@Override
	public void close ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void flush ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void write (final byte[] b)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void write (final byte[] b, final int off, final int len)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void write (final int b)
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final BrokenOutputStream create ()
	{
		return (new BrokenOutputStream ());
	}
	
	public static final BrokenOutputStream defaultInstance = BrokenOutputStream.create ();
}
