package eu.mosaic_cloud.jetty.connectors.httpg;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import eu.mosaic_cloud.jetty.connectors.httpg.MessageHandler.MessageFormatException;

import org.eclipse.jetty.io.ByteArrayEndPoint;
import org.eclipse.jetty.io.ConnectedEndPoint;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;


public class AmqpConnector extends AbstractConnector {
	protected final Set<EndPoint> _connections;

	private ConnectionFactory _connectionFactory = null;
	private Connection _connection = null;
	private Channel _channel = null;
	private QueueingConsumer _consumer = null;
	private String _hostName;
	private String _userName;
	private String _userPassword;
	private String _routingKey;
	private String _exchangeName;
	private String _inputQueueName;
	private boolean _autoDeclareQueue;

	public AmqpConnector(String exchangeName, String routingKey,
			String queueName, String hostName, String userName,
			String userPassword, int port, boolean autoDeclareQueue) {
		_userName = userName;
		_userPassword = userPassword;
		_exchangeName = exchangeName;
		_routingKey = routingKey;
		_hostName = hostName;
		_inputQueueName = queueName;
		_autoDeclareQueue = autoDeclareQueue;
		_connections = new HashSet<EndPoint>();
	}

	@Override
	protected void accept(int acceptorID) throws IOException,
			InterruptedException {
		QueueMessage msg = null;
		// Log.info("Waiting for messages");
		final QueueingConsumer.Delivery delivery = _consumer.nextDelivery();
		try {
			msg = MessageHandler.decodeMessage(delivery);
			msg.set_channel(_channel);
		} catch (MessageFormatException e) {
			Log.warn("Could not decode message: " + e.getMessage());
		} catch (IOException e) {
			Log.warn("Could not read message: " + e.getMessage());
			e.printStackTrace();
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
		setupConnection();
		_connection.addShutdownListener(new ShutdownListener() {

			@Override
			public void shutdownCompleted(ShutdownSignalException cause) {
				Log.warn("Connection to RabbitMQ failed!");
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				try {
					setupConnection();
				} catch (IOException e) {
					Log.warn("Faild connecting... Going to sleep and retry");
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e1) {

					}
					shutdownCompleted(cause);
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
				e.printStackTrace();
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
				synchronized (_connections) {
					_connections.add(this);
				}

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
				synchronized (_connections) {
					_connections.remove(this);
				}
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
