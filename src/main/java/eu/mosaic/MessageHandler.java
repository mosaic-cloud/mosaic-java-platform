package eu.mosaic;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.rabbitmq.client.QueueingConsumer;

public class MessageHandler {

	private static String[] _headers = {"Connection", "Content-Length"}; 
	private static HashSet<String> _ignored_http_headers = new HashSet<String>(
			Arrays.asList(_headers)
			);
	
	@SuppressWarnings("unchecked")
	private static byte[] generate_full_http_request(JSONObject headers, byte[] body) throws JSONException, IOException, MessageFormatException {
		ByteArrayOutputStream outs = new ByteArrayOutputStream();
		String uri = null;
		String method = null;
		
		JSONObject http_headers = null;
		try {
			http_headers = headers.getJSONObject("http-headers");
		} catch (JSONException e) {
			throw new MessageFormatException("Could not find HTTP headers in message: "+e.getMessage());
		}
		try {
			uri = headers.getString("http-uri");
		} catch (JSONException e) {
			throw new MessageFormatException("Could not find request uri in message: "+e.getMessage());
		}
		
		try {
			method = headers.getString("http-method");
		} catch (JSONException e) {
			throw new MessageFormatException("Could not find request method in message: "+e.getMessage());
		} 
		
		Iterator<String> it = http_headers.keys();
		String request = method + " " + uri + " " + "HTTP/1.1\r\n";
		outs.write(request.getBytes());
		while (it.hasNext()) {
			String header_name = it.next();
			String header_value = http_headers.getString(header_name);
			String header = header_name+": "+header_value+"\r\n";
			outs.write(header.getBytes());
		}
		outs.write("\r\n".getBytes());
		outs.write(body);
		return outs.toByteArray();
	}
	
	public static QueueMessage decodeMessage(QueueingConsumer.Delivery delivery) throws MessageFormatException, IOException {
		
		byte[] message_body = delivery.getBody();
		ByteArrayInputStream is = new ByteArrayInputStream(message_body);
		DataInputStream dis = new DataInputStream(is);
		int metadataLength = 0;
		try {
			metadataLength = dis.readInt();
		} catch (IOException e) {
			throw new MessageFormatException("Expecting metadata length bug got nothing!");
		}
		if (metadataLength > message_body.length) {
			throw new MessageFormatException("Expecting metadata length but found garbage");
		}
		byte[] raw_headers = new byte[metadataLength];
		if (dis.read(raw_headers) != metadataLength) {
			throw new MessageFormatException("Could not read metadata");
		}
		JSONObject headers = null;
		
		try {
			headers = new JSONObject(new String(raw_headers));
		} catch (JSONException e) {
			throw new MessageFormatException("Failed parsing JSON object: "+e.getMessage());
		}
		
		
		
		
		byte[] body = new byte[0];
		
		if (!headers.optString("http-body", "empty").equalsIgnoreCase("empty")) {

			int bodyLength = dis.readInt(); // Body Length
			if (bodyLength > (message_body.length - metadataLength)) {
				throw new MessageFormatException(
						"Expected body length but found garbage");
			}
			body = new byte[bodyLength];
			if (dis.read(body) != bodyLength) {
				throw new MessageFormatException("Could not read body");
			}

		}
		QueueMessage _msg = new QueueMessage(headers, body);
		try {
			String callback_exchange = headers.getString("callback-exchange");
			String callback_identifier = headers.getString("callback-identifier");
			String callback_routing_key = headers.getString("callback-routing-key");
			_msg.set_callback_exchange(callback_exchange);
			_msg.set_callback_identifier(callback_identifier);
			_msg.set_callback_routing_key(callback_routing_key);
		} catch (JSONException e) {
			throw new MessageFormatException("Failed to extract routing information: " + e.getMessage());
		}
		
		
		_msg.set_delivery(delivery);
		try {
			_msg.set_http_request(generate_full_http_request(headers, body));
		} catch (JSONException e) {
			throw new MessageFormatException("Error generating http request: "+e.getMessage());
		}
		return _msg;
	}
	
	public static byte[] encodeMessage(byte[] in, String callback_identifier) throws IOException, HttpFormatException, JSONException {
		
		HashMap<String, String> headers = new HashMap<String, String>();

		
		int startOfBody = 0;
		int end = in.length - 1;

		while (startOfBody < end) {
			if (in[startOfBody] == '\n') { // Reached an end of line
				if ((startOfBody + 2) < end) {
					if (in[startOfBody+1]=='\r' && in[startOfBody+2]=='\n') { // We have finished
						startOfBody = startOfBody + 3;
						break;
					}
				}
			}
			startOfBody++;
		}
		byte[] header_bytes = new byte[startOfBody];
		int size_of_body = end - startOfBody;
		byte[] body_bytes = new byte[size_of_body];
		for (int i=0;i<startOfBody;i++) {
			header_bytes[i] = in[i];
		}
		for (int i=startOfBody;i<end;i++) {
			body_bytes[i - (end - size_of_body)] = in[i];
		}
		
		
		ByteArrayInputStream header_istream = new ByteArrayInputStream(header_bytes);
		BufferedReader header_reader = new BufferedReader(new InputStreamReader(new DataInputStream(header_istream)));
		String http_response = header_reader.readLine();
		
		String[] http_response_fields = http_response.split(" ", 3);
		if (http_response_fields.length != 3) {
			throw new HttpFormatException("Error reading HTTP response");
		}
		
		String server_protocol = http_response_fields[0];
		String http_version = server_protocol.split("/")[1];
		int response_code = Integer.parseInt(http_response_fields[1]);
		String response_message = http_response_fields[2];
		
		if (response_code > 500) {
			System.out.println("Made a booo");
		}
		
		if (http_response == null ) {
			throw new HttpFormatException("Expected to get http response but got nothing!");
		}
		while (true) {
			String _line = header_reader.readLine();
			if (_line == null) {
				break;
			}
			
			if (_line.length() == 0) { // Reached the end of headers
				break;
			}
			
			String[] header = _line.split(":", 2);
			if (header.length != 2) {
				throw new HttpFormatException("Invalid header: " + _line);
			}
			String header_name = header[0];
			String header_value = header[1];
			headers.put(header_name, header_value);
		}
		
		/*
		 * Prepare the response
		 */
		JSONObject json = new JSONObject();
		JSONObject http_headers = new JSONObject();
		for (String k : headers.keySet()) {
			if (!_ignored_http_headers.contains(k)) {
				http_headers.put(k, headers.get(k));
			}
		}
		
		json
			.put("version", 1)
			.put("callback-identifier", callback_identifier)
			.put("http-version", http_version)
			.put("http-code", response_code)
			.put("http-status", response_message)
			.put("http-headers", http_headers)
			.put("http-body", "following")
		;
		byte[] json_data = json.toString().getBytes();
		int message_size = json_data.length + body_bytes.length + 8;
		ByteArrayOutputStream ostream = new ByteArrayOutputStream(message_size);
		DataOutputStream dos = new DataOutputStream(ostream);
		dos.writeInt(json_data.length);
		dos.write(json_data);
		dos.writeInt(body_bytes.length);
		dos.write(body_bytes);
		dos.flush();
		return ostream.toByteArray();
	}
	
	public static class MessageFormatException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public MessageFormatException(String msg) {
			super(msg);
		}
		
	};
	
	public static class HttpFormatException extends IOException {

		private static final long serialVersionUID = 1L;
		
		public HttpFormatException(String msg) {
			super(msg);
		}
		
	}
}
