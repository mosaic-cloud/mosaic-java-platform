/*
 * #%L
 * mosaic-tools-json
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

package eu.mosaic_cloud.json.tools;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.json.core.JsonMapper;
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


public final class DefaultJsonMapper
		extends Object
		implements
			JsonMapper
{
	private DefaultJsonMapper ()
	{
		super ();
	}
	
	@Override
	public final <_Object_ extends Object> _Object_ decode (final Object data, final Class<_Object_> clasz)
	{
		try {
			final ObjectMapper mapper = new ObjectMapper ();
			final JsonNode node = this.encode (data);
			final _Object_ object = mapper.treeToValue (node, clasz);
			return (object);
		} catch (final IOException exception) {
			throw (new IllegalStateException (exception));
		}
	}
	
	@Override
	public final <_Object_ extends Object> Object encode (final _Object_ object, final Class<_Object_> clasz)
	{
		try {
			final ObjectMapper mapper = new ObjectMapper ();
			final TokenBuffer buffer = new TokenBuffer (mapper);
			mapper.viewWriter (clasz).writeValue (buffer, object);
			final JsonParser parser = buffer.asParser ();
			final JsonNode node = mapper.readTree (parser);
			parser.close ();
			final Object data = this.transformFromNode (node);
			return (data);
		} catch (final IOException exception) {
			throw (new IllegalStateException (exception));
		}
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
	
	public static final DefaultJsonMapper create ()
	{
		return (new DefaultJsonMapper ());
	}
	
	public static final DefaultJsonMapper defaultInstance = new DefaultJsonMapper ();
}
