
package eu.mosaic_cloud.components.implementations.basic;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;


public final class BasicComponentContainerPreMain
		extends Object
{
	private BasicComponentContainerPreMain ()
	{
		super ();
		throw (new UnsupportedOperationException ());
	}
	
	static {
		stdin = System.in;
		stdout = System.out;
		System.setIn (new ByteArrayInputStream (new byte[0]));
		System.setOut (System.err);
	}
	
	public static final void main (final String[] arguments)
			throws Exception
	{
		final Class<?> mainClass = BasicComponentContainerPreMain.class.getClassLoader ().loadClass (BasicComponentContainerPreMain.class.getName ().replace ("PreMain", "Main"));
		final Method mainMethod = mainClass.getMethod ("main", String[].class);
		mainMethod.invoke (null, new Object[] {arguments});
	}
	
	static final InputStream stdin;
	static final OutputStream stdout;
}
