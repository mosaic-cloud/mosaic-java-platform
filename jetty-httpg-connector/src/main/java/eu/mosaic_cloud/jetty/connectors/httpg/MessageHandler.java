/*
 * #%L
 * mosaic-jetty-httpg-connector
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
package eu.mosaic_cloud.jetty.connectors.httpg;

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
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.rabbitmq.client.QueueingConsumer;

public class MessageHandler {

	private static String[] _headers = { "Connection", "Content-Length" };
	private static HashSet<String> _ignored_http_headers = new HashSet<String>(
			Arrays.asList(_headers));

	@SuppressWarnings("unchecked")
	private static byte[] generate_full_http_request(JSONObject headers,
			byte[] body) throws JSONException, IOException,
			MessageFormatException {
		final ByteArrayOutputStream outs = new ByteArrayOutputStream();
		String uri = null;
		String method = null;

		JSONObject http_headers = null;
		try {
			http_headers = headers.getJSONObject("http-headers");
		} catch (JSONException e) {
			throw new MessageFormatException(
					"Could not find HTTP headers in message: " + e.getMessage());
		}
		try {
			uri = headers.getString("http-uri");
		} catch (JSONException e) {
			throw new MessageFormatException(
					"Could not find request uri in message: " + e.getMessage());
		}

		try {
			method = headers.getString("http-method");
		} catch (JSONException e) {
			throw new MessageFormatException(
					"Could not find request method in message: "
							+ e.getMessage());
		}

		Iterator<String> it = http_headers.keys();
		String request = method + " " + uri + " " + "HTTP/1.1\r\n";
		outs.write(request.getBytes());
		while (it.hasNext()) {
			String header_name = it.next();
			String header_value = http_headers.getString(header_name);
			String header = header_name + ": " + header_value + "\r\n";
			outs.write(header.getBytes());
		}
		outs.write("\r\n".getBytes());
		outs.write(body);
		return outs.toByteArray();
	}

	public static QueueMessage decodeMessage(QueueingConsumer.Delivery delivery)
			throws MessageFormatException, IOException {

		byte[] message_body = delivery.getBody();
		ByteArrayInputStream is = new ByteArrayInputStream(message_body);
		DataInputStream dis = new DataInputStream(is);
		int metadataLength = 0;
		try {
			metadataLength = dis.readInt();
		} catch (IOException e) {
			throw new MessageFormatException(
					"Expecting metadata length bug got nothing!");
		}
		if (metadataLength > message_body.length) {
			throw new MessageFormatException(
					"Expecting metadata length but found garbage");
		}
		final byte[] raw_headers = new byte[metadataLength];
		if (dis.read(raw_headers) != metadataLength) {
			throw new MessageFormatException("Could not read metadata");
		}
		JSONObject headers = null;

		try {
			headers = new JSONObject(new String(raw_headers));
		} catch (JSONException e) {
			throw new MessageFormatException("Failed parsing JSON object: "
					+ e.getMessage());
		}

		byte[] body = new byte[0];

		String body_method = headers.optString("http-body", "empty");
		if (body_method.equalsIgnoreCase("empty")) {
			
		} else if (body_method.equalsIgnoreCase("following")) {
			final int bodyLength = dis.readInt(); // Body Length
			if (bodyLength > (message_body.length - metadataLength)) {
				throw new MessageFormatException(
						"Expected body length but found garbage");
			}
			body = new byte[bodyLength];
			if (dis.read(body) != bodyLength) {
				throw new MessageFormatException("Could not read body");
			}
		} else if (body_method.equalsIgnoreCase("embedded")) {
			body = headers.optString("http-body-content").getBytes();
		} else {
			throw new MessageFormatException("Unknown body encapsulation method");
		}
		
		
		QueueMessage _msg = new QueueMessage(headers, body);
		try {
			final String callback_exchange = headers.getString("callback-exchange");
			final String callback_identifier = headers
					.getString("callback-identifier");
			final String callback_routing_key = headers
					.getString("callback-routing-key");
			_msg.set_callback_exchange(callback_exchange);
			_msg.set_callback_identifier(callback_identifier);
			_msg.set_callback_routing_key(callback_routing_key);
		} catch (JSONException e) {
			throw new MessageFormatException(
					"Failed to extract routing information: " + e.getMessage());
		}

		_msg.set_delivery(delivery);
		try {
			_msg.set_http_request(generate_full_http_request(headers, body));
		} catch (JSONException e) {
			throw new MessageFormatException("Error generating http request: "
					+ e.getMessage());
		}
		return _msg;
	}

	public static byte[] encodeMessage(byte[] in, String callback_identifier)
			throws IOException, HttpFormatException, JSONException {

		final HashMap<String, String> headers = new HashMap<String, String>();

		int startOfBody = 0;
		int end = in.length - 1;

		while (startOfBody < end) {
			if (in[startOfBody] == '\n') { // Reached an end of line
				if ((startOfBody + 2) < end) {
					if (in[startOfBody + 1] == '\r'
							&& in[startOfBody + 2] == '\n') { // We have
																// finished
						startOfBody = startOfBody + 3;
						break;
					}
				}
			}
			startOfBody++;
		}
		final byte[] header_bytes = new byte[startOfBody];
		final int size_of_body = end - startOfBody + 1;
		final byte[] body_bytes = new byte[size_of_body];

		System.arraycopy(in, 0, header_bytes, 0, startOfBody);
		System.arraycopy(in, startOfBody, body_bytes, 0, size_of_body);

		final ByteArrayInputStream header_istream = new ByteArrayInputStream(
				header_bytes);
		final BufferedReader header_reader = new BufferedReader(
				new InputStreamReader(new DataInputStream(header_istream)));
		final String http_response = header_reader.readLine();
		if (null == http_response) {
			throw new HttpFormatException("Error reading HTTP response");
		}
		final String[] http_response_fields = http_response.split(" ", 3);
		if (http_response_fields.length != 3) {
			throw new HttpFormatException("Error reading HTTP response");
		}

		final String server_protocol = http_response_fields[0];
		final String http_version = server_protocol.split("/")[1];
		final int response_code = Integer.parseInt(http_response_fields[1]);
		final String response_message = http_response_fields[2];

		// if (response_code > 500) {
		//	System.out.println("Made a booo");
		// }


		while (true) {
			final String _line = header_reader.readLine();
			if (_line == null) {
				break;
			}

			if (_line.length() == 0) { // Reached the end of headers
				break;
			}

			final String[] header = _line.split(":", 2);
			if (header.length != 2) {
				throw new HttpFormatException("Invalid header: " + _line);
			}
			final String header_name = header[0];
			final String header_value = header[1];
			headers.put(header_name, header_value);
		}

		/*
		 * Prepare the response
		 */
		final JSONObject json = new JSONObject();
		final JSONObject http_headers = new JSONObject();

		for (Map.Entry<String, String> k : headers.entrySet()) {
			if (!_ignored_http_headers.contains(k.getKey())) {
				http_headers.put(k.getKey(), k.getValue());
			}
		}

		json.put("version", 1).put("callback-identifier", callback_identifier)
				.put("http-version", http_version)
				.put("http-code", response_code)
				.put("http-status", response_message)
				.put("http-headers", http_headers)
				.put("http-body", "following");
		final byte[] json_data = json.toString().getBytes();
		final int message_size = json_data.length + body_bytes.length + 8;

		final ByteArrayOutputStream ostream = new ByteArrayOutputStream(message_size);
		final DataOutputStream dos = new DataOutputStream(ostream);
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
