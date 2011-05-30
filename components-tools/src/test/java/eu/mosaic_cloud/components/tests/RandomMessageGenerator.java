
package eu.mosaic_cloud.components.tests;


import java.nio.ByteBuffer;
import java.util.Random;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;


public class RandomMessageGenerator
{
	public RandomMessageGenerator ()
	{
		super ();
		this.random = new Random ();
	}
	
	public JSONArray generateArray ()
	{
		return (this.generateArray (1.0f));
	}
	
	public ChannelMessage generateChannelMessage ()
	{
		return (ChannelMessage.create (ChannelMessageType.Exchange, this.generateObject (), this.generateData ()));
	}
	
	public ComponentCallReply generateComponentCallReply (final ComponentCallRequest request)
	{
		Preconditions.checkNotNull (request);
		return (ComponentCallReply.create (request.metaData, request.data, request.reference));
	}
	
	public ComponentCallRequest generateComponentCallRequest ()
	{
		return (ComponentCallRequest.create (this.generateObject (), this.generateData (), ComponentCallReference.create ()));
	}
	
	public ComponentCastRequest generateComponentCastRequest ()
	{
		return (ComponentCastRequest.create (this.generateObject (), this.generateData ()));
	}
	
	public ByteBuffer generateData ()
	{
		final byte[] data = new byte[1024];
		this.random.nextBytes (data);
		return (ByteBuffer.wrap (data).asReadOnlyBuffer ());
	}
	
	public JSONObject generateObject ()
	{
		return (this.generateObject (1.0f));
	}
	
	protected JSONArray generateArray (final float chance)
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
	
	protected JSONObject generateObject (final float chance)
	{
		final JSONObject object;
		if (this.random.nextFloat () <= chance) {
			object = new JSONObject ();
			object.put ("some-boolean", Boolean.valueOf (this.random.nextBoolean ()));
			object.put ("some-integer", Long.valueOf (this.random.nextLong ()));
			object.put ("some-float", Double.valueOf (this.random.nextDouble ()));
			object.put ("some-string", Long.toString (this.random.nextLong ()));
			object.put ("some-object", this.generateObject (chance * 0.9f));
			object.put ("some-array", this.generateArray (chance * 0.9f));
		} else
			object = null;
		return (object);
	}
	
	protected final Random random;
	public static final RandomMessageGenerator defaultInstance = new RandomMessageGenerator ();
}
