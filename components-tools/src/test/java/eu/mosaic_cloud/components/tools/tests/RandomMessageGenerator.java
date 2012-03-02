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

package eu.mosaic_cloud.components.tools.tests;


import java.nio.ByteBuffer;
import java.util.Random;

import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCastRequest;

import com.google.common.base.Preconditions;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;


public final class RandomMessageGenerator
{
	private RandomMessageGenerator ()
	{
		super ();
		this.random = new Random ();
	}
	
	public final JSONArray generateArray ()
	{
		return (this.generateArray (1.0f));
	}
	
	public final ChannelMessage generateChannelMessage ()
	{
		return (ChannelMessage.create (ChannelMessageType.Exchange, this.generateObject (), this.generateData ()));
	}
	
	public final ComponentCallReply generateComponentCallReply (final ComponentCallRequest request)
	{
		Preconditions.checkNotNull (request);
		return (ComponentCallReply.create (true, request.inputs, request.data, request.reference));
	}
	
	public final ComponentCallRequest generateComponentCallRequest ()
	{
		return (ComponentCallRequest.create (Long.toString (this.random.nextLong ()), this.generateObject (), this.generateData (), ComponentCallReference.create ()));
	}
	
	public final ComponentCastRequest generateComponentCastRequest ()
	{
		return (ComponentCastRequest.create (Long.toString (this.random.nextLong ()), this.generateObject (), this.generateData ()));
	}
	
	public final ByteBuffer generateData ()
	{
		final byte[] data = new byte[1024];
		this.random.nextBytes (data);
		return (ByteBuffer.wrap (data).asReadOnlyBuffer ());
	}
	
	public final JSONObject generateObject ()
	{
		return (this.generateObject (1.0f));
	}
	
	protected final JSONArray generateArray (final float chance)
	{
		final JSONArray array;
		if (this.random.nextFloat () <= chance) {
			array = new JSONArray ();
			while (true) {
				if (this.random.nextFloat () > chance)
					break;
				array.add (this.generateObject (chance * 0.9f));
			}
		} else
			array = null;
		return (array);
	}
	
	protected final JSONObject generateObject (final float chance)
	{
		final JSONObject object;
		if (this.random.nextFloat () <= chance) {
			object = new JSONObject ();
			object.put ("some-boolean", Boolean.valueOf (this.random.nextBoolean ()));
			object.put ("some-integer", Long.valueOf (this.random.nextLong ()));
			object.put ("some-float", Float.valueOf (this.random.nextFloat ()));
			object.put ("some-string", Long.toString (this.random.nextLong ()));
			object.put ("some-object", this.generateObject (chance * 0.9f));
			object.put ("some-array", this.generateArray (chance * 0.9f));
		} else
			object = null;
		return (object);
	}
	
	public static final RandomMessageGenerator create ()
	{
		return (new RandomMessageGenerator ());
	}
	
	protected final Random random;
	public static final RandomMessageGenerator defaultInstance = RandomMessageGenerator.create ();
}
