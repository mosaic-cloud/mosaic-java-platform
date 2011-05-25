package mosaic.driver.interop;

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
import mosaic.driver.IResourceDriver;
import mosaic.driver.kvstore.MemcachedDriver;
import mosaic.driver.kvstore.MemcachedOperations;
import mosaic.interop.idl.kvstore.CompletionToken;
import mosaic.interop.idl.kvstore.DeleteOperation;
import mosaic.interop.idl.kvstore.GetOperation;
import mosaic.interop.idl.kvstore.Operation;
import mosaic.interop.idl.kvstore.OperationNames;
import mosaic.interop.idl.kvstore.StoreOperation;

/**
 * Stub for the driver for key-value distributed storage systems implementing
 * the memcached protocol. This is used for communicating with a memcached
 * driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MemcachedStub extends AbstractDriverStub implements Runnable {
	private static final String DEFAULT_QUEUE_NAME = "memcached_requests";
	private static final String DEFAULT_EXCHANGE_NAME = "memcached";

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
			ResponseTransmitter transmitter, IResourceDriver driver) {
		super(config, DEFAULT_EXCHANGE_NAME, DEFAULT_QUEUE_NAME, transmitter,
				driver);

	}

	/**
	 * Returns a stub for the Memcached driver.
	 * 
	 * @param config
	 *            the configuration data for the stub and driver
	 * @return the Memcached driver stub
	 */
	public static MemcachedStub create(IConfiguration config) {
		MemcachedStub stub = null;
		try {
			MemcachedResponseTransmitter transmitter = new MemcachedResponseTransmitter(
					config);
			MemcachedDriver driver = MemcachedDriver.create(config);
			stub = new MemcachedStub(config, transmitter, driver);
		} catch (IOException e) {
			ExceptionTracer.traceRethrown(new ConnectionException(
					"The Memcached proxy cannot connect to the driver: "
							+ e.getMessage(), e));
		}
		return stub;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mosaic.driver.interop.AbstractDriverStub#startOperation(byte[])
	 */
	protected void startOperation(byte[] message) throws IOException,
			ClassNotFoundException {
		CompletionToken token;
		OperationNames opName;
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
					"Received request for " + opName.toString()
							+ " - request id: " + token.get(0) + " client id: "
							+ token.get(1));

			// execute operation
			IResult<Boolean> resultStore = null;
			DriverOperationFinishedHandler storeCallback = new DriverOperationFinishedHandler(
					token);
			MemcachedOperations operation = MemcachedOperations.ADD;
			switch (opName) {
			case ADD:
				resultStore = driver.invokeAddOperation(key, exp, data,
						storeCallback);
				break;
			case APPEND:
				resultStore = driver.invokeAppendOperation(key, data,
						storeCallback);
				operation = MemcachedOperations.APPEND;
				break;
			case CAS:
				resultStore = driver.invokeCASOperation(key, data,
						storeCallback);
				operation = MemcachedOperations.CAS;
				break;
			case PREPEND:
				resultStore = driver.invokePrependOperation(key, data,
						storeCallback);
				operation = MemcachedOperations.PREPEND;
				break;
			case REPLACE:
				resultStore = driver.invokeReplaceOperation(key, exp, data,
						storeCallback);
				operation = MemcachedOperations.REPLACE;
				break;
			case SET:
				resultStore = driver.invokeSetOperation(key, exp, data,
						storeCallback);
				operation = MemcachedOperations.SET;
				break;
			default:
				MosaicLogger.getLogger()
						.error("Unknown memcached store message: "
								+ opName.toString());
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

			if (opName.equals(OperationNames.DELETE)) {
				DriverOperationFinishedHandler delCallback = new DriverOperationFinishedHandler(
						token);
				IResult<Boolean> resultDelete = driver.invokeDeleteOperation(
						key, delCallback);
				delCallback
						.setDetails(MemcachedOperations.DELETE, resultDelete);

			} else {
				MosaicLogger.getLogger().error(
						"Unknown memcached delete message: "
								+ opName.toString());
			}
		} else if (ob instanceof GetOperation) {
			GetOperation gop = (GetOperation) ob;
			token = (CompletionToken) gop.get(0);
			opName = (OperationNames) gop.get(1);
			@SuppressWarnings("unchecked")
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
				getCallback.setDetails(MemcachedOperations.GET, resultGet);
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
				getBCallback.setDetails(MemcachedOperations.GET_BULK,
						resultGetBulk);
				break;
			default:
				MosaicLogger.getLogger().error(
						"Unknown memcached get message: " + opName.toString());
				break;
			}
		}
	}

	/**
	 * Handler for processing responses of the requests submitted to the stub.
	 * This will basically call the transmitter associated with the stub.
	 * 
	 * @author Georgiana Macariu
	 * 
	 */
	final class DriverOperationFinishedHandler implements
			IOperationCompletionHandler {
		private IResult<?> result;
		private MemcachedOperations operation;
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

		public void setDetails(MemcachedOperations op, IResult<?> result) {
			this.operation = op;
			this.result = result;
			this.signal.countDown();
		}

		@Override
		public void onSuccess(Object response) {
			try {
				this.signal.await();
			} catch (InterruptedException e) {
				ExceptionTracer.traceRethrown(e);
			}
			this.driver.removePendingOperation(result);

			if (operation.equals(MemcachedOperations.GET)) {
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
				ExceptionTracer.traceRethrown(e);
			}
			this.driver.removePendingOperation(result);
			// result is error
			transmitter.sendResponse(complToken, operation, error.getMessage(),
					true);
		}
	}

}
