/*
 * #%L
 * mosaic-drivers-stubs-memcache
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

package eu.mosaic_cloud.drivers.kvstore.interop;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.drivers.interop.DriverConnectionData;
import eu.mosaic_cloud.drivers.kvstore.AbstractKeyValueDriver;
import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.drivers.kvstore.MemcachedDriver;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ConnectionException;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.core.utils.EncodingMetadata;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.SetRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.MemcachedPayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.MemcachedPayloads.AddRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.MemcachedPayloads.AppendRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.MemcachedPayloads.CasRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.MemcachedPayloads.PrependRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.MemcachedPayloads.ReplaceRequest;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.platform.interop.specs.kvstore.MemcachedMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.MemcachedSession;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;


/**
 * Stub for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used for communicating with a memcached
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedStub
		extends KeyValueStub
{
	/**
	 * Creates a new stub for the Memcached driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @param transmitter
	 *            the transmitter object which will send responses to requests
	 *            submitted to this stub
	 * @param driver
	 *            the driver used for processing requests submitted to this stub
	 * @param commChannel
	 *            the channel for communicating with connectors
	 */
	public MemcachedStub (final IConfiguration config, final KeyValueResponseTransmitter transmitter, final AbstractKeyValueDriver driver, final ZeroMqChannel commChannel)
	{
		super (config, transmitter, driver, commChannel);
	}
	
	@Override
	public synchronized void destroy ()
	{
		synchronized (AbstractDriverStub.MONITOR) {
			final int ref = AbstractDriverStub.decDriverReference (this);
			if ((ref == 0)) {
				final DriverConnectionData cData = KeyValueStub.readConnectionData (this.configuration);
				MemcachedStub.stubs.remove (cData);
			}
		}
		super.destroy ();
	}
	
	@Override
	@SuppressWarnings ("unchecked")
	protected void startOperation (final Message message, final Session session)
			throws IOException,
				ClassNotFoundException
	{
		Preconditions.checkArgument ((message.specification instanceof KeyValueMessage) || (message.specification instanceof MemcachedMessage));
		byte[] data;
		CompletionToken token;
		String key;
		int exp;
		IResult<Boolean> resultStore;
		DriverOperationFinishedHandler callback;
		final eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage messageData;
		final MemcachedDriver driver = super.getDriver (MemcachedDriver.class);
		final String mssgPrefix = "MemcachedStub - Received request for ";
		if (message.specification instanceof KeyValueMessage) {
			// NOTE: handle set with exp
			boolean handle = false;
			final KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
			if (kvMessage == KeyValueMessage.SET_REQUEST) {
				final KeyValuePayloads.SetRequest setRequest = (SetRequest) message.payload;
				if (setRequest.hasExpTime ()) {
					token = setRequest.getToken ();
					key = setRequest.getKey ();
					MemcachedStub.logger.trace (mssgPrefix + " SET key: " + key + " - request id: " + token.getMessageId () + " client id: " + token.getClientId ());
					exp = setRequest.getExpTime ();
					data = setRequest.getValue ().toByteArray ();
					messageData = new eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage (key, data, setRequest.getEnvelope ().getContentEncoding (), setRequest.getEnvelope ().getContentType ());
					callback = new DriverOperationFinishedHandler (token, session, MemcachedDriver.class, MemcachedResponseTransmitter.class);
					resultStore = driver.invokeSetOperation (token.getClientId (), messageData, exp, callback);
					callback.setDetails (KeyValueOperations.SET, resultStore);
					handle = true;
				}
			} else if (kvMessage == KeyValueMessage.GET_REQUEST) {
				final KeyValuePayloads.GetRequest getRequest = (GetRequest) message.payload;
				if (getRequest.getKeyCount () > 1) {
					token = getRequest.getToken ();
					MemcachedStub.logger.trace (mssgPrefix + "GET_BULK " + " - request id: " + token.getMessageId () + " client id: " + token.getClientId ());
					callback = new DriverOperationFinishedHandler (token, session, MemcachedDriver.class, MemcachedResponseTransmitter.class);
					final EncodingMetadata expectedEncoding = new EncodingMetadata (getRequest.getEnvelope ().getContentType (), getRequest.getEnvelope ().getContentEncoding ());
					final IResult<Map<String, eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage>> resultGet = driver.invokeGetBulkOperation (token.getClientId (), getRequest.getKeyList (), expectedEncoding, callback);
					callback.setDetails (KeyValueOperations.GET_BULK, resultGet);
					handle = true;
				}
			}
			if (!handle) {
				this.handleKVOperation (message, session, driver, MemcachedResponseTransmitter.class);
			}
			return;
		}
		final MemcachedMessage mcMessage = (MemcachedMessage) message.specification;
		switch (mcMessage) {
			case ADD_REQUEST :
				final MemcachedPayloads.AddRequest addRequest = (AddRequest) message.payload;
				token = addRequest.getToken ();
				key = addRequest.getKey ();
				MemcachedStub.logger.trace (mssgPrefix + mcMessage.toString () + " key: " + key + " - request id: " + token.getMessageId () + " client id: " + token.getClientId ());
				exp = addRequest.getExpTime ();
				data = addRequest.getValue ().toByteArray ();
				messageData = new eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage (key, data, addRequest.getEnvelope ().getContentEncoding (), addRequest.getEnvelope ().getContentType ());
				callback = new DriverOperationFinishedHandler (token, session, MemcachedDriver.class, MemcachedResponseTransmitter.class);
				resultStore = driver.invokeAddOperation (token.getClientId (), messageData, exp, callback);
				callback.setDetails (KeyValueOperations.ADD, resultStore);
				break;
			case APPEND_REQUEST :
				final MemcachedPayloads.AppendRequest appendRequest = (AppendRequest) message.payload;
				token = appendRequest.getToken ();
				key = appendRequest.getKey ();
				MemcachedStub.logger.trace (mssgPrefix + mcMessage.toString () + " key: " + key + " - request id: " + token.getMessageId () + " client id: " + token.getClientId ());
				data = appendRequest.getValue ().toByteArray ();
				messageData = new eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage (key, data, appendRequest.getEnvelope ().getContentEncoding (), appendRequest.getEnvelope ().getContentType ());
				callback = new DriverOperationFinishedHandler (token, session, MemcachedDriver.class, MemcachedResponseTransmitter.class);
				resultStore = driver.invokeAppendOperation (token.getClientId (), messageData, callback);
				callback.setDetails (KeyValueOperations.APPEND, resultStore);
				break;
			case PREPEND_REQUEST :
				final MemcachedPayloads.PrependRequest prependRequest = (PrependRequest) message.payload;
				token = prependRequest.getToken ();
				key = prependRequest.getKey ();
				MemcachedStub.logger.trace (mssgPrefix + mcMessage.toString () + " key: " + key + " - request id: " + token.getMessageId () + " client id: " + token.getClientId ());
				data = prependRequest.getValue ().toByteArray ();
				messageData = new eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage (key, data, prependRequest.getEnvelope ().getContentEncoding (), prependRequest.getEnvelope ().getContentType ());
				callback = new DriverOperationFinishedHandler (token, session, MemcachedDriver.class, MemcachedResponseTransmitter.class);
				resultStore = driver.invokePrependOperation (token.getClientId (), messageData, callback);
				callback.setDetails (KeyValueOperations.PREPEND, resultStore);
				break;
			case REPLACE_REQUEST :
				final MemcachedPayloads.ReplaceRequest replaceRequest = (ReplaceRequest) message.payload;
				token = replaceRequest.getToken ();
				key = replaceRequest.getKey ();
				MemcachedStub.logger.trace (mssgPrefix + mcMessage.toString () + " key: " + key + " - request id: " + token.getMessageId () + " client id: " + token.getClientId ());
				exp = replaceRequest.getExpTime ();
				data = replaceRequest.getValue ().toByteArray ();
				messageData = new eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage (key, data, replaceRequest.getEnvelope ().getContentEncoding (), replaceRequest.getEnvelope ().getContentType ());
				callback = new DriverOperationFinishedHandler (token, session, MemcachedDriver.class, MemcachedResponseTransmitter.class);
				resultStore = driver.invokeReplaceOperation (token.getClientId (), messageData, exp, callback);
				callback.setDetails (KeyValueOperations.REPLACE, resultStore);
				break;
			case CAS_REQUEST :
				final MemcachedPayloads.CasRequest casRequest = (CasRequest) message.payload;
				token = casRequest.getToken ();
				key = casRequest.getKey ();
				MemcachedStub.logger.trace (mssgPrefix + mcMessage.toString () + " key: " + key + " - request id: " + token.getMessageId () + " client id: " + token.getClientId ());
				data = casRequest.getValue ().toByteArray ();
				messageData = new eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage (key, data, casRequest.getEnvelope ().getContentEncoding (), casRequest.getEnvelope ().getContentType ());
				callback = new DriverOperationFinishedHandler (token, session, MemcachedDriver.class, MemcachedResponseTransmitter.class);
				resultStore = driver.invokeCASOperation (token.getClientId (), messageData, callback);
				callback.setDetails (KeyValueOperations.CAS, resultStore);
				break;
			default:
				break;
		}
	}
	
	/**
	 * Returns a stub for the Memcached driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @param channel
	 *            the channel used by the driver for receiving requests
	 * @return the Memcached driver stub
	 */
	public static MemcachedStub create (final IConfiguration config, final ZeroMqChannel channel, final ThreadingContext threading)
	{
		final DriverConnectionData cData = KeyValueStub.readConnectionData (config);
		MemcachedStub stub;
		synchronized (AbstractDriverStub.MONITOR) {
			stub = MemcachedStub.stubs.get (cData);
			try {
				if (stub == null) {
					MemcachedStub.logger.trace ("MemcachedStub: create new stub.");
					final MemcachedResponseTransmitter transmitter = new MemcachedResponseTransmitter ();
					final MemcachedDriver driver = MemcachedDriver.create (config, threading);
					stub = new MemcachedStub (config, transmitter, driver, channel);
					MemcachedStub.stubs.put (cData, stub);
					AbstractDriverStub.incDriverReference (stub);
					channel.accept (KeyValueSession.DRIVER, stub);
					channel.accept (MemcachedSession.DRIVER, stub);
				} else {
					MemcachedStub.logger.trace ("MemcachedStub: use existing stub.");
					AbstractDriverStub.incDriverReference (stub);
				}
			} catch (final Exception e) {
				FallbackExceptionTracer.defaultInstance.traceDeferredException (e);
				final ConnectionException e1 = new ConnectionException ("The Memcached proxy cannot connect to the driver: " + e.getMessage (), e);
				FallbackExceptionTracer.defaultInstance.traceIgnoredException (e1);
			}
		}
		return stub;
	}
	
	public static MemcachedStub createDetached (final IConfiguration config, final ZeroMqChannel channel, final ThreadingContext threading)
	{
		MemcachedStub stub;
		try {
			MemcachedStub.logger.trace ("MemcachedStub: create new stub.");
			final MemcachedResponseTransmitter transmitter = new MemcachedResponseTransmitter ();
			final MemcachedDriver driver = MemcachedDriver.create (config, threading);
			stub = new MemcachedStub (config, transmitter, driver, channel);
			AbstractDriverStub.incDriverReference (stub);
			channel.accept (KeyValueSession.DRIVER, stub);
			channel.accept (MemcachedSession.DRIVER, stub);
		} catch (final Exception e) {
			FallbackExceptionTracer.defaultInstance.traceDeferredException (e);
			final ConnectionException e1 = new ConnectionException ("The Memcached proxy cannot connect to the driver: " + e.getMessage (), e);
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (e1);
			stub = null;
		}
		return stub;
	}
	
	private static final Logger logger = Transcript.create (MemcachedStub.class).adaptAs (Logger.class);
	private static final Map<DriverConnectionData, MemcachedStub> stubs = new HashMap<DriverConnectionData, MemcachedStub> ();
}
