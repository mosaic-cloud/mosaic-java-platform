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

import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;


public class SerializedDataEncoder<TData extends Object>
		extends BaseDataEncoder<TData>
{
	protected SerializedDataEncoder (final Class<TData> dataClass, final boolean nullAllowed, final ExceptionTracer exceptions)
	{
		super (dataClass, nullAllowed, SerializedDataEncoder.EXPECTED_ENCODING_METADATA, exceptions);
	}
	
	@Override
	protected TData decodeActual (final byte[] dataBytes, final EncodingMetadata metadata)
			throws EncodingException
	{
		try {
			return (this.dataClass.cast (SerDesUtils.toObject (dataBytes)));
		} catch (final ClassNotFoundException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected class loader exception", exception));
		} catch (final IOException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected i/o exception", exception));
		}
	}
	
	@Override
	protected byte[] encodeActual (final TData data, final EncodingMetadata metadata)
			throws EncodingException
	{
		try {
			return (SerDesUtils.pojoToBytes (data));
		} catch (final IOException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected i/o exception", exception));
		}
	}
	
	public static <TData extends Object> SerializedDataEncoder<TData> create (final Class<TData> dataClass)
	{
		return (new SerializedDataEncoder<TData> (dataClass, false, FallbackExceptionTracer.defaultInstance));
	}
	
	public static <TData extends Object> SerializedDataEncoder<TData> create (final Class<TData> dataClass, final boolean nullAllowed)
	{
		return (new SerializedDataEncoder<TData> (dataClass, nullAllowed, FallbackExceptionTracer.defaultInstance));
	}
	
	public static final EncodingMetadata EXPECTED_ENCODING_METADATA = new EncodingMetadata ("application/x-java-serialized-object", "identity");
}
