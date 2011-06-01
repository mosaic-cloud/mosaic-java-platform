
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


public final class DefaultChannelMessageCoder
		extends Object
		implements
			ChannelMessageCoder
{
	private DefaultChannelMessageCoder ()
	{
		super ();
		this.transcript = Transcript.create (this);
		this.metaDataCharset = Charset.forName ("utf-8");
		this.style = new JSONStyle (-1 & ~JSONStyle.FLAG_PROTECT_KEYS & ~JSONStyle.FLAG_PROTECT_VALUES);
	}
	
	@Override
	public final ChannelMessage decode (final ByteBuffer packet_)
			throws IOException,
				ParseException
	{
		this.transcript.traceDebugging ("decoding message...");
		Preconditions.checkNotNull (packet_);
		final ByteBuffer packet = packet_.asReadOnlyBuffer ();
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
		Preconditions.checkArgument ((metaDataValue != null) && (metaDataValue instanceof JSONObject), "unexpected meta-data value: `%s`", metaDataValue);
		final JSONObject metaData = (JSONObject) metaDataValue;
		final Object messageTypeValue = metaData.get ("__type__");
		Preconditions.checkArgument ((messageTypeValue != null) && (messageTypeValue instanceof String), "unexpected message type value: `%s`", messageTypeValue);
		final ChannelMessageType messageType;
		if ("exchange".equals (ChannelMessageType.Exchange.identifier))
			messageType = ChannelMessageType.Exchange;
		else
			messageType = null;
		Preconditions.checkArgument (messageType != null, "invalid message type: `%s`", messageTypeValue);
		metaData.remove ("__type__");
		final ChannelMessage message = ChannelMessage.create (messageType, metaData, packet);
		return (message);
	}
	
	@Override
	public final ByteBuffer encode (final ChannelMessage message)
	{
		this.transcript.traceDebugging ("encoding message...");
		Preconditions.checkNotNull (message);
		Preconditions.checkArgument (message.type != null, "unexpected message-type value: `%s`", message.type);
		Preconditions.checkArgument ((message.metaData != null), "unexpected meta-data value: `%s`", message.metaData);
		Preconditions.checkNotNull (message.data);
		final JSONObject metaData = new JSONObject (message.metaData);
		metaData.put ("__type__", message.type.identifier);
		final String metaDataString = JSONValue.toJSONString (metaData, this.style);
		final byte[] metaDataBytes = metaDataString.getBytes (this.metaDataCharset);
		final ByteBuffer data = message.data.asReadOnlyBuffer ();
		final int packetSize = metaDataBytes.length + 1 + data.remaining ();
		final ByteBuffer packet = ByteBuffer.allocate (packetSize + 4);
		packet.putInt (packetSize);
		packet.put (metaDataBytes);
		packet.put ((byte) 0);
		packet.put (data);
		packet.flip ();
		return (packet.asReadOnlyBuffer ());
	}
	
	private final Charset metaDataCharset;
	private final JSONStyle style;
	private final Transcript transcript;
	
	public static final DefaultChannelMessageCoder create ()
	{
		return (new DefaultChannelMessageCoder ());
	}
	
	public static final DefaultChannelMessageCoder defaultInstance = new DefaultChannelMessageCoder ();
}
