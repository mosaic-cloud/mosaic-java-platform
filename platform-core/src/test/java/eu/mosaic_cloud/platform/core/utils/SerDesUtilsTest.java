
package eu.mosaic_cloud.platform.core.utils;


import java.io.Serializable;

import org.junit.Test;


public class SerDesUtilsTest
{
	@Test
	public void testArray ()
			throws Throwable
	{
		SerDesUtils.toObject (SerDesUtils.pojoToBytes (new SomeClass ()));
	}
	
	public static class SomeClass
			implements
				Serializable
	{
		private static final long serialVersionUID = 1L;
		public Object[] someField = new Object[] {1, 2, true, ""};
	}
}
