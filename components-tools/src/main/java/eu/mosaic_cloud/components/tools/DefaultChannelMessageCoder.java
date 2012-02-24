/*
 * #%L
 * mosaic-components-tools
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

package eu.mosaic_cloud.components.tools;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageCoder;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.tools.json.core.JsonCoder;
import eu.mosaic_cloud.tools.json.tools.DefaultJsonCoder;

import com.google.common.base.Preconditions;

import net.minidev.json.JSONObject;


public final class DefaultChannelMessageCoder
		extends Object
		implements
			ChannelMessageCoder
{
	private DefaultChannelMessageCoder (final JsonCoder jsonCoder)
	{
		super ();
		Preconditions.checkNotNull (jsonCoder);
		this.jsonCoder = jsonCoder;
	}
	
	@Override
	public final ChannelMessage decode (final ByteBuffer packet_)
			throws Throwable
	{
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
		final ByteBuffer metaDataSlice = packet.asReadOnlyBuffer ();
		metaDataSlice.position (metaDataBeginPosition);
		metaDataSlice.limit (metaDataEndPosition);
		final Object metaDataValue = this.jsonCoder.decode (metaDataSlice);
		packet.position (metaDataEndPosition + 1);
		Preconditions.checkArgument ((metaDataValue != null) && (metaDataValue instanceof Map), "unexpected meta-data value: `%s`", metaDataValue);
		final Map<String, Object> metaData = (Map<String, Object>) metaDataValue;
		final Object messageTypeValue = metaData.remove ("__type__");
		Preconditions.checkArgument ((messageTypeValue != null) && (messageTypeValue instanceof String), "unexpected message type value: `%s`", messageTypeValue);
		final ChannelMessageType messageType;
		if ("exchange".equals (ChannelMessageType.Exchange.identifier))
			messageType = ChannelMessageType.Exchange;
		else
			messageType = null;
		Preconditions.checkArgument (messageType != null, "invalid message type: `%s`", messageTypeValue);
		final ChannelMessage message = ChannelMessage.create (messageType, metaData, packet);
		return (message);
	}
	
	@Override
	public final ByteBuffer encode (final ChannelMessage message)
			throws Throwable
	{
		Preconditions.checkNotNull (message);
		Preconditions.checkArgument (message.type != null, "unexpected message-type value: `%s`", message.type);
		Preconditions.checkArgument ((message.metaData != null), "unexpected meta-data value: `%s`", message.metaData);
		Preconditions.checkNotNull (message.data);
		final JSONObject metaDataValue = new JSONObject (message.metaData);
		metaDataValue.put ("__type__", message.type.identifier);
		final ByteBuffer metaData = this.jsonCoder.encode (metaDataValue);
		final ByteBuffer data = message.data.asReadOnlyBuffer ();
		final int packetSize = metaData.remaining () + 1 + data.remaining ();
		final ByteBuffer packet = ByteBuffer.allocate (packetSize + 4);
		packet.putInt (packetSize);
		packet.put (metaData);
		packet.put ((byte) 0);
		packet.put (data);
		packet.flip ();
		return (packet.asReadOnlyBuffer ());
	}
	
	private final JsonCoder jsonCoder;
	
	public static final DefaultChannelMessageCoder create ()
	{
		return (new DefaultChannelMessageCoder (DefaultJsonCoder.defaultInstance));
	}
	
	public static final DefaultChannelMessageCoder defaultInstance = DefaultChannelMessageCoder.create ();
}
