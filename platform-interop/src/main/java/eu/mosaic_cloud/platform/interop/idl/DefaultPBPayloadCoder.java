/*
 * #%L
 * mosaic-platform-interop
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
package eu.mosaic_cloud.platform.interop.idl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;
import com.google.protobuf.GeneratedMessage;

import eu.mosaic_cloud.interoperability.core.PayloadCoder;

/**
 * Encodes an object (request for operation or response to operation) using the
 * Google Protocol Buffers encoding.
 * 
 * @author Georgiana Macariu
 * 
 */
public class DefaultPBPayloadCoder implements PayloadCoder {

	private final Class<? extends GeneratedMessage> clasz;
	private final boolean nullAllowed;

	/**
	 * Creates a new encoder.
	 * 
	 * @param clasz
	 *            the message class
	 * @param nullAllowed
	 *            <code>true</code> if null objects can also be encoded
	 */
	public DefaultPBPayloadCoder(Class<? extends GeneratedMessage> clasz,
			boolean nullAllowed) {
		super();
		Preconditions.checkNotNull(clasz);
		Preconditions.checkArgument(Serializable.class.isAssignableFrom(clasz));
		this.clasz = clasz;
		this.nullAllowed = nullAllowed;
	}

	@Override
	public Object decode(ByteBuffer buffer) throws Throwable {
		Method createMethod = this.clasz.getMethod("parseFrom", byte[].class);
		Object object = createMethod.invoke(null, buffer.array());
		if (!this.nullAllowed && (object == null)) {
			throw new IOException("unexpected null object");
		}
		if (!this.clasz.isInstance(object)) {
			throw new IOException(String.format(
					"unexpected object class: `%s`", object.getClass()));
		}
		return this.clasz.cast(object);
	}

	@Override
	public ByteBuffer encode(Object object) throws Throwable {
		if (!this.nullAllowed) {
			Preconditions.checkNotNull(object);
		}
		Preconditions.checkArgument(this.clasz.isInstance(object));
		byte[] objectBytes = this.clasz.cast(object).toByteArray();
		return ByteBuffer.wrap(objectBytes);
	}

}
