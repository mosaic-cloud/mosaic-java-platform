
package eu.mosaic_cloud.components.tools;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageCoder;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.transcript.core.Transcript;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;


public final class DefaultJsonSimpleMessageCoder
		extends Object
		implements
			ChannelMessageCoder
{
	public DefaultJsonSimpleMessageCoder ()
	{
		super ();
		this.transcript = Transcript.create (this);
		this.metaDataCharset = Charset.forName ("utf-8");
	}
	
	@Override
	public ChannelMessage decode (final ByteBuffer packet)
			throws IOException,
				ParseException
	{
		this.transcript.traceDebugging ("decoding message...");
		Preconditions.checkNotNull (packet);
		Preconditions.checkArgument (packet.order () == ByteOrder.BIG_ENDIAN, "invalid packet byte-order");
		Preconditions.checkArgument (packet.remaining () >= 4, "invalid packet framing");
		final int packetSize = packet.getInt ();
		Preconditions.checkArgument (packetSize == packet.remaining (), "invalid packet framing");
		final int metaDataBeginPosition = packet.position ();
		packet.mark ();
		while (true) {
			Preconditions.checkArgument (packet.remaining () > 0, "unexpected end of buffer while searching for JSON meta-data delimiter");
			if (packet.get () == 0)
				break;
		}
		final int metaDataEndPosition = packet.position () - 1;
		packet.position (metaDataBeginPosition);
		final byte[] metaDataBytes = new byte[metaDataEndPosition - metaDataBeginPosition];
		packet.get (metaDataBytes);
		packet.position (metaDataEndPosition + 1);
		final String metaDataString = new String (metaDataBytes, this.metaDataCharset);
		final JSONParser metaDataParser = new JSONParser ();
		final Object metaDataValue = metaDataParser.parse (metaDataString);
		Preconditions.checkArgument ((metaDataValue != null) && (metaDataValue instanceof JSONObject), "unexpected meta-data value: `{}`", metaDataValue);
		final JSONObject metaData = (JSONObject) metaDataValue;
		final Object messageTypeValue = metaData.get ("__type__");
		Preconditions.checkArgument ((messageTypeValue != null) && (messageTypeValue instanceof String), "unexpected message type value: `{}`", messageTypeValue);
		final ChannelMessageType messageType;
		if ("exchange".equals (ChannelMessageType.Exchange.identifier))
			messageType = ChannelMessageType.Exchange;
		else
			messageType = null;
		Preconditions.checkArgument (messageType != null, "invalid message type: `{}`", messageTypeValue);
		metaData.remove ("__type__");
		final ChannelMessage message = new ChannelMessage (messageType, metaData, packet);
		return (message);
	}
	
	@Override
	public ByteBuffer encode (final ChannelMessage message)
	{
		this.transcript.traceDebugging ("encoding message...");
		Preconditions.checkNotNull (message);
		Preconditions.checkArgument (message.type != null, "unexpected message-type value: `{}`", message.type);
		Preconditions.checkArgument ((message.metaData != null) && (message.metaData instanceof JSONObject), "unexpected meta-data value: `{}`", message.metaData);
		Preconditions.checkNotNull (message.data);
		final JSONObject metaData = (JSONObject) message.metaData;
		metaData.put ("__type__", message.type.identifier);
		final String metaDataString = JSONValue.toJSONString (metaData, JSONStyle.MAX_COMPRESS);
		final byte[] metaDataBytes = metaDataString.getBytes (this.metaDataCharset);
		final int packetSize = metaDataBytes.length + 1 + message.data.remaining ();
		final ByteBuffer packet = ByteBuffer.allocate (packetSize + 4);
		packet.putInt (packetSize);
		packet.put (metaDataBytes);
		packet.put ((byte) 0);
		packet.put (message.data);
		packet.flip ();
		return (packet);
	}
	
	private final Charset metaDataCharset;
	private final Transcript transcript;
	
	public static final DefaultJsonSimpleMessageCoder defaultInstance = new DefaultJsonSimpleMessageCoder ();
}
