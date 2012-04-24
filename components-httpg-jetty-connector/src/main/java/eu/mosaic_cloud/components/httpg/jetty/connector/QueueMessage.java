/*
 * #%L
 * mosaic-components-httpg-jetty-connector
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

package eu.mosaic_cloud.components.httpg.jetty.connector;


import java.io.Serializable;

import org.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer.Delivery;


public class QueueMessage
		implements
			Serializable
{
	public QueueMessage (final JSONObject headers, final byte[] body)
	{
		this._headers = headers;
		this.set_body (body);
	}
	
	public byte[] get_body ()
	{
		return this._body;
	}
	
	public String get_callback_exchange ()
	{
		return this._callback_exchange;
	}
	
	public String get_callback_identifier ()
	{
		return this._callback_identifier;
	}
	
	public String get_callback_routing_key ()
	{
		return this._callback_routing_key;
	}
	
	public Channel get_channel ()
	{
		return this._channel;
	}
	
	public Delivery get_delivery ()
	{
		return this._delivery;
	}
	
	public JSONObject get_headers ()
	{
		return this._headers;
	}
	
	public byte[] get_http_request ()
	{
		return this._http_request;
	}
	
	public void set_body (final byte[] _body)
	{
		this._body = _body;
	}
	
	public void set_callback_exchange (final String _callback_exchange)
	{
		this._callback_exchange = _callback_exchange;
	}
	
	public void set_callback_identifier (final String _callback_identifier)
	{
		this._callback_identifier = _callback_identifier;
	}
	
	public void set_callback_routing_key (final String _callback_routing_key)
	{
		this._callback_routing_key = _callback_routing_key;
	}
	
	public void set_channel (final Channel _channel)
	{
		this._channel = _channel;
	}
	
	public void set_delivery (final Delivery _delivery)
	{
		this._delivery = _delivery;
	}
	
	public void set_headers (final JSONObject _headers)
	{
		this._headers = _headers;
	}
	
	public void set_http_request (final byte[] _http_request)
	{
		this._http_request = _http_request;
	}
	
	private byte[] _body;
	private String _callback_exchange = null;
	private String _callback_identifier = null;
	private String _callback_routing_key = null;
	transient private Channel _channel = null;
	transient private Delivery _delivery = null;
	transient private JSONObject _headers = null;
	private byte[] _http_request = null;
	/**
	 * 
	 */
	private static final long serialVersionUID = 6681953494107319042L;
}
