package mosaic.driver.interop.kvstore;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.ConfigProperties;
import mosaic.driver.DriverNotFoundException;
import mosaic.driver.IResourceDriver;
import mosaic.driver.interop.AbstractDriverStub;
import mosaic.driver.interop.DriverConnectionData;
import mosaic.driver.interop.ResponseTransmitter;
import mosaic.driver.kvstore.BaseKeyValueDriver;
import mosaic.driver.kvstore.KeyValueDriverFactory;
import mosaic.driver.kvstore.KeyValueOperations;
import mosaic.driver.kvstore.memcached.MemcachedDriver;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.DeleteOperation;
import mosaic.interop.idl.kvstore.GetOperation;
import mosaic.interop.idl.kvstore.ListOperation;
import mosaic.interop.idl.kvstore.Operation;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.SetOperation;
import mosaic.interop.idl.kvstore.StoreOperation;

/**
 * Stub for the driver for key-value distributed storage systems. This is used
 * for communicating with a key-value driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueStub extends AbstractDriverStub implements Runnable {
	private static final String DEFAULT_QUEUE_NAME = "kvstore_requests"; //$NON-NLS-1$
	private static final String DEFAULT_EXCHANGE_NAME = "kvstore"; //$NON-NLS-1$
	private static Map<DriverConnectionData, KeyValueStub> stubs = new HashMap<DriverConnectionData, KeyValueStub>();

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
	 */
	public KeyValueStub(IConfiguration config, ResponseTransmitter transmitter,
			IResourceDriver driver) {
		super(config, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME, transmitter,
				driver);

	}

	/**
	 * Returns a stub for the key-value store driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @return the driver stub
	 */
	public static KeyValueStub create(IConfiguration config) {
		DriverConnectionData cData = KeyValueStub.readConnectionData(config);
		KeyValueStub stub = null;
		synchronized (AbstractDriverStub.lock) {
			stub = stubs.get(cData);
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
					stub = new KeyValueStub(config, transmitter, driver);
					stubs.put(cData, stub);
					// FIXME this will be removed - the driver will be started
					// from somewhere else
					Thread driverThread = new Thread(stub);
					driverThread.start();
				} else
					MosaicLogger.getLogger().trace(
							"MemcachedStub: use existing stub."); //$NON-NLS-1$
			} catch (DriverNotFoundException e) {
				ExceptionTracer.traceDeferred(new ConnectionException(
						"The required key-value driver cannot be provided: " //$NON-NLS-1$
								+ e.getMessage(), e));
			}
		}
		return stub;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.driver.interop.AbstractDriverStub#startOperation(byte[])
	 */
	@SuppressWarnings("unchecked")
	protected void startOperation(byte[] message) throws IOException,
			ClassNotFoundException {
		CompletionToken token;
		OperationNames opName;
		String key;

		Operation op = new Operation();
		op = SerDesUtils.deserializeWithSchema(message, op);
		Object ob = op.get(0);

		MemcachedDriver driver = super.getDriver(MemcachedDriver.class);

		if (ob instanceof SetOperation) {
			StoreOperation sop = (StoreOperation) ob;
			token = (CompletionToken) sop.get(0);
			opName = (OperationNames) sop.get(1);
			key = ((CharSequence) sop.get(2)).toString();
			ByteBuffer dataBytes = (ByteBuffer) sop.get(3);
			Object data = SerDesUtils.toObject(dataBytes.array());

			MosaicLogger.getLogger().trace(
					"KeyValueStub - Received request for " + opName.toString() //$NON-NLS-1$
							+ " - request id: " + token.get(0) + " client id: " //$NON-NLS-1$ //$NON-NLS-2$
							+ token.get(1));

			// execute operation
			IResult<Boolean> resultStore = null;
			DriverOperationFinishedHandler storeCallback = new DriverOperationFinishedHandler(
					token);
			if (opName.equals(OperationNames.SET)) {
				resultStore = driver.invokeSetOperation(key, data,
						storeCallback);
			} else {
				MosaicLogger.getLogger().error(
						"Unknown store message: " + opName.toString()); //$NON-NLS-1$
				driver.handleUnsupportedOperationError(opName.toString(),
						storeCallback);
			}
			storeCallback.setDetails(KeyValueOperations.SET, resultStore);
		} else if (ob instanceof DeleteOperation) {
			DeleteOperation dop = (DeleteOperation) ob;
			token = (CompletionToken) dop.get(0);
			opName = (OperationNames) dop.get(1);
			key = ((CharSequence) dop.get(2)).toString();
			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString() + " - id: " //$NON-NLS-1$ //$NON-NLS-2$
							+ token.get(0) + " key: " + key); //$NON-NLS-1$

			DriverOperationFinishedHandler delCallback = new DriverOperationFinishedHandler(
					token);
			if (opName.equals(OperationNames.DELETE)) {
				IResult<Boolean> resultDelete = driver.invokeDeleteOperation(
						key, delCallback);
				delCallback.setDetails(KeyValueOperations.DELETE, resultDelete);

			} else {
				driver.handleUnsupportedOperationError(opName.toString(),
						delCallback);
				MosaicLogger.getLogger().error(
						"Unknown delete message: " + opName.toString()); //$NON-NLS-1$
			}
		} else if (ob instanceof GetOperation) {
			GetOperation gop = (GetOperation) ob;
			token = (CompletionToken) gop.get(0);
			opName = (OperationNames) gop.get(1);
			List<CharSequence> keys = (List<CharSequence>) gop.get(2);

			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString() + " - id: " //$NON-NLS-1$ //$NON-NLS-2$
							+ token.get(0));

			if (opName.equals(OperationNames.GET)) {
				DriverOperationFinishedHandler getCallback = new DriverOperationFinishedHandler(
						token);
				IResult<Object> resultGet = driver.invokeGetOperation(
						keys.get(0).toString(), getCallback);
				getCallback.setDetails(KeyValueOperations.GET, resultGet);
			} else {
				DriverOperationFinishedHandler callback = new DriverOperationFinishedHandler(
						token);
				driver.handleUnsupportedOperationError(opName.toString(),
						callback);
				MosaicLogger.getLogger().error(
						"Unknown get message: " + opName.toString()); //$NON-NLS-1$
			}
		} else if (ob instanceof ListOperation) {
			ListOperation lop = (ListOperation) ob;
			token = (CompletionToken) lop.get(0);
			opName = (OperationNames) lop.get(1);

			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString() + " - id: " //$NON-NLS-1$ //$NON-NLS-2$
							+ token.get(0));
			DriverOperationFinishedHandler listCallback = new DriverOperationFinishedHandler(
					token);
			if (opName.equals(OperationNames.LIST)) {
				IResult<List<String>> resultList = driver
						.invokeListOperation(listCallback);
				listCallback.setDetails(KeyValueOperations.LIST, resultList);

			} else {
				driver.handleUnsupportedOperationError(opName.toString(),
						listCallback);
				MosaicLogger.getLogger().error(
						"Unknown list message: " + opName.toString()); //$NON-NLS-1$
			}
		}
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
		DriverConnectionData cData = null;
		if (user.equals("") && passwd.equals("")) //$NON-NLS-1$ //$NON-NLS-2$
			cData = new DriverConnectionData(resourceHost, resourcePort, driver);
		else
			cData = new DriverConnectionData(resourceHost, resourcePort,
					driver, user, passwd);
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
	final class DriverOperationFinishedHandler implements
			IOperationCompletionHandler {
		private IResult<?> result;
		private KeyValueOperations operation;
		private final CompletionToken complToken;
		private CountDownLatch signal;
		private BaseKeyValueDriver driver;
		private KeyValueResponseTransmitter transmitter;

		public DriverOperationFinishedHandler(CompletionToken complToken) {
			this.complToken = complToken;
			this.signal = new CountDownLatch(1);
			this.driver = KeyValueStub.this.getDriver(MemcachedDriver.class);
			this.transmitter = KeyValueStub.this
					.getResponseTransmitter(KeyValueResponseTransmitter.class);
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
			this.driver.removePendingOperation(result);

			if (operation.equals(KeyValueOperations.GET)) {
				Map<String, Object> resMap = new HashMap<String, Object>();
				resMap.put("dummy", response); //$NON-NLS-1$
				transmitter.sendResponse(complToken, operation, resMap, false);
			} else {
				transmitter
						.sendResponse(complToken, operation, response, false);
			}

		}

		@Override
		public void onFailure(Throwable error) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				ExceptionTracer.traceDeferred(e);
			}
			this.driver.removePendingOperation(result);
			// result is error
			transmitter.sendResponse(complToken, operation, error.getMessage(),
					true);
		}
	}

}
