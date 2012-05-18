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

import eu.mosaic_cloud.components.httpg.jetty.connector.MessageHandler.MessageFormatException;
import eu.mosaic_cloud.tools.threading.tools.Threading;

import org.eclipse.jetty.io.ByteArrayEndPoint;
import org.eclipse.jetty.io.ConnectedEndPoint;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.BlockingHttpConnection;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;


public class AmqpConnector
		extends AbstractConnector
{
	public AmqpConnector (final String exchangeName, final String routingKey, final String queueName, final String hostName, final String userName, final String userPassword, final int port, final String virtualHost, final boolean autoDeclareQueue)
	{
		this._userName = userName;
		this._userPassword = userPassword;
		this._exchangeName = exchangeName;
		this._routingKey = routingKey;
		this._hostName = hostName;
		this._port = port;
		this._virtualHost = virtualHost;
		this._inputQueueName = queueName;
		this._autoDeclareQueue = autoDeclareQueue;
		this._connections = Collections.synchronizedSet (new HashSet<EndPoint> ());
	}
	
	@Override
	public void close ()
			throws IOException
	{}
	
	@Override
	public Object getConnection ()
	{
		Log.debug ("getConnection()");
		return this._consumer;
	}
	
	@Override
	public int getLocalPort ()
	{
		return 0;
	}
	
	@Override
	public void open ()
			throws IOException
	{
		while (true) {
			try {
				this.setupConnection ();
				break;
			} catch (final IOException e) {
				Log.warn (e);
				Threading.sleep (1000);
				continue;
			}
		}
		this._connection.addShutdownListener (new ShutdownListener () {
			@Override
			public void shutdownCompleted (final ShutdownSignalException cause)
			{
				Log.warn ("Connection to RabbitMQ failed!");
				while (true) {
					try {
						AmqpConnector.this.setupConnection ();
						break;
					} catch (final IOException e) {
						Log.warn (e);
						Threading.sleep (1000);
						continue;
					}
				}
			}
		});
	}
	
	@Override
	protected void accept (final int acceptorID)
			throws IOException,
				InterruptedException
	{
		QueueMessage msg = null;
		QueueingConsumer.Delivery delivery;
		try {
			delivery = this._consumer.nextDelivery ();
		} catch (final ShutdownSignalException e) {
			Log.warn (e);
			throw e;
		}
		try {
			msg = MessageHandler.decodeMessage (delivery);
			msg.set_channel (this._channel);
		} catch (final MessageFormatException e) {
			Log.warn (e);
			throw new IOException (e);
		} catch (final IOException e) {
			Log.warn (e);
			throw e;
		}
		final ConnectorEndPoint _endPoint = new ConnectorEndPoint (msg);
		_endPoint.dispatch ();
	}
	
	protected org.eclipse.jetty.io.Connection newConnection (final EndPoint endp)
	{
		return new BlockingHttpConnection (this, endp, this.getServer ());
	}
	
	private void setupConnection ()
			throws IOException
	{
		Log.info ("Opening AmqpConnector");
		if (this._connectionFactory == null) {
			this._connectionFactory = new ConnectionFactory ();
			this._connectionFactory.setHost (this._hostName);
			this._connectionFactory.setPort (this._port);
			this._connectionFactory.setVirtualHost (this._virtualHost);
			this._connectionFactory.setUsername (this._userName);
			this._connectionFactory.setPassword (this._userPassword);
		}
		this._connection = this._connectionFactory.newConnection ();
		this._channel = this._connection.createChannel ();
		if (this._autoDeclareQueue) {
			this._channel.exchangeDeclare (this._exchangeName, "topic", false);
			this._inputQueueName = this._channel.queueDeclare (this._inputQueueName, false, false, false, null).getQueue ();
		}
		this._channel.queueBind (this._inputQueueName, this._exchangeName, this._routingKey);
		this._consumer = new QueueingConsumer (this._channel);
		this._channel.basicConsume (this._inputQueueName, true, this._consumer);
	}
	
	protected final Set<EndPoint> _connections;
	private final boolean _autoDeclareQueue;
	private Channel _channel = null;
	private Connection _connection = null;
	private ConnectionFactory _connectionFactory = null;
	private QueueingConsumer _consumer = null;
	private final String _exchangeName;
	private final String _hostName;
	private String _inputQueueName;
	private final int _port;
	private final String _routingKey;
	private final String _userName;
	private final String _userPassword;
	private final String _virtualHost;
	
	protected class ConnectorEndPoint
			extends ByteArrayEndPoint
			implements
				ConnectedEndPoint,
				Runnable
	{
		public ConnectorEndPoint (final QueueMessage msg)
		{
			super (msg.get_http_request (), 128);
			this._jettyConnection = AmqpConnector.this.newConnection (this);
			this.set_message (msg);
			this.setGrowOutput (true);
		}
		
		@Override
		public void close ()
				throws IOException
		{
			if (!this._closed) {
				try {
					this.sendResponse ();
				} catch (final JSONException e) {
					// FIXME: Handle this...
					Log.warn (e);
				}
			}
			super.close ();
		}
		
		public void dispatch ()
				throws IOException
		{
			if ((AmqpConnector.this.getThreadPool () == null) || !AmqpConnector.this.getThreadPool ().dispatch (this)) {
				Log.warn ("dispatch failed for {}", this._jettyConnection);
				this.close ();
			}
		}
		
		public QueueMessage get_message ()
		{
			return this._message;
		}
		
		@Override
		public org.eclipse.jetty.io.Connection getConnection ()
		{
			return this._jettyConnection;
		}
		
		@Override
		public void run ()
		{
			try {
				AmqpConnector.this.connectionOpened (this.getConnection ());
				AmqpConnector.this._connections.add (this);
				while (AmqpConnector.this.isStarted () && this.isOpen ()) {
					if (this._jettyConnection.isIdle ()) {
						if (AmqpConnector.this.isLowResources ()) {
							this.setMaxIdleTime (AmqpConnector.this.getLowResourcesMaxIdleTime ());
						}
					}
					this._jettyConnection = this._jettyConnection.handle ();
				}
			} catch (final EofException e) {
				Log.debug ("EOF", e);
				try {
					this.close ();
				} catch (final IOException e2) {
					Log.ignore (e2);
				}
			} catch (final Exception e) {
				Log.warn ("handle failed?", e);
				try {
					this.close ();
				} catch (final IOException e2) {
					Log.ignore (e2);
				}
			} finally {
				AmqpConnector.this.connectionClosed (this._jettyConnection);
				AmqpConnector.this._connections.remove (this);
			}
		}
		
		public void set_message (final QueueMessage _message)
		{
			this._message = _message;
		}
		
		@Override
		public void setConnection (final org.eclipse.jetty.io.Connection connection)
		{
			if (this._jettyConnection != connection) {
				AmqpConnector.this.connectionUpgraded (this._jettyConnection, connection);
			}
			this._jettyConnection = connection;
		}
		
		private void sendResponse ()
				throws IOException,
					JSONException
		{
			final QueueMessage msg = this.get_message ();
			final Channel c = msg.get_channel ();
			c.basicPublish (msg.get_callback_exchange (), msg.get_callback_routing_key (), null, MessageHandler.encodeMessage (this.getOut ().array (), msg.get_callback_identifier ()));
		}
		
		volatile org.eclipse.jetty.io.Connection _jettyConnection;
		private QueueMessage _message;
	}
}
