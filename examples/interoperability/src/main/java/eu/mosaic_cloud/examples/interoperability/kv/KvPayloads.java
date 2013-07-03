/*
 * #%L
 * mosaic-examples-interoperability
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

package eu.mosaic_cloud.examples.interoperability.kv;


import java.io.Serializable;


public interface KvPayloads
{
	public static final class Error
				implements
					Serializable
	{
		public Error (final long sequence) {
			this.sequence = sequence;
		}
		
		public final long sequence;
		private static final long serialVersionUID = 1L;
	}
	
	public static final class GetReply
				implements
					Serializable
	{
		public GetReply (final long sequence, final String value) {
			this.sequence = sequence;
			this.value = value;
		}
		
		public final long sequence;
		public final String value;
		private static final long serialVersionUID = 1L;
	}
	
	public static final class GetRequest
				implements
					Serializable
	{
		public GetRequest (final long sequence, final String key) {
			this.sequence = sequence;
			this.key = key;
		}
		
		public final String key;
		public final long sequence;
		private static final long serialVersionUID = 1L;
	}
	
	public static final class Ok
				implements
					Serializable
	{
		public Ok (final long sequence) {
			this.sequence = sequence;
		}
		
		public final long sequence;
		private static final long serialVersionUID = 1L;
	}
	
	public static final class PutRequest
				implements
					Serializable
	{
		public PutRequest (final long sequence, final String key, final String value) {
			this.sequence = sequence;
			this.key = key;
			this.value = value;
		}
		
		public final String key;
		public final long sequence;
		public final String value;
		private static final long serialVersionUID = 1L;
	}
}
