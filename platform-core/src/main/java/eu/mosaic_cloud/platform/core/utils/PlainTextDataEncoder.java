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


import java.nio.charset.Charset;

import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;


public class PlainTextDataEncoder
		extends BaseDataEncoder<String>
{
	protected PlainTextDataEncoder (final Charset charset, final ExceptionTracer exceptions)
	{
		super (String.class, false, PlainTextDataEncoder.EXPECTED_ENCODING_METADATA, exceptions);
		Preconditions.checkNotNull (charset);
		this.charset = charset;
	}
	
	@Override
	protected String decodeActual (final byte[] dataBytes, final EncodingMetadata metadata)
			throws EncodingException
	{
		try {
			return (new String (dataBytes, this.charset));
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected abnormal exception", exception));
		}
	}
	
	@Override
	protected byte[] encodeActual (final String data, final EncodingMetadata metadata)
			throws EncodingException
	{
		try {
			return (data.getBytes (this.charset));
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected abnormal exception", exception));
		}
	}
	
	public static PlainTextDataEncoder create ()
	{
		return (new PlainTextDataEncoder (Charsets.UTF_8, FallbackExceptionTracer.defaultInstance));
	}
	
	public static PlainTextDataEncoder create (final Charset charset)
	{
		return (new PlainTextDataEncoder (charset, FallbackExceptionTracer.defaultInstance));
	}
	
	public static PlainTextDataEncoder create (final String charset)
	{
		return (new PlainTextDataEncoder (Charset.forName (charset), FallbackExceptionTracer.defaultInstance));
	}
	
	protected final Charset charset;
	public static final PlainTextDataEncoder DEFAULT_INSTANCE = PlainTextDataEncoder.create ();
	public static final EncodingMetadata EXPECTED_ENCODING_METADATA = new EncodingMetadata ("text/plain", "identity");
}
