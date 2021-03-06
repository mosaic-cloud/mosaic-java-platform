/*
 * #%L
 * mosaic-platform-core
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.platform.implementation.v2.serialization;


import eu.mosaic_cloud.platform.v2.serialization.DataEncoder;
import eu.mosaic_cloud.platform.v2.serialization.EncodingException;
import eu.mosaic_cloud.platform.v2.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

import com.google.common.base.Preconditions;


public abstract class BaseDataEncoder<TData extends Object>
			implements
				DataEncoder<TData>
{
	protected BaseDataEncoder (final Class<TData> dataClass, final boolean nullAllowed, final EncodingMetadata expectedEncodingMetadata, final ExceptionTracer exceptions) {
		super ();
		Preconditions.checkNotNull (dataClass);
		Preconditions.checkNotNull (expectedEncodingMetadata);
		this.dataClass = dataClass;
		this.nullAllowed = nullAllowed;
		this.expectedEncodingMetadata = expectedEncodingMetadata;
		this.transcirpt = Transcript.create (this, true);
		this.exceptions = TranscriptExceptionTracer.create (this.transcirpt, exceptions);
	}
	
	@Override
	public final TData decode (final byte[] dataBytes, final EncodingMetadata metadata)
				throws EncodingException {
		Preconditions.checkNotNull (metadata);
		this.checkMetadata (metadata);
		if (dataBytes == null) {
			throw (new EncodingException ("unexpected `null` data bytes"));
		}
		final TData data;
		try {
			data = this.decodeActual (dataBytes, metadata);
		} catch (final EncodingException exception) {
			throw (exception);
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected abnormal exception", exception));
		}
		if (data != null) {
			if (!this.dataClass.isInstance (data)) {
				throw (new EncodingException (String.format ("unexpected data class `%s`", data.getClass ().getName ())));
			}
		} else {
			if (!this.nullAllowed) {
				throw (new EncodingException ("disallowed `null` data"));
			}
		}
		return (data);
	}
	
	@Override
	public final EncodeOutcome encode (final TData data, final EncodingMetadata metadata)
				throws EncodingException {
		final EncodingMetadata metadataActual = (metadata != null) ? metadata : this.expectedEncodingMetadata;
		Preconditions.checkNotNull (metadataActual);
		this.checkMetadata (metadataActual);
		if (data != null) {
			if (!this.dataClass.isInstance (data)) {
				throw (new EncodingException (String.format ("unexpected data class `%s`", data.getClass ().getName ())));
			}
		} else {
			if (!this.nullAllowed) {
				throw (new EncodingException ("disallowed `null` data"));
			}
		}
		final byte[] dataBytes;
		try {
			dataBytes = this.encodeActual (data, metadataActual);
		} catch (final Throwable exception) {
			this.exceptions.traceHandledException (exception);
			throw (new EncodingException ("unexpected abnormal exception", exception));
		}
		if (dataBytes == null) {
			throw (new EncodingException ("unexpected `null` data bytes"));
		}
		final EncodeOutcome outcome = new EncodeOutcome (dataBytes, metadataActual);
		return (outcome);
	}
	
	@Override
	public EncodingMetadata getExpectedEncodingMetadata () {
		return (this.expectedEncodingMetadata);
	}
	
	protected void checkMetadata (final EncodingMetadata metadata)
				throws EncodingException {
		if (!this.expectedEncodingMetadata.hasSameContentEncoding (EncodingMetadata.ANY)) {
			if (!metadata.hasContentType ()) {
				this.transcirpt.traceWarning ("decoding binary data with a `null` content-type; ignoring!");
			} else if (!this.expectedEncodingMetadata.hasSameContentType (metadata)) {
				this.transcirpt.traceError ("encoding / decoding binary data with an unexpected `%s` content-type; throwing!", metadata.getContentType ());
				throw (new EncodingException (String.format ("unexpected content-type: `%s`", metadata.getContentType ())));
			}
		}
		if (!this.expectedEncodingMetadata.hasSameContentType (EncodingMetadata.ANY)) {
			if (metadata.hasContentEncoding () && !this.expectedEncodingMetadata.hasSameContentEncoding (metadata)) {
				this.transcirpt.traceError ("encoding / decoding binary data with an unexpected `%s` content-encoding; throwing!", metadata.getContentEncoding ());
				throw (new EncodingException (String.format ("unexpected content-encoding: `%s`", metadata.getContentEncoding ())));
			}
		}
	}
	
	protected abstract TData decodeActual (final byte[] dataBytes, final EncodingMetadata metadata)
				throws EncodingException;
	
	protected abstract byte[] encodeActual (final TData data, final EncodingMetadata metadata)
				throws EncodingException;
	
	protected final Class<TData> dataClass;
	protected final TranscriptExceptionTracer exceptions;
	protected final EncodingMetadata expectedEncodingMetadata;
	protected final boolean nullAllowed;
	protected final Transcript transcirpt;
}
