/*
 * #%L
 * mosaic-core
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package mosaic.core.utils;

/**
 * Interface for defining data specific encoders (serializers) and decode
 * (deserializers).
 * 
 * @param <T>
 *            the type of data to encode and decode
 * 
 * @author Georgiana Macariu
 * 
 */
public  interface DataEncoder<T extends Object> {

	/**
	 * Encodes (serializes) an object as a stream of bytes.
	 * 
	 * @param data
	 *            the data to serialize
	 * @return the bytes
	 */
	byte[] encode(T data) throws Exception; // NOPMD by georgiana on 10/12/11 5:02 PM

	/**
	 * Decodes (deserializes) the data.
	 * 
	 * @param dataBytes
	 *            data bytes
	 * @return the decoded object
	 */
	T decode(byte[] dataBytes) throws Exception; // NOPMD by georgiana on 10/12/11 5:02 PM

}
