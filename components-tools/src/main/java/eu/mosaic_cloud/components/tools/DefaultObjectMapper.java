
package eu.mosaic_cloud.components.tools;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.DoubleNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.NumericNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.codehaus.jackson.util.TokenBuffer;


public final class DefaultObjectMapper
{
	private DefaultObjectMapper ()
	{
		super ();
	}
	
	public final <_Object_ extends Object> _Object_ decode (final Map<String, ? extends Object> data, final Class<_Object_> clasz)
			throws Throwable
	{
		final ObjectMapper mapper = new ObjectMapper ();
		final JsonNode node = this.transformToNode (data);
		final _Object_ object = mapper.treeToValue (node, clasz);
		return (object);
	}
	
	public final <_Object_ extends Object> Map<String, ? extends Object> encode (final _Object_ object, final Class<? super _Object_> clasz)
			throws Throwable
	{
		final ObjectMapper mapper = new ObjectMapper ();
		final TokenBuffer buffer = new TokenBuffer (mapper);
		mapper.viewWriter (clasz).writeValue (buffer, object);
		final JsonParser parser = buffer.asParser ();
		final JsonNode node = mapper.readTree (parser);
		parser.close ();
		final Map<String, ? extends Object> data = this.transformFromNode ((ObjectNode) node);
		return (data);
	}
	
	public final ArrayNode encode (final List<?> data)
	{
		final ArrayNode node = new ArrayNode (JsonNodeFactory.instance);
		for (final Object element : data)
			node.add (this.encode (element));
		return (node);
	}
	
	public final JsonNode encode (final Object data)
	{
		final JsonNode node;
		if (data == null)
			node = NullNode.instance;
		else if (data instanceof String)
			node = TextNode.valueOf ((String) data);
		else if (data instanceof Integer)
			node = IntNode.valueOf (((Integer) data).intValue ());
		else if (data instanceof Long)
			node = LongNode.valueOf (((Long) data).longValue ());
		else if (data instanceof Float)
			node = DoubleNode.valueOf (((Float) data).doubleValue ());
		else if (data instanceof Short)
			node = IntNode.valueOf (((Short) data).intValue ());
		else if (data instanceof Double)
			node = DoubleNode.valueOf (((Double) data).doubleValue ());
		else if (data instanceof Boolean)
			node = BooleanNode.valueOf (((Boolean) data).booleanValue ());
		else if (data instanceof Map)
			node = this.transformToNode ((Map<?, ?>) data);
		else if (data instanceof List)
			node = this.encode ((List<?>) data);
		else
			throw (new IllegalArgumentException (String.format ("invalid data (of class `%s`) `%s`", data.getClass (), data)));
		return (node);
	}
	
	public final List<? extends Object> transformFromNode (final ArrayNode node)
	{
		final ArrayList<Object> data = new ArrayList<Object> (node.size ());
		final Iterator<JsonNode> iterator = node.getElements ();
		while (iterator.hasNext ())
			data.add (this.transformFromNode (iterator.next ()));
		return (data);
	}
	
	public final Object transformFromNode (final JsonNode node)
	{
		final Object data;
		if (node instanceof NullNode)
			data = null;
		else if (node instanceof TextNode)
			data = ((TextNode) node).getTextValue ();
		else if (node instanceof NumericNode)
			data = ((NumericNode) node).getNumberValue ();
		else if (node instanceof BooleanNode)
			data = Boolean.valueOf (((BooleanNode) node).getBooleanValue ());
		else if (node instanceof ObjectNode)
			data = this.transformFromNode ((ObjectNode) node);
		else if (node instanceof ArrayNode)
			data = this.transformFromNode ((ArrayNode) node);
		else
			throw (new IllegalArgumentException (String.format ("invalid node (of class `%s`) `%s`", (node != null) ? node.getClass () : null, node)));
		return (data);
	}
	
	public final Map<String, ? extends Object> transformFromNode (final ObjectNode node)
	{
		final HashMap<String, Object> data = new HashMap<String, Object> (node.size ());
		final Iterator<Map.Entry<String, JsonNode>> iterator = node.getFields ();
		while (iterator.hasNext ()) {
			final Map.Entry<String, JsonNode> entry = iterator.next ();
			data.put (entry.getKey (), this.transformFromNode (entry.getValue ()));
		}
		return (data);
	}
	
	public final ObjectNode transformToNode (final Map<?, ?> data)
	{
		final ObjectNode node = new ObjectNode (JsonNodeFactory.instance);
		for (final Map.Entry<?, ?> entry : data.entrySet ()) {
			final Object key = entry.getKey ();
			if (!(key instanceof String))
				throw (new IllegalArgumentException ());
			node.put ((String) key, this.encode (entry.getValue ()));
		}
		return (node);
	}
	
	public static final DefaultObjectMapper create ()
	{
		return (new DefaultObjectMapper ());
	}
	
	public static final DefaultObjectMapper defaultInstance = new DefaultObjectMapper ();
}
