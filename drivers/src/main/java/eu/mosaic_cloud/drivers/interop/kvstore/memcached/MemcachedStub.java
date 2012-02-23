/*
 * #%L
 * mosaic-drivers
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
package eu.mosaic_cloud.drivers.interop.kvstore.memcached; // NOPMD by georgiana on 10/12/11 2:55 PM

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.platform.interop.specs.kvstore.MemcachedMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.MemcachedSession;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.drivers.interop.DriverConnectionData;
import eu.mosaic_cloud.drivers.interop.kvstore.KeyValueResponseTransmitter;
import eu.mosaic_cloud.drivers.interop.kvstore.KeyValueStub;
import eu.mosaic_cloud.drivers.kvstore.AbstractKeyValueDriver;
import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.drivers.kvstore.memcached.MemcachedDriver;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ConnectionException;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IResult;
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
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

/**
 * Stub for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used for communicating with a memcached
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedStub extends KeyValueStub { // NOPMD by georgiana on
													// 10/12/11 2:56 PM

	private static Map<DriverConnectionData, MemcachedStub> stubs = new HashMap<DriverConnectionData, MemcachedStub>();

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
	public MemcachedStub(IConfiguration config,
			KeyValueResponseTransmitter transmitter,
			AbstractKeyValueDriver driver, ZeroMqChannel commChannel) {
		super(config, transmitter, driver, commChannel);

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
	public static MemcachedStub create(IConfiguration config,
			ZeroMqChannel channel, ThreadingContext threading) {
		DriverConnectionData cData = KeyValueStub.readConnectionData(config);
		MosaicLogger sLogger = MosaicLogger.createLogger(MemcachedStub.class);
		MemcachedStub stub;
		synchronized (AbstractDriverStub.MONITOR) {
			stub = MemcachedStub.stubs.get(cData);
			try {
				if (stub == null) {
					sLogger.trace("MemcachedStub: create new stub.");

					MemcachedResponseTransmitter transmitter = new MemcachedResponseTransmitter();
					MemcachedDriver driver = MemcachedDriver.create(config,
							threading);
					stub = new MemcachedStub(config, transmitter, driver,
							channel);
					MemcachedStub.stubs.put(cData, stub);
					incDriverReference(stub);
					channel.accept(KeyValueSession.DRIVER, stub);
					channel.accept(MemcachedSession.DRIVER, stub);
				} else {
					sLogger.trace("MemcachedStub: use existing stub.");
					incDriverReference(stub);
				}
			} catch (IOException e) {
				ExceptionTracer.traceDeferred(e);
				ConnectionException e1 = new ConnectionException(
						"The Memcached proxy cannot connect to the driver: "
								+ e.getMessage(), e);
				ExceptionTracer.traceIgnored(e1);
			}
		}
		return stub;
	}

	@Override
	public synchronized void destroy() {
		synchronized (AbstractDriverStub.MONITOR) {
			int ref = decDriverReference(this);
			if ((ref == 0)) {
				DriverConnectionData cData = KeyValueStub
						.readConnectionData(this.configuration);
				MemcachedStub.stubs.remove(cData);
			}

		}
		super.destroy();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void startOperation(Message message, Session session) // NOPMD by
																	// georgiana
																	// on
																	// 10/12/11
																	// 2:55 PM
			throws IOException, ClassNotFoundException {
		Preconditions
				.checkArgument((message.specification instanceof KeyValueMessage)
						|| (message.specification instanceof MemcachedMessage));

		byte[] data;
		CompletionToken token;
		String key;
		int exp;
		IResult<Boolean> resultStore;
		DriverOperationFinishedHandler callback;

		MemcachedDriver driver = super.getDriver(MemcachedDriver.class); // NOPMD
																			// by
																			// georgiana
																			// on
																			// 10/12/11
																			// 2:54
																			// PM
		String mssgPrefix = "MemcachedStub - Received request for "; // NOPMD by
																		// georgiana
																		// on
																		// 10/12/11
																		// 2:54
																		// PM
		if (message.specification instanceof KeyValueMessage) {
			// handle set with exp
			boolean handle = false; // NOPMD by georgiana on 10/12/11 2:54 PM
			KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
			if (kvMessage == KeyValueMessage.SET_REQUEST) {
				KeyValuePayloads.SetRequest setRequest = (SetRequest) message.payload;
				if (setRequest.hasExpTime()) {
					token = setRequest.getToken();
					key = setRequest.getKey();

					this.logger.trace(mssgPrefix + " SET key: " + key + " - request id: " + token.getMessageId () + " client id: "
							+ token.getClientId());

					exp = setRequest.getExpTime();
					data = setRequest.getValue().toByteArray();

					callback = new DriverOperationFinishedHandler(token,
							session, MemcachedDriver.class,
							MemcachedResponseTransmitter.class);
					resultStore = driver.invokeSetOperation(
							token.getClientId(), key, exp, data, callback);
					callback.setDetails(KeyValueOperations.SET, resultStore);
					handle = true;
				}
			} else if (kvMessage == KeyValueMessage.GET_REQUEST) {
				KeyValuePayloads.GetRequest getRequest = (GetRequest) message.payload;
				if (getRequest.getKeyCount() > 1) {
					token = getRequest.getToken();
					this.logger.trace(mssgPrefix + "GET_BULK " + " - request id: " + token.getMessageId () + " client id: "
							+ token.getClientId());

					callback = new DriverOperationFinishedHandler(token,
							session, MemcachedDriver.class,
							MemcachedResponseTransmitter.class);
					IResult<Map<String, byte[]>> resultGet = driver
							.invokeGetBulkOperation(token.getClientId(),
									getRequest.getKeyList(), callback);

					callback.setDetails(KeyValueOperations.GET_BULK, resultGet);
					handle = true;
				}
			}

			if (!handle) {
				handleKVOperation(message, session, driver,
						MemcachedResponseTransmitter.class);
			}
			return;
		}

		MemcachedMessage mcMessage = (MemcachedMessage) message.specification;

		switch (mcMessage) {
		case ADD_REQUEST:
			MemcachedPayloads.AddRequest addRequest = (AddRequest) message.payload;
			token = addRequest.getToken();
			key = addRequest.getKey();

			this.logger.trace(mssgPrefix + mcMessage.toString() + " key: "
					+ key + " - request id: " + token.getMessageId () + " client id: "
					+ token.getClientId()); // NOPMD by georgiana on 10/12/11 2:56 PM

			exp = addRequest.getExpTime();
			data = addRequest.getValue().toByteArray();

			callback = new DriverOperationFinishedHandler(token, session,
					MemcachedDriver.class, MemcachedResponseTransmitter.class);
			resultStore = driver.invokeAddOperation(token.getClientId(), key,
					exp, data, callback);
			callback.setDetails(KeyValueOperations.ADD, resultStore);
			break;
		case APPEND_REQUEST:
			MemcachedPayloads.AppendRequest appendRequest = (AppendRequest) message.payload;
			token = appendRequest.getToken();
			key = appendRequest.getKey();

			this.logger.trace(mssgPrefix + mcMessage.toString() + " key: "
					+ key + " - request id: " + token.getMessageId () + " client id: "
					+ token.getClientId());

			exp = appendRequest.getExpTime(); // NOPMD by georgiana on 10/12/11
												// 2:54 PM
			data = appendRequest.getValue().toByteArray();

			callback = new DriverOperationFinishedHandler(token, session,
					MemcachedDriver.class, MemcachedResponseTransmitter.class);
			resultStore = driver.invokeAppendOperation(token.getClientId(),
					key, data, callback);
			callback.setDetails(KeyValueOperations.APPEND, resultStore);
			break;
		case PREPEND_REQUEST:
			MemcachedPayloads.PrependRequest prependRequest = (PrependRequest) message.payload;
			token = prependRequest.getToken();
			key = prependRequest.getKey();

			this.logger.trace(mssgPrefix + mcMessage.toString() + " key: "
					+ key + " - request id: " + token.getMessageId () + " client id: "
					+ token.getClientId());

			exp = prependRequest.getExpTime(); // NOPMD by georgiana on 10/12/11
												// 2:55 PM
			data = prependRequest.getValue().toByteArray();

			callback = new DriverOperationFinishedHandler(token, session,
					MemcachedDriver.class, MemcachedResponseTransmitter.class);
			resultStore = driver.invokePrependOperation(token.getClientId(),
					key, data, callback);
			callback.setDetails(KeyValueOperations.PREPEND, resultStore);
			break;
		case REPLACE_REQUEST:
			MemcachedPayloads.ReplaceRequest replaceRequest = (ReplaceRequest) message.payload;
			token = replaceRequest.getToken();
			key = replaceRequest.getKey();

			this.logger.trace(mssgPrefix + mcMessage.toString() + " key: "
					+ key + " - request id: " + token.getMessageId () + " client id: "
					+ token.getClientId());

			exp = replaceRequest.getExpTime();
			data = replaceRequest.getValue().toByteArray();

			callback = new DriverOperationFinishedHandler(token, session,
					MemcachedDriver.class, MemcachedResponseTransmitter.class);
			resultStore = driver.invokeReplaceOperation(token.getClientId(),
					key, exp, data, callback);
			callback.setDetails(KeyValueOperations.REPLACE, resultStore);
			break;
		case CAS_REQUEST:
			MemcachedPayloads.CasRequest casRequest = (CasRequest) message.payload;
			token = casRequest.getToken();
			key = casRequest.getKey();

			this.logger.trace(mssgPrefix + mcMessage.toString() + " key: "
					+ key + " - request id: " + token.getMessageId()
					+ " client id: " + token.getClientId());

			exp = casRequest.getExpTime(); // NOPMD by georgiana on 10/12/11
											// 2:55 PM
			data = casRequest.getValue().toByteArray();

			callback = new DriverOperationFinishedHandler(token, session,
					MemcachedDriver.class, MemcachedResponseTransmitter.class);
			resultStore = driver.invokeCASOperation(token.getClientId(), key,
					data, callback);
			callback.setDetails(KeyValueOperations.CAS, resultStore);
			break;
		default:
			break;
		}
	}

}
