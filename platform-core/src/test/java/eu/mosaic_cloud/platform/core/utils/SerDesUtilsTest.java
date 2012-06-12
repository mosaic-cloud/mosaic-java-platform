/*
 * #%L
 * mosaic-platform-core
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
