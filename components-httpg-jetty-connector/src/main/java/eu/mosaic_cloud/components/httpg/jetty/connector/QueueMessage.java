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

public class QueueMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6681953494107319042L;
	transient private JSONObject _headers = null;
	private byte[] _body;
	transient private Delivery _delivery = null;
	private byte[] _http_request = null;
	private String _callback_exchange = null;
	private String _callback_identifier = null;
	private String _callback_routing_key = null;
	transient private Channel _channel = null;

	public QueueMessage(JSONObject headers, byte[] body) {
		_headers = headers;
		set_body(body);

	}

	public byte[] get_body() {
		return _body;
	}

	public String get_callback_exchange() {
		return _callback_exchange;
	}

	public String get_callback_identifier() {
		return _callback_identifier;
	}

	public String get_callback_routing_key() {
		return _callback_routing_key;
	}

	public Channel get_channel() {
		return _channel;
	}

	public Delivery get_delivery() {
		return _delivery;
	}

	public JSONObject get_headers() {
		return _headers;
	}

	public byte[] get_http_request() {
		return _http_request;
	}

	public void set_body(byte[] _body) {
		this._body = _body;
	}

	public void set_callback_exchange(String _callback_exchange) {
		this._callback_exchange = _callback_exchange;
	}

	public void set_callback_identifier(String _callback_identifier) {
		this._callback_identifier = _callback_identifier;
	}

	public void set_callback_routing_key(String _callback_routing_key) {
		this._callback_routing_key = _callback_routing_key;
	}

	public void set_channel(Channel _channel) {
		this._channel = _channel;
	}

	public void set_delivery(Delivery _delivery) {
		this._delivery = _delivery;
	}

	public void set_headers(JSONObject _headers) {
		this._headers = _headers;
	}

	public void set_http_request(byte[] _http_request) {
		this._http_request = _http_request;
	}

}
