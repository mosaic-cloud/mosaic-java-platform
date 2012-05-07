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
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


public class PojoDataEncoder<TData extends Object>
		implements
			DataEncoder<TData>
{
	public PojoDataEncoder (final Class<TData> dataClass, final ExceptionTracer exceptions)
	{
		super ();
		Preconditions.checkNotNull (dataClass);
		this.dataClass = dataClass;
		this.transcirpt = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcirpt, exceptions);
	}
	
	@Override
	public TData decode (final byte[] dataBytes, final EncodingMetadata metadata)
			throws EncodingException
	{
		Preconditions.checkNotNull (dataBytes);
		Preconditions.checkNotNull (metadata);
		this.checkMetadata (metadata);
		final TData object;
		try {
			object = this.dataClass.cast (SerDesUtils.toObject (dataBytes));
		} catch (final IOException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected i/o exception", exception));
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected abnormal exception", exception));
		}
		return (object);
	}
	
	@Override
	public byte[] encode (final TData data, final EncodingMetadata metadata)
			throws EncodingException
	{
		Preconditions.checkNotNull (metadata);
		this.checkMetadata (metadata);
		final byte[] dataBytes;
		try {
			dataBytes = SerDesUtils.pojoToBytes (data);
		} catch (final IOException exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected i/o exception", exception));
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected abnormal exception", exception));
		}
		return (dataBytes);
	}
	
	private void checkMetadata (final EncodingMetadata metadata)
			throws EncodingException
	{
		if (metadata.contentType == null) {
			this.transcirpt.traceWarning ("decoding binary data with a `null` content-type; ignoring!");
		} else if (!PojoDataEncoder.EXPECTED_CONTENT_TYPE.equals (metadata.contentType)) {
			this.transcirpt.traceError ("encoding / decoding binary data with an unexpected `%s` content-type; throwing!", metadata.contentType);
			throw (new EncodingException (String.format ("unexpected content-type: `%s`", metadata.contentType)));
		}
		if ((metadata.contentEncoding != null) && !PojoDataEncoder.EXPECTED_CONTENT_ENCODING.equals (metadata.contentEncoding)) {
			this.transcirpt.traceError ("encoding / decoding binary data with an unexpected `%s` content-encoding; throwing!", metadata.contentType);
			throw (new EncodingException (String.format ("unexpected content-encoding: `%s`", metadata.contentEncoding)));
		}
	}
	
	public static <TData extends Object> PojoDataEncoder<TData> create (final Class<TData> dataClass)
	{
		return (new PojoDataEncoder<TData> (dataClass, FallbackExceptionTracer.defaultInstance));
	}
	
	protected final Class<TData> dataClass;
	protected final TranscriptExceptionTracer exceptions;
	protected final Transcript transcirpt;
	public static final String EXPECTED_CONTENT_ENCODING = "identity";
	public static final String EXPECTED_CONTENT_TYPE = "application/x-java-serialized-object";
}
