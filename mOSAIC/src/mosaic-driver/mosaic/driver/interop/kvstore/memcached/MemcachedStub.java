package mosaic.driver.interop.kvstore.memcached;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ConnectionException;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.core.utils.SerDesUtils;
import mosaic.driver.interop.AbstractDriverStub;
import mosaic.driver.interop.DriverConnectionData;
import mosaic.driver.interop.kvstore.KeyValueResponseTransmitter;
import mosaic.driver.interop.kvstore.KeyValueStub;
import mosaic.driver.kvstore.BaseKeyValueDriver;
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
 * Stub for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used for communicating with a memcached
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedStub extends KeyValueStub implements Runnable {
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
	 */
	public MemcachedStub(IConfiguration config,
			KeyValueResponseTransmitter transmitter, BaseKeyValueDriver driver) {
		super(config, transmitter, driver);

	}

	/**
	 * Returns a stub for the Memcached driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @return the Memcached driver stub
	 */
	public static MemcachedStub create(IConfiguration config) {
		DriverConnectionData cData = KeyValueStub.readConnectionData(config);
		MemcachedStub stub = null;
		synchronized (AbstractDriverStub.lock) {
			stub = stubs.get(cData);
			try {
				if (stub == null) {
					MosaicLogger.getLogger().trace(
							"MemcachedStub: create new stub.");

					MemcachedResponseTransmitter transmitter = new MemcachedResponseTransmitter(
							config);
					MemcachedDriver driver = MemcachedDriver.create(config);
					stub = new MemcachedStub(config, transmitter, driver);
					stubs.put(cData, stub);
					// FIXME this will be removed - the driver will be started
					// from somewhere else
					Thread driverThread = new Thread(stub);
					driverThread.start();
				} else
					MosaicLogger.getLogger().trace(
							"MemcachedStub: use existing stub.");
			} catch (IOException e) {
				ExceptionTracer.traceDeferred(new ConnectionException(
						"The Memcached proxy cannot connect to the driver: "
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
		CompletionToken token = null;
		OperationNames opName = null;
		String key;

		Operation op = new Operation();
		op = SerDesUtils.deserializeWithSchema(message, op);
		Object ob = op.get(0);

		MemcachedDriver driver = super.getDriver(MemcachedDriver.class);

		if (ob instanceof StoreOperation) {
			StoreOperation sop = (StoreOperation) ob;
			token = (CompletionToken) sop.get(0);
			opName = (OperationNames) sop.get(1);
			key = ((CharSequence) sop.get(2)).toString();
			int exp = (Integer) sop.get(3);
			ByteBuffer dataBytes = (ByteBuffer) sop.get(4);
			Object data = SerDesUtils.toObject(dataBytes.array());

			MosaicLogger.getLogger().trace(
					"MemcachedStub - Received request for " + opName.toString()
							+ " - request id: " + token.get(0) + " client id: "
							+ token.get(1));

			// execute operation
			IResult<Boolean> resultStore = null;
			DriverOperationFinishedHandler storeCallback = new DriverOperationFinishedHandler(
					token);
			KeyValueOperations operation = KeyValueOperations.ADD;
			switch (opName) {
			case ADD:
				resultStore = driver.invokeAddOperation(key, exp, data,
						storeCallback);
				break;
			case APPEND:
				resultStore = driver.invokeAppendOperation(key, data,
						storeCallback);
				operation = KeyValueOperations.APPEND;
				break;
			case CAS:
				resultStore = driver.invokeCASOperation(key, data,
						storeCallback);
				operation = KeyValueOperations.CAS;
				break;
			case PREPEND:
				resultStore = driver.invokePrependOperation(key, data,
						storeCallback);
				operation = KeyValueOperations.PREPEND;
				break;
			case REPLACE:
				resultStore = driver.invokeReplaceOperation(key, exp, data,
						storeCallback);
				operation = KeyValueOperations.REPLACE;
				break;
			case SET:
				resultStore = driver.invokeSetOperation(key, exp, data,
						storeCallback);
				operation = KeyValueOperations.SET;
				break;
			default:
				MosaicLogger.getLogger()
						.error("Unknown memcached store message: "
								+ opName.toString());
				driver.handleUnsupportedOperationError(opName.toString(),
						storeCallback);
				break;
			}
			storeCallback.setDetails(operation, resultStore);
		} else if (ob instanceof DeleteOperation) {
			DeleteOperation dop = (DeleteOperation) ob;
			token = (CompletionToken) dop.get(0);
			opName = (OperationNames) dop.get(1);
			key = ((CharSequence) dop.get(2)).toString();
			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString() + " - id: "
							+ token.get(0) + " key: " + key);

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
						"Unknown memcached delete message: "
								+ opName.toString());
			}
		} else if (ob instanceof GetOperation) {
			GetOperation gop = (GetOperation) ob;
			token = (CompletionToken) gop.get(0);
			opName = (OperationNames) gop.get(1);
			List<CharSequence> keys = (List<CharSequence>) gop.get(2);

			MosaicLogger.getLogger().trace(
					"Received request for " + opName.toString() + " - id: "
							+ token.get(0));

			switch (opName) {
			case GET:
				DriverOperationFinishedHandler getCallback = new DriverOperationFinishedHandler(
						token);
				IResult<Object> resultGet = driver.invokeGetOperation(
						keys.get(0).toString(), getCallback);
				getCallback.setDetails(KeyValueOperations.GET, resultGet);
				break;
			case GET_BULK:
				List<String> strKeys = new ArrayList<String>();
				for (CharSequence kcs : keys) {
					strKeys.add(kcs.toString());
				}
				DriverOperationFinishedHandler getBCallback = new DriverOperationFinishedHandler(
						token);
				IResult<Map<String, Object>> resultGetBulk = driver
						.invokeGetBulkOperation(strKeys, getBCallback);
				getBCallback.setDetails(KeyValueOperations.GET_BULK,
						resultGetBulk);
				break;
			default:
				DriverOperationFinishedHandler callback = new DriverOperationFinishedHandler(
						token);
				driver.handleUnsupportedOperationError(opName.toString(),
						callback);
				MosaicLogger.getLogger().error(
						"Unknown memcached get message: " + opName.toString());
				break;
			}
		} else {
			if (ob instanceof SetOperation) {
				SetOperation opp = (SetOperation) ob;
				token = (CompletionToken) opp.get(0);
				opName = (OperationNames) opp.get(1);
			} else if (ob instanceof ListOperation) {
				ListOperation opp = (ListOperation) ob;
				token = (CompletionToken) opp.get(0);
				opName = (OperationNames) opp.get(1);
			}

			DriverOperationFinishedHandler callback = new DriverOperationFinishedHandler(
					token);
			driver.handleUnsupportedOperationError(opName.toString(), callback);
			MosaicLogger.getLogger().error(
					"Unknown memcached get message: " + opName.toString());
		}
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
		private MemcachedDriver driver;
		private MemcachedResponseTransmitter transmitter;

		public DriverOperationFinishedHandler(CompletionToken complToken) {
			this.complToken = complToken;
			this.signal = new CountDownLatch(1);
			this.driver = MemcachedStub.this.getDriver(MemcachedDriver.class);
			this.transmitter = MemcachedStub.this
					.getResponseTransmitter(MemcachedResponseTransmitter.class);
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
				resMap.put("dummy", response);
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
