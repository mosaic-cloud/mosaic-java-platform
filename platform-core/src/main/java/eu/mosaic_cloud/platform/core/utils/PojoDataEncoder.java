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


import java.io.IOException;

import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;


public class PojoDataEncoder<T extends Object>
		implements
			DataEncoder<T>
{
	public PojoDataEncoder (final Class<T> dataClass)
	{
		this.dataClass = dataClass;
	}
	
	@Override
	public T decode (final byte[] dataBytes)
	{
		T object = null; // NOPMD by georgiana on 10/12/11 5:03 PM
		try {
			object = this.dataClass.cast (SerDesUtils.toObject (dataBytes));
		} catch (final IOException e) {
			ExceptionTracer.traceIgnored (e);
		} catch (final ClassNotFoundException e) {
			ExceptionTracer.traceIgnored (e);
		}
		return object;
	}
	
	@Override
	public byte[] encode (final T data)
			throws EncodingException
	{
		try {
			return SerDesUtils.pojoToBytes (data);
		} catch (final IOException e) {
			throw new EncodingException ("Java object cannot be serialized", e);
		}
	}
	
	private final Class<T> dataClass;
}
