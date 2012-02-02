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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import eu.mosaic_cloud.components.httpg.jetty.connector.MessageHandler.MessageFormatException;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import org.eclipse.jetty.io.ByteArrayEndPoint;
import org.eclipse.jetty.io.ConnectedEndPoint;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;


public class AmqpConnector extends AbstractConnector {
	protected final Set<EndPoint> _connections;

	private ConnectionFactory _connectionFactory = null;
	private Connection _connection = null;
	private Channel _channel = null;
	private QueueingConsumer _consumer = null;
	private String _hostName;
	private int _port;
	private String _virtualHost;
	private String _userName;
	private String _userPassword;
	private String _routingKey;
	private String _exchangeName;
	private String _inputQueueName;
	private boolean _autoDeclareQueue;

	public AmqpConnector(String exchangeName, String routingKey,
			String queueName, String hostName, String userName,
			String userPassword, int port, String virtualHost, boolean autoDeclareQueue) {
		_userName = userName;
		_userPassword = userPassword;
		_exchangeName = exchangeName;
		_routingKey = routingKey;
		_hostName = hostName;
		_port = port;
		_virtualHost = virtualHost;
		_inputQueueName = queueName;
		_autoDeclareQueue = autoDeclareQueue;
		_connections = Collections.synchronizedSet (new HashSet<EndPoint>());
	}

	@Override
	protected void accept(int acceptorID) throws IOException,
			InterruptedException {
		QueueMessage msg = null;
		// Log.info("Waiting for messages");
		QueueingConsumer.Delivery delivery;
		try {
			delivery = _consumer.nextDelivery();
		} catch (ShutdownSignalException e) {
			Log.warn (e);
			throw e;
		}
		try {
			msg = MessageHandler.decodeMessage(delivery);
			msg.set_channel(_channel);
		} catch (MessageFormatException e) {
			Log.warn(e);
			throw new IOException(e);
		} catch (IOException e) {
			Log.warn(e);
			throw e;
		}
		ConnectorEndPoint _endPoint = new ConnectorEndPoint(msg);
		_endPoint.dispatch();
	}

	@Override
	public void close() throws IOException {
		
	}

	protected org.eclipse.jetty.io.Connection newConnection(EndPoint endp) {
		return new HttpConnection(this, endp, getServer());
	}

	@Override
	public Object getConnection() {
		Log.debug("getConnection()");
		return _consumer;
	}

	@Override
	public int getLocalPort() {

		return 0;
	}

	private void setupConnection() throws IOException {
		Log.info("Opening AmqpConnector");
		if (_connectionFactory == null) {
			_connectionFactory = new ConnectionFactory();
			_connectionFactory.setHost(_hostName);
			_connectionFactory.setPort(_port);
			_connectionFactory.setVirtualHost(_virtualHost);
			_connectionFactory.setUsername(_userName);
			_connectionFactory.setPassword(_userPassword);
		}

		_connection = _connectionFactory.newConnection();

		_channel = _connection.createChannel();

		if (_autoDeclareQueue) {
			_channel.exchangeDeclare(_exchangeName, "topic", false);
			_inputQueueName = _channel.queueDeclare(_inputQueueName, false,
					false, false, null).getQueue();
		}

		_channel.queueBind(_inputQueueName, _exchangeName, _routingKey);
		_consumer = new QueueingConsumer(_channel);
		_channel.basicConsume(_inputQueueName, true, _consumer);

	}

	@Override
	public void open() throws IOException {
		while (true) {
			try {
				setupConnection();
				break;
			} catch (IOException e) {
				Log.warn(e);
				Threading.sleep (1000);
				continue;
			}
		}
		_connection.addShutdownListener(new ShutdownListener() {
			@Override
			public void shutdownCompleted(ShutdownSignalException cause) {
				Log.warn("Connection to RabbitMQ failed!");
				while (true) {
					try {
						setupConnection();
						break;
					} catch (IOException e) {
						Log.warn(e);
						Threading.sleep (1000);
						continue;
					}
				}
			}
		});
	}

	protected class ConnectorEndPoint extends ByteArrayEndPoint implements
			ConnectedEndPoint, Runnable {
		volatile org.eclipse.jetty.io.Connection _jettyConnection;
		private QueueMessage _message;

		public ConnectorEndPoint(QueueMessage msg) {
			super(msg.get_http_request(), 128);
			_jettyConnection = newConnection(this);
			set_message(msg);

			setGrowOutput(true);
		}

		public org.eclipse.jetty.io.Connection getConnection() {
			return _jettyConnection;
		}

		private void sendResponse() throws IOException, JSONException {
			final QueueMessage msg = this.get_message();

			final Channel c = msg.get_channel();

			c.basicPublish(
					msg.get_callback_exchange(),
					msg.get_callback_routing_key(),
					null,
					MessageHandler.encodeMessage(this.getOut().array(),
							msg.get_callback_identifier()));
		}

		@Override
		public void close() throws IOException {
			// Log.info("connector close() called!");
			if (!_closed) {
				try {
					sendResponse();
				} catch (JSONException e) {
					// TODO Handle this...
					Log.warn(e);
				}
			}
			super.close();
		}

		public void setConnection(org.eclipse.jetty.io.Connection connection) {
			if (_jettyConnection != connection) {
				connectionUpgraded(_jettyConnection, connection);
			}
			_jettyConnection = connection;
		}

		public void dispatch() throws IOException {
			if (getThreadPool() == null || !getThreadPool().dispatch(this)) {
				Log.warn("dispatch failed for {}", _jettyConnection);
				close();
			}

		}

		@Override
		public void run() {
			// Log.warn("Running...");
			try {
				connectionOpened(getConnection());
				_connections.add(this);

				while (isStarted() && isOpen()) {
					if (_jettyConnection.isIdle()) {
						if (isLowResources()) {
							setMaxIdleTime(getLowResourcesMaxIdleTime());
						}
					}

					_jettyConnection = _jettyConnection.handle();

				}

			} catch (EofException e) {
				Log.debug("EOF", e);
				try {
					close();
				} catch (IOException e2) {
					Log.ignore(e2);
				}

			} catch (Exception e) {
				Log.warn("handle failed?", e);
				try {
					close();
				} catch (IOException e2) {
					Log.ignore(e2);
				}
			}

			finally {
				connectionClosed(_jettyConnection);
				_connections.remove(this);
			}
		}

		public void set_message(QueueMessage _message) {
			this._message = _message;
		}

		public QueueMessage get_message() {
			return _message;
		}

	}

}
