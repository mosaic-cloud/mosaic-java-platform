package mosaic.driver.interop.kvstore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.ConfigProperties;
import mosaic.driver.DriverNotFoundException;
import mosaic.driver.interop.AbstractDriverStub;
import mosaic.driver.interop.DriverConnectionData;
import mosaic.driver.kvstore.BaseKeyValueDriver;
import mosaic.driver.kvstore.KeyValueDriverFactory;
import mosaic.driver.kvstore.KeyValueOperations;
import mosaic.interop.idl.IdlCommon;
import mosaic.interop.idl.IdlCommon.AbortRequest;
import mosaic.interop.idl.IdlCommon.CompletionToken;
import mosaic.interop.idl.kvstore.KeyValuePayloads;
import mosaic.interop.idl.kvstore.KeyValuePayloads.DeleteRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.GetRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.ListRequest;
import mosaic.interop.idl.kvstore.KeyValuePayloads.SetRequest;
import mosaic.interop.kvstore.KeyValueMessage;
import mosaic.interop.kvstore.KeyValueSession;

import com.google.common.base.Preconditions;

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
public class KeyValueStub extends AbstractDriverStub {

	private static Map<DriverConnectionData, KeyValueStub> stubs = new HashMap<DriverConnectionData, KeyValueStub>();

	private Class<? extends BaseKeyValueDriver> driverClass;

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
			KeyValueResponseTransmitter transmitter, BaseKeyValueDriver driver,
			ZeroMqChannel commChannel) {
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
		KeyValueStub stub = null;
		synchronized (AbstractDriverStub.lock) {
			stub = KeyValueStub.stubs.get(cData);
			try {
				if (stub == null) {
					MosaicLogger.getLogger().trace(
							"KeyValueStub: create new stub."); //$NON-NLS-1$

					KeyValueResponseTransmitter transmitter = new KeyValueResponseTransmitter(
							config);
					String driverName = ConfigUtils
							.resolveParameter(
									config,
									ConfigProperties
											.getString("KVStoreDriver.6"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
					BaseKeyValueDriver driver = KeyValueDriverFactory
							.createDriver(driverName, config);
					stub = new KeyValueStub(config, transmitter, driver,
							channel);
					stub.driverClass = KeyValueDriverFactory.DriverType
							.valueOf(driverName.toUpperCase()).getDriverClass();
					KeyValueStub.stubs.put(cData, stub);
					incDriverReference(stub);
					channel.accept(KeyValueSession.DRIVER, stub);
				} else {
					MosaicLogger.getLogger().trace(
							"KeyValueStub: use existing stub."); //$NON-NLS-1$
					incDriverReference(stub);
				}
			} catch (DriverNotFoundException e) {
				ExceptionTracer.traceDeferred(new ConnectionException(
						"The required key-value driver cannot be provided: " //$NON-NLS-1$
								+ e.getMessage(), e));
			}
		}
		return stub;
	}

	@Override
	public void destroy() {
		synchronized (AbstractDriverStub.lock) {
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
		BaseKeyValueDriver driver = super.getDriver(this.driverClass);
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
			BaseKeyValueDriver driver,
			Class<? extends KeyValueResponseTransmitter> transmitterClass)
			throws IOException, ClassNotFoundException {
		byte[] data;
		boolean unknownMessage = false;
		KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
		CompletionToken token = null;
		String key;

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
			// data = SerDesUtils.toObject(setRequest.getValue().toByteArray(),
			// Object.class);
			data = setRequest.getValue().toByteArray();

			MosaicLogger.getLogger().trace(
					"KeyValueStub - Received request for "
							+ kvMessage.toString() + " key: " + key
							+ " - request id: " + token.getMessageId()
							+ " client id: " + token.getClientId());

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
					"KeyValueStub - Received request for "
							+ kvMessage.toString() + " key: " + key
							+ " - request id: " + token.getMessageId()
							+ " client id: " + token.getClientId());

			IResult<byte[]> resultGet = driver.invokeGetOperation(
					token.getClientId(), key, getCallback);
			getCallback.setDetails(KeyValueOperations.GET, resultGet);
			break;
		case DELETE_REQUEST:
			KeyValuePayloads.DeleteRequest delRequest = (DeleteRequest) message.payload;
			token = delRequest.getToken();
			key = delRequest.getKey();

			MosaicLogger.getLogger().trace(
					"KeyValueStub - Received request for "
							+ kvMessage.toString() + " key: " + key
							+ " - request id: " + token.getMessageId()
							+ " client id: " + token.getClientId());

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
					"KeyValueStub - Received request for "
							+ kvMessage.toString() + " - request id: "
							+ token.getMessageId() + " client id: "
							+ token.getClientId());

			DriverOperationFinishedHandler listCallback = new DriverOperationFinishedHandler(
					token, session, driver.getClass(), transmitterClass);
			IResult<List<String>> resultList = driver.invokeListOperation(
					token.getClientId(), listCallback);
			listCallback.setDetails(KeyValueOperations.LIST, resultList);
			break;
		case ERROR:
			token = ((IdlCommon.Error) message.payload).getToken();
			unknownMessage = true;
			break;
		case OK:
			token = ((IdlCommon.Ok) message.payload).getToken();
			unknownMessage = true;
			break;
		case GET_REPLY:
			token = ((KeyValuePayloads.GetReply) message.payload).getToken();
			unknownMessage = true;
			break;
		case LIST_REPLY:
			token = ((KeyValuePayloads.ListReply) message.payload).getToken();
			unknownMessage = true;
			break;
		}

		if (unknownMessage) {
			handleUnknownMessage(session, driver, kvMessage.toString(), token,
					transmitterClass);
		}
	}

	protected void handleUnknownMessage(Session session,
			BaseKeyValueDriver driver, String messageType,
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
//		String bucket = ConfigUtils.resolveParameter(config,
//				ConfigProperties.getString("KVStoreDriver.3"), String.class, //$NON-NLS-1$
//				"");
		DriverConnectionData cData = null;
		if (user.equals("") && passwd.equals("")) {
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
		private CountDownLatch signal;
		private BaseKeyValueDriver driver;
		private KeyValueResponseTransmitter transmitter;
		private Session session;

		public DriverOperationFinishedHandler(CompletionToken complToken,
				Session session,
				Class<? extends BaseKeyValueDriver> driverClass,
				Class<? extends KeyValueResponseTransmitter> transmitterClass) {
			this.complToken = complToken;
			this.signal = new CountDownLatch(1);
			this.driver = KeyValueStub.this.getDriver(driverClass);
			this.transmitter = KeyValueStub.this
					.getResponseTransmitter(transmitterClass);
			this.session = session;
		}

		public void setDetails(KeyValueOperations op, IResult<?> result) {
			this.operation = op;
			this.result = result;
			this.signal.countDown();
		}

		@Override
		public void onSuccess(Object response) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				ExceptionTracer.traceDeferred(e);
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
				ExceptionTracer.traceDeferred(e);
			}

			this.driver.removePendingOperation(this.result);
			// result is error
			this.transmitter.sendResponse(this.session, this.complToken,
					this.operation, error.getMessage(), true);
		}
	}

}
