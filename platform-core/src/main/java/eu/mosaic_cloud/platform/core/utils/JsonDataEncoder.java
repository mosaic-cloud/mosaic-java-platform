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

import org.json.JSONException;
import org.json.JSONObject;


public class JsonDataEncoder<TData extends Object>
		extends BaseDataEncoder<TData>
{
	protected JsonDataEncoder (final Class<TData> dataClass, final boolean nullAllowed, final ExceptionTracer exceptions)
	{
		super (dataClass, nullAllowed, JsonDataEncoder.EXPECTED_ENCODING_METADATA, exceptions);
	}
	
	@Override
	protected TData decodeActual (final byte[] dataBytes, final EncodingMetadata metadata)
			throws EncodingException
	{
		try {
			final TData data;
			if (this.dataClass == JSONObject.class)
				data = this.dataClass.cast (SerDesUtils.jsonToRawObject (dataBytes));
			else
				data = this.dataClass.cast (SerDesUtils.jsonToObject (dataBytes, this.dataClass));
			return (data);
		} catch (final JSONException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("JSON encoding exception", exception));
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
			final byte[] dataBytes;
			if (this.dataClass == JSONObject.class)
				dataBytes = SerDesUtils.toJsonBytes ((JSONObject) data);
			else
				dataBytes = SerDesUtils.toJsonBytes (data);
			return (dataBytes);
		} catch (final JSONException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("JSON encoding exception", exception));
		} catch (final IOException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected i/o exception", exception));
		}
	}
	
	public static JsonDataEncoder<JSONObject> create ()
	{
		return (new JsonDataEncoder<JSONObject> (JSONObject.class, false, FallbackExceptionTracer.defaultInstance));
	}
	
	public static <TData extends Object> JsonDataEncoder<TData> create (final Class<TData> dataClass)
	{
		return (new JsonDataEncoder<TData> (dataClass, false, FallbackExceptionTracer.defaultInstance));
	}
	
	public static <TData extends Object> JsonDataEncoder<TData> create (final Class<TData> dataClass, final boolean nullAllowed)
	{
		return (new JsonDataEncoder<TData> (dataClass, nullAllowed, FallbackExceptionTracer.defaultInstance));
	}
	
	public static final JsonDataEncoder<JSONObject> DEFAULT_INSTANCE = JsonDataEncoder.create ();
	public static final EncodingMetadata EXPECTED_ENCODING_METADATA = new EncodingMetadata ("application/json", "identity");
}
