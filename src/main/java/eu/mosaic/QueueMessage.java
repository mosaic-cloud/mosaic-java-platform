package eu.mosaic;

import java.io.Serializable;

import org.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer.Delivery;

public class QueueMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6681953494107319042L;
	private JSONObject _headers;
	private byte[] _body;
	private Delivery _delivery = null;
	private byte[] _http_request = null;
	private String _callback_exchange = null;
	private String _callback_identifier = null;
	private String _callback_routing_key = null;
	private Channel _channel = null;
	public QueueMessage(JSONObject headers,
			byte[] body) {
		_headers = headers;
		set_body(body);
		
	}
	public void set_headers(JSONObject _headers) {
		this._headers = _headers;
	}
	public JSONObject get_headers() {
		return _headers;
	}
	public void set_body(byte[] _body) {
		this._body = _body;
	}
	public byte[] get_body() {
		return _body;
	}
	public void set_delivery(Delivery _delivery) {
		this._delivery = _delivery;
	}
	public Delivery get_delivery() {
		return _delivery;
	}
	public void set_http_request(byte[] _http_request) {
		this._http_request = _http_request;
	}
	public byte[] get_http_request() {
		return _http_request;
	}
	public void set_callback_exchange(String _callback_exchange) {
		this._callback_exchange = _callback_exchange;
	}
	public String get_callback_exchange() {
		return _callback_exchange;
	}
	public void set_callback_identifier(String _callback_identifier) {
		this._callback_identifier = _callback_identifier;
	}
	public String get_callback_identifier() {
		return _callback_identifier;
	}
	public void set_callback_routing_key(String _callback_routing_key) {
		this._callback_routing_key = _callback_routing_key;
	}
	public String get_callback_routing_key() {
		return _callback_routing_key;
	}
	public void set_channel(Channel _channel) {
		this._channel = _channel;
	}
	public Channel get_channel() {
		return _channel;
	}

}
