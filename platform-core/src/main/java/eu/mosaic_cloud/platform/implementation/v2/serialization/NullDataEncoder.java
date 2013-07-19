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


import eu.mosaic_cloud.platform.v2.serialization.EncodingMetadata;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;


public class NullDataEncoder
			extends BaseDataEncoder<byte[]>
{
	protected NullDataEncoder (final ExceptionTracer exceptions) {
		super (byte[].class, false, NullDataEncoder.EXPECTED_ENCODING_METADATA, exceptions);
	}
	
	@Override
	protected byte[] decodeActual (final byte[] data, final EncodingMetadata metadata) {
		return (data);
	}
	
	@Override
	protected byte[] encodeActual (final byte[] data, final EncodingMetadata metadata) {
		return (data);
	}
	
	public static final NullDataEncoder create () {
		return (new NullDataEncoder (FallbackExceptionTracer.defaultInstance));
	}
	
	static {
		EXPECTED_ENCODING_METADATA = EncodingMetadata.ANY;
		DEFAULT_INSTANCE = NullDataEncoder.create ();
	}
	public static final NullDataEncoder DEFAULT_INSTANCE;
	public static final EncodingMetadata EXPECTED_ENCODING_METADATA;
}
