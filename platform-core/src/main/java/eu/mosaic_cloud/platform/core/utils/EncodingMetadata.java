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


import java.util.Objects;

import com.google.common.base.Strings;


public final class EncodingMetadata
{
	public EncodingMetadata (final String contentType, final String contentEncoding)
	{
		super ();
		this.contentType = Strings.emptyToNull (contentType);
		this.contentEncoding = Strings.emptyToNull (contentEncoding);
	}
	
	public String getContentEncoding ()
	{
		return this.contentEncoding;
	}
	
	public String getContentType ()
	{
		return this.contentType;
	}
	
	public boolean hasContentEncoding ()
	{
		return this.contentEncoding != null;
	}
	
	public boolean hasContentType ()
	{
		return this.contentType != null;
	}
	
	public boolean hasSameContentEncoding (final EncodingMetadata other)
	{
		return Objects.equals (this.contentEncoding, other.contentEncoding);
	}
	
	public boolean hasSameContentType (final EncodingMetadata other)
	{
		return Objects.equals (this.contentType, other.contentType);
	}
	
	private final String contentEncoding;
	private final String contentType;
	public static final EncodingMetadata ANY = new EncodingMetadata ("*", "*");
	public static final EncodingMetadata NULL = new EncodingMetadata (null, null);
}
