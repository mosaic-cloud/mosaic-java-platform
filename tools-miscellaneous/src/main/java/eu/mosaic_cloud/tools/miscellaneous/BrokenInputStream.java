package eu.mosaic_cloud.tools.miscellaneous;

import java.io.InputStream;

public final class BrokenInputStream
		extends InputStream
{
	private BrokenInputStream ()
	{
		super ();
	}
	
	@Override
	public final int available ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final void close ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final void mark (final int readlimit)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final boolean markSupported ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final int read ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final int read (final byte[] b)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final int read (final byte[] b, final int off, final int len)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final void reset ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final long skip (final long n)
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final BrokenInputStream create ()
	{
		return (new BrokenInputStream ());
	}
	
	public static final BrokenInputStream defaultInstance = BrokenInputStream.create ();
}