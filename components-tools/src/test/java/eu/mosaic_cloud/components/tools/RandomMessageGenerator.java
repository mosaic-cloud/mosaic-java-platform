
package eu.mosaic_cloud.components.tools;


import java.nio.ByteBuffer;
import java.util.Random;

import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;


public class RandomMessageGenerator
{
	public RandomMessageGenerator ()
	{
		super ();
		this.random = new Random ();
	}
	
	public ChannelMessage generate ()
	{
		final JSONObject metaData = this.generateObject (1.0f);
		final byte[] data = new byte[1024];
		this.random.nextBytes (data);
		final ChannelMessage message = new ChannelMessage (ChannelMessageType.Exchange, metaData, ByteBuffer.wrap (data));
		return (message);
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
