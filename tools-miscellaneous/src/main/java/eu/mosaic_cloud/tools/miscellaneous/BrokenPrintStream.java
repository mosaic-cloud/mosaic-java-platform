/*
 * #%L
 * mosaic-tools-miscellaneous
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.tools.miscellaneous;


import java.io.PrintStream;
import java.util.Locale;


public final class BrokenPrintStream
		extends PrintStream
{
	private BrokenPrintStream ()
	{
		super (BrokenOutputStream.defaultInstance);
	}
	
	@Override
	public PrintStream append (final char c)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public PrintStream append (final CharSequence csq)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public PrintStream append (final CharSequence csq, final int start, final int end)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public boolean checkError ()
	{
		throw (new UnsupportedOperationException ());
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
	public PrintStream format (final Locale l, final String format, final Object ... args)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public PrintStream format (final String format, final Object ... args)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final boolean b)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final char c)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final char[] s)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final double d)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final float f)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final int i)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final long l)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final Object obj)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void print (final String s)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public PrintStream printf (final Locale l, final String format, final Object ... args)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public PrintStream printf (final String format, final Object ... args)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final boolean x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final char x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final char[] x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final double x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final float x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final int x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final long x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final Object x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void println (final String x)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void write (final byte[] b)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void write (final byte[] buf, final int off, final int len)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public void write (final int b)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	protected void clearError ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	protected void setError ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	public static final BrokenPrintStream create ()
	{
		return (new BrokenPrintStream ());
	}
	
	public static final BrokenPrintStream defaultInstance = BrokenPrintStream.create ();
}
