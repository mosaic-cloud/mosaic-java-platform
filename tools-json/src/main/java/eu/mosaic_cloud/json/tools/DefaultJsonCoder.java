/*
 * #%L
 * mosaic-tools-json
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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

package eu.mosaic_cloud.json.tools;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.json.core.JsonCoder;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.JSONParserBase;
import net.minidev.json.parser.ParseException;


public final class DefaultJsonCoder
		extends Object
		implements
			JsonCoder
{
	private DefaultJsonCoder ()
	{
		super ();
		this.metaDataCharset = Charset.forName ("utf-8");
		this.style = new JSONStyle (-1 & ~JSONStyle.FLAG_PROTECT_KEYS & ~JSONStyle.FLAG_PROTECT_VALUES);
	}
	
	@Override
	public final Object decode (final ByteBuffer data_)
	{
		Preconditions.checkNotNull (data_);
		final ByteBuffer data = data_.asReadOnlyBuffer ();
		final byte[] dataBytes = new byte[data.remaining ()];
		data.get (dataBytes);
		final String dataString = new String (dataBytes, this.metaDataCharset);
		return (this.decodeFromString (dataString));
	}
	
	public final Object decodeFromString (final String data)
	{
		try {
			Preconditions.checkNotNull (data);
			final JSONParser parser = new JSONParser (JSONParserBase.MODE_RFC4627);
			final Object structure = parser.parse (data);
			return (structure);
		} catch (final ParseException exception) {
			throw (new IllegalArgumentException (exception.getMessage (), exception.getCause ()));
		} catch (final IOException exception) {
			throw (new IllegalStateException (exception));
		}
	}
	
	@Override
	public final ByteBuffer encode (final Object structure)
	{
		final String dataString = this.encodeToString (structure);
		final byte[] dataBytes = dataString.getBytes (this.metaDataCharset);
		final ByteBuffer data = ByteBuffer.wrap (dataBytes);
		return (data);
	}
	
	public final String encodeToString (final Object structure)
	{
		return (JSONValue.toJSONString (structure, this.style));
	}
	
	private final Charset metaDataCharset;
	private final JSONStyle style;
	
	public static final DefaultJsonCoder create ()
	{
		return (new DefaultJsonCoder ());
	}
	
	public static final DefaultJsonCoder defaultInstance = new DefaultJsonCoder ();
}
