/*
 * #%L
 * mosaic-driver
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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
package eu.mosaic_cloud.driver.interop.kvstore; // NOPMD

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.core.configuration.ConfigUtils;
import eu.mosaic_cloud.core.configuration.IConfiguration;
import eu.mosaic_cloud.core.exceptions.ConnectionException;
import eu.mosaic_cloud.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.core.log.MosaicLogger;
import eu.mosaic_cloud.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.core.ops.IResult;
import eu.mosaic_cloud.driver.ConfigProperties;
import eu.mosaic_cloud.driver.DriverNotFoundException;
import eu.mosaic_cloud.driver.interop.AbstractDriverStub;
import eu.mosaic_cloud.driver.interop.DriverConnectionData;
import eu.mosaic_cloud.driver.kvstore.AbstractKeyValueDriver;
import eu.mosaic_cloud.driver.kvstore.KeyValueDriverFactory;
import eu.mosaic_cloud.driver.kvstore.KeyValueOperations;
import eu.mosaic_cloud.interop.idl.IdlCommon;
import eu.mosaic_cloud.interop.idl.IdlCommon.AbortRequest;
import eu.mosaic_cloud.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.DeleteRequest;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.GetRequest;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.ListRequest;
import eu.mosaic_cloud.interop.idl.kvstore.KeyValuePayloads.SetRequest;
import eu.mosaic_cloud.interop.kvstore.KeyValueMessage;
import eu.mosaic_cloud.interop.kvstore.KeyValueSession;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Stub for the driver for key-value distributed storage systems. This is used
 * for communicating with a key-value driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueStub extends AbstractDriverStub { // NOPMD 

	private static Map<DriverConnectionData, KeyValueStub> stubs = new HashMap<DriverConnectionData, KeyValueStub>();

	private Class<? extends AbstractKeyValueDriver> driverClass;

	/**
	 * Creates a new stub for the key-value store driver.
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
	public KeyValueStub(IConfiguration config,
			KeyValueResponseTransmitter transmitter,
			AbstractKeyValueDriver driver, ZeroMqChannel commChannel) {
		super(config, transmitter, driver, commChannel);
	}

	/**
	 * Returns a stub for the key-value store driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @param channel
	 *            the channel used by the driver for receiving requests
	 * @return the driver stub
	 */
	public static KeyValueStub create(IConfiguration config,
			ZeroMqChannel channel) {
		DriverConnectionData cData = KeyValueStub.readConnectionData(config);
		KeyValueStub stub;
		synchronized (AbstractDriverStub.LOCK) {
			stub = KeyValueStub.stubs.get(cData);
			try {
				if (stub == null) {
					MosaicLogger.getLogger().trace(
							"KeyValueStub: create new stub."); //$NON-NLS-1$

					KeyValueResponseTransmitter transmitter = new KeyValueResponseTransmitter();
					String driverName = ConfigUtils
							.resolveParameter(
									config,
									ConfigProperties
											.getString("KVStoreDriver.6"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
					AbstractKeyValueDriver driver = KeyValueDriverFactory
							.createDriver(driverName, config);
					stub = new KeyValueStub(config, transmitter, driver,
							channel);
					stub.driverClass = KeyValueDriverFactory.DriverType
							.valueOf(driverName.toUpperCase(Locale.ENGLISH))
							.getDriverClass();
					KeyValueStub.stubs.put(cData, stub);
					incDriverReference(stub);
					channel.accept(KeyValueSession.DRIVER, stub);
				} else {
					MosaicLogger.getLogger().trace(
							"KeyValueStub: use existing stub."); //$NON-NLS-1$
					incDriverReference(stub);
				}
			} catch (DriverNotFoundException e) {
				ExceptionTracer.traceDeferred(e);
				ConnectionException e1 = new ConnectionException("The required key-value driver cannot be provided: " + e.getMessage(), e);
				ExceptionTracer.traceIgnored(e1);
			}
		}
		return stub;
	}

	@Override
	public void destroy() {
		synchronized (AbstractDriverStub.LOCK) {
			int ref = decDriverReference(this);
			if ((ref == 0)) {
				DriverConnectionData cData = KeyValueStub
						.readConnectionData(this.configuration);
				KeyValueStub.stubs.remove(cData);
			}

		}
		super.destroy();
	}

	@Override
	protected void startOperation(Message message, Session session)
			throws IOException, ClassNotFoundException {
		Preconditions
				.checkArgument(message.specification instanceof KeyValueMessage);
		AbstractKeyValueDriver driver = super.getDriver(this.driverClass);
		handleKVOperation(message, session, driver,
				KeyValueResponseTransmitter.class);
	}

	/**
	 * Handles basic key-value store operations.
	 * 
	 * @param message
	 *            the message containing the operation request
	 * @param session
	 *            the session
	 * @param driver
	 *            the driver to handle the operation request
	 * @param transmitterClass
	 *            class of the response transmitter
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	protected void handleKVOperation(Message message, Session session,
			AbstractKeyValueDriver driver,
			Class<? extends KeyValueResponseTransmitter> transmitterClass)
			throws IOException, ClassNotFoundException {
		byte[] data;
		boolean unknownMessage = false; // NOPMD by georgiana on 9/30/11 2:36 PM
		KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
		CompletionToken token = null; // NOPMD by georgiana on 9/30/11 2:37 PM
		String key;
		String messagePrefix = "KeyValueStub - Received request for "; // NOPMD by georgiana on 10/12/11 2:11 PM

		switch (kvMessage) {
		case ACCESS:
			MosaicLogger.getLogger().trace("Received initiation message");
			KeyValuePayloads.InitRequest initRequest = (InitRequest) message.payload;
			token = initRequest.getToken();
			String bucket = initRequest.getBucket();
			driver.registerClient(token.getClientId(), bucket);
			break;
		case ABORTED:
			MosaicLogger.getLogger().trace("Received termination message");
			IdlCommon.AbortRequest abortRequest = (AbortRequest) message.payload;
			token = abortRequest.getToken();
			driver.unregisterClient(token.getClientId());
			break;
		case SET_REQUEST:
			KeyValuePayloads.SetRequest setRequest = (SetRequest) message.payload;
			token = setRequest.getToken();
			key = setRequest.getKey();
			data = setRequest.getValue().toByteArray();

			MosaicLogger.getLogger().trace(
					messagePrefix + kvMessage.toString() + " key: " + key);

			// execute operation
			DriverOperationFinishedHandler setCallback = new DriverOperationFinishedHandler(
					token, session, driver.getClass(), transmitterClass);
			IResult<Boolean> resultSet = driver.invokeSetOperation(
					token.getClientId(), key, data, setCallback);
			setCallback.setDetails(KeyValueOperations.SET, resultSet);
			break;
		case GET_REQUEST:
			KeyValuePayloads.GetRequest getRequest = (GetRequest) message.payload;
			token = getRequest.getToken();
			DriverOperationFinishedHandler getCallback = new DriverOperationFinishedHandler(
					token, session, driver.getClass(), transmitterClass);

			if (getRequest.getKeyCount() != 1) {
				// error - the simple driver can handle only single-key get
				MosaicLogger.getLogger().error(
						"Basic driver can handle only single-key GET.");
				driver.handleUnsupportedOperationError(kvMessage.toString(),
						getCallback);
				break;
			}
			key = getRequest.getKey(0);

			MosaicLogger.getLogger().trace(
					messagePrefix + kvMessage.toString() + " key: " + key);

			IResult<byte[]> resultGet = driver.invokeGetOperation(
					token.getClientId(), key, getCallback);
			getCallback.setDetails(KeyValueOperations.GET, resultGet);
			break;
		case DELETE_REQUEST:
			KeyValuePayloads.DeleteRequest delRequest = (DeleteRequest) message.payload;
			token = delRequest.getToken();
			key = delRequest.getKey();

			MosaicLogger.getLogger().trace(
					messagePrefix + kvMessage.toString() + " key: " + key);

			DriverOperationFinishedHandler delCallback = new DriverOperationFinishedHandler(
					token, session, driver.getClass(), transmitterClass);
			IResult<Boolean> resultDelete = driver.invokeDeleteOperation(
					token.getClientId(), key, delCallback);
			delCallback.setDetails(KeyValueOperations.DELETE, resultDelete);
			break;
		case LIST_REQUEST:
			KeyValuePayloads.ListRequest listRequest = (ListRequest) message.payload;
			token = listRequest.getToken();

			MosaicLogger.getLogger().trace(
					messagePrefix + kvMessage.toString() + " - request id: "
							+ token.getMessageId() + " client id: "
							+ token.getClientId());

			DriverOperationFinishedHandler listCallback = new DriverOperationFinishedHandler(
					token, session, driver.getClass(), transmitterClass);
			IResult<List<String>> resultList = driver.invokeListOperation(
					token.getClientId(), listCallback);
			listCallback.setDetails(KeyValueOperations.LIST, resultList);
			break;
		case ERROR:
			token = ((IdlCommon.Error) message.payload).getToken(); // NOPMD
			unknownMessage = true;
			break;
		case OK:
			token = ((IdlCommon.Ok) message.payload).getToken(); // NOPMD
			unknownMessage = true;
			break;
		case GET_REPLY:
			token = ((KeyValuePayloads.GetReply) message.payload).getToken(); // NOPMD
			unknownMessage = true;
			break;
		case LIST_REPLY:
			token = ((KeyValuePayloads.ListReply) message.payload).getToken(); // NOPMD
			unknownMessage = true;
			break;
		default:
			break;
		}

		if (unknownMessage) {
			handleUnknownMessage(session, driver, kvMessage.toString(), token,
					transmitterClass);
		}
	}

	protected void handleUnknownMessage(Session session,
			AbstractKeyValueDriver driver, String messageType,
			CompletionToken token,
			Class<? extends KeyValueResponseTransmitter> transmitterClass) {
		MosaicLogger.getLogger().error(
				"Unexpected message type: " + messageType);
		// create callback
		DriverOperationFinishedHandler failCallback = new DriverOperationFinishedHandler(
				token, session, driver.getClass(), transmitterClass);
		driver.handleUnsupportedOperationError(messageType, failCallback);
	}

	/**
	 * Reads resource connection data from the configuration data.
	 * 
	 * @param config
	 *            the configuration data
	 * @return resource connection data
	 */
	protected static DriverConnectionData readConnectionData(
			IConfiguration config) {
		String resourceHost = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.0"), String.class, //$NON-NLS-1$
				"localhost"); //$NON-NLS-1$
		int resourcePort = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.1"), Integer.class, //$NON-NLS-1$
				0);
		String driver = ConfigUtils
				.resolveParameter(
						config,
						ConfigProperties.getString("KVStoreDriver.6"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
		String user = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.5"), String.class, //$NON-NLS-1$
				""); //$NON-NLS-1$
		String passwd = ConfigUtils.resolveParameter(config,
				ConfigProperties.getString("KVStoreDriver.4"), String.class, //$NON-NLS-1$
				""); //$NON-NLS-1$
		// String bucket = ConfigUtils.resolveParameter(config,
		//				ConfigProperties.getString("KVStoreDriver.3"), String.class, //$NON-NLS-1$
		// "");
		DriverConnectionData cData;
		if ("".equals(user) && "".equals(passwd)) {
			cData = new DriverConnectionData(resourceHost, resourcePort, driver);
		} else {
			cData = new DriverConnectionData(resourceHost, resourcePort,
					driver, user, passwd);
		}
		return cData;
	}

	/**
	 * Handler for processing responses of the requests submitted to the stub.
	 * This will basically call the transmitter associated with the stub.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	@SuppressWarnings("rawtypes")
	protected class DriverOperationFinishedHandler implements
			IOperationCompletionHandler {

		private IResult<?> result;
		private KeyValueOperations operation;
		private final CompletionToken complToken;
		private final CountDownLatch signal;
		private final AbstractKeyValueDriver driver;
		private final KeyValueResponseTransmitter transmitter;
		private final Session session;

		public DriverOperationFinishedHandler(CompletionToken complToken,
				Session session,
				Class<? extends AbstractKeyValueDriver> driverClass,
				Class<? extends KeyValueResponseTransmitter> transmitterClass) {
			this.complToken = complToken;
			this.signal = new CountDownLatch(1);
			this.driver = KeyValueStub.this.getDriver(driverClass);
			this.transmitter = KeyValueStub.this
					.getResponseTransmitter(transmitterClass);
			this.session = session;
		}

		public void setDetails(KeyValueOperations operation, IResult<?> result) {
			this.operation = operation;
			this.result = result;
			this.signal.countDown();
		}

		@Override
		public void onSuccess(Object response) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				ExceptionTracer.traceIgnored(e);
			}
			this.driver.removePendingOperation(this.result);

			if (this.operation.equals(KeyValueOperations.GET)) {
				Map<String, Object> resMap = new HashMap<String, Object>();
				resMap.put("dummy", response); //$NON-NLS-1$
				this.transmitter.sendResponse(this.session, this.complToken,
						this.operation, resMap, false);
			} else {
				this.transmitter.sendResponse(this.session, this.complToken,
						this.operation, response, false);
			}

		}

		@Override
		public void onFailure(Throwable error) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				ExceptionTracer.traceIgnored(e);
			}

			this.driver.removePendingOperation(this.result);
			// result is error
			this.transmitter.sendResponse(this.session, this.complToken,
					this.operation, error.getMessage(), true);
		}
	}

}
