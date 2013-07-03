/*
 * #%L
 * mosaic-platform-interop
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

package eu.mosaic_cloud.platform.interop.common.kv;


public class KeyValueMessage
{
	public KeyValueMessage (final String key, final byte[] data, final String contentType) {
		this (key, data, null, contentType);
	}
	
	public KeyValueMessage (final String key, final byte[] data, final String contentEncoding, final String contentType) {
		super ();
		this.key = key;
		this.data = data;
		this.contentEncoding = contentEncoding;
		this.contentType = contentType;
	}
	
	public String getContentEncoding () {
		return this.contentEncoding;
	}
	
	public String getContentType () {
		return this.contentType;
	}
	
	public byte[] getData () {
		return this.data;
	}
	
	public String getKey () {
		return this.key;
	}
	
	@Override
	public String toString () {
		return this.key + " " + this.contentType + "(" + this.contentEncoding + ")";
	}
	
	final String key;
	final private String contentEncoding;
	final private String contentType;
	final private byte[] data;
}
