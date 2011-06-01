
package eu.mosaic_cloud.json.tools;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.json.core.JsonCoder;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
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
		try {
			Preconditions.checkNotNull (data_);
			final ByteBuffer data = data_.asReadOnlyBuffer ();
			final byte[] dataBytes = new byte[data.remaining ()];
			data.get (dataBytes);
			final String dataString = new String (dataBytes, this.metaDataCharset);
			final JSONParser parser = new JSONParser ();
			final Object structure = parser.parse (dataString);
			return (structure);
		} catch (final ParseException exception) {
			throw (new IllegalArgumentException (exception.getMessage (), exception.getCause ()));
		} catch (final IOException exception) {
			throw (new RuntimeException (exception));
		}
	}
	
	@Override
	public final ByteBuffer encode (final Object structure)
	{
		final String dataString = JSONValue.toJSONString (structure, this.style);
		final byte[] dataBytes = dataString.getBytes (this.metaDataCharset);
		final ByteBuffer data = ByteBuffer.wrap (dataBytes);
		return (data);
	}
	
	private final Charset metaDataCharset;
	private final JSONStyle style;
	
	public static final DefaultJsonCoder create ()
	{
		return (new DefaultJsonCoder ());
	}
	
	public static final DefaultJsonCoder defaultInstance = new DefaultJsonCoder ();
}
