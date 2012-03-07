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

package eu.mosaic_cloud.drivers.interop.kvstore; // NOPMD

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import eu.mosaic_cloud.drivers.ConfigProperties;
import eu.mosaic_cloud.drivers.DriverNotFoundException;
import eu.mosaic_cloud.drivers.interop.AbstractDriverStub;
import eu.mosaic_cloud.drivers.interop.DriverConnectionData;
import eu.mosaic_cloud.drivers.kvstore.AbstractKeyValueDriver;
import eu.mosaic_cloud.drivers.kvstore.KeyValueDriverFactory;
import eu.mosaic_cloud.drivers.kvstore.KeyValueOperations;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.ConfigUtils;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ConnectionException;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.platform.core.ops.IOperationCompletionHandler;
import eu.mosaic_cloud.platform.core.ops.IResult;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.AbortRequest;
import eu.mosaic_cloud.platform.interop.idl.IdlCommon.CompletionToken;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.DeleteRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.GetRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.InitRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.ListRequest;
import eu.mosaic_cloud.platform.interop.idl.kvstore.KeyValuePayloads.SetRequest;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueMessage;
import eu.mosaic_cloud.platform.interop.specs.kvstore.KeyValueSession;
import eu.mosaic_cloud.tools.threading.core.ThreadingContext;

import com.google.common.base.Preconditions;

/**
 * Stub for the driver for key-value distributed storage systems. This is used
 * for communicating with a key-value driver.
 * 
 * @author Georgiana Macariu
 * 
 */
public class KeyValueStub extends AbstractDriverStub { // NOPMD

    /**
     * Handler for processing responses of the requests submitted to the stub.
     * This will basically call the transmitter associated with the stub.
     * 
     * @author Georgiana Macariu
     * 
     */
    @SuppressWarnings("rawtypes")
    protected class DriverOperationFinishedHandler implements IOperationCompletionHandler {

        private IResult<?> result;

        private KeyValueOperations operation;

        private final CompletionToken complToken;

        private final CountDownLatch signal;

        private final AbstractKeyValueDriver driver;

        private final KeyValueResponseTransmitter transmitter;

        private final Session session;

        public DriverOperationFinishedHandler(CompletionToken complToken, Session session,
                Class<? extends AbstractKeyValueDriver> driverClass,
                Class<? extends KeyValueResponseTransmitter> transmitterClass) {
            this.complToken = complToken;
            this.signal = new CountDownLatch(1);
            this.driver = KeyValueStub.this.getDriver(driverClass);
            this.transmitter = KeyValueStub.this.getResponseTransmitter(transmitterClass);
            this.session = session;
        }

        @Override
        public void onFailure(Throwable error) {
            try {
                this.signal.await();
            } catch (final InterruptedException e) {
                ExceptionTracer.traceIgnored(e);
            }
            this.driver.removePendingOperation(this.result);
            // NOTE: result is error
            this.transmitter.sendResponse(this.session, this.complToken, this.operation,
                    error.getMessage(), true);
        }

        @Override
        public void onSuccess(Object response) {
            try {
                this.signal.await();
            } catch (final InterruptedException e) {
                ExceptionTracer.traceIgnored(e);
            }
            this.driver.removePendingOperation(this.result);
            if (this.operation.equals(KeyValueOperations.GET)) {
                final Map<String, Object> resMap = new HashMap<String, Object>();
                resMap.put("dummy", response); //$NON-NLS-1$
                this.transmitter.sendResponse(this.session, this.complToken, this.operation,
                        resMap, false);
            } else {
                this.transmitter.sendResponse(this.session, this.complToken, this.operation,
                        response, false);
            }
        }

        public void setDetails(KeyValueOperations operation, IResult<?> result) {
            this.operation = operation;
            this.result = result;
            this.signal.countDown();
        }
    }

    private static Map<DriverConnectionData, KeyValueStub> stubs = new HashMap<DriverConnectionData, KeyValueStub>();

    /**
     * Returns a stub for the key-value store driver.
     * 
     * @param config
     *            the configuration data for the stub and driver
     * @param the
     *            context for creating threads
     * @param channel
     *            the channel used by the driver for receiving requests
     * @return the driver stub
     */
    public static KeyValueStub create(IConfiguration config, ThreadingContext threadingContext,
            ZeroMqChannel channel) {
        final DriverConnectionData cData = KeyValueStub.readConnectionData(config);
        final MosaicLogger sLogger = MosaicLogger.createLogger(KeyValueStub.class);
        KeyValueStub stub;
        synchronized (AbstractDriverStub.MONITOR) {
            stub = KeyValueStub.stubs.get(cData);
            try {
                if (stub == null) {
                    sLogger.trace("KeyValueStub: create new stub."); //$NON-NLS-1$
                    final KeyValueResponseTransmitter transmitter = new KeyValueResponseTransmitter();
                    final String driverName = ConfigUtils.resolveParameter(config,
                            ConfigProperties.getString("KVStoreDriver.6"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
                    final AbstractKeyValueDriver driver = KeyValueDriverFactory.createDriver(
                            driverName, config, threadingContext);
                    stub = new KeyValueStub(config, transmitter, driver, channel);
                    stub.driverClass = KeyValueDriverFactory.DriverType.valueOf(
                            driverName.toUpperCase(Locale.ENGLISH)).getDriverClass();
                    KeyValueStub.stubs.put(cData, stub);
                    incDriverReference(stub);
                    channel.accept(KeyValueSession.DRIVER, stub);
                } else {
                    sLogger.trace("KeyValueStub: use existing stub."); //$NON-NLS-1$
                    incDriverReference(stub);
                }
            } catch (final DriverNotFoundException e) {
                ExceptionTracer.traceDeferred(e);
                final ConnectionException e1 = new ConnectionException(
                        "The required key-value driver cannot be provided: " + e.getMessage(), e);
                ExceptionTracer.traceIgnored(e1);
            }
        }
        return stub;
    }

    public static KeyValueStub createDetached(IConfiguration config,
            ThreadingContext threadingContext, ZeroMqChannel channel) {
        final MosaicLogger sLogger = MosaicLogger.createLogger(KeyValueStub.class);
        KeyValueStub stub;
        try {
            sLogger.trace("KeyValueStub: create new stub."); //$NON-NLS-1$
            final KeyValueResponseTransmitter transmitter = new KeyValueResponseTransmitter();
            final String driverName = ConfigUtils.resolveParameter(config,
                    ConfigProperties.getString("KVStoreDriver.6"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
            final AbstractKeyValueDriver driver = KeyValueDriverFactory.createDriver(driverName,
                    config, threadingContext);
            stub = new KeyValueStub(config, transmitter, driver, channel);
            stub.driverClass = KeyValueDriverFactory.DriverType.valueOf(
                    driverName.toUpperCase(Locale.ENGLISH)).getDriverClass();
            channel.accept(KeyValueSession.DRIVER, stub);
        } catch (final DriverNotFoundException e) {
            ExceptionTracer.traceDeferred(e);
            final ConnectionException e1 = new ConnectionException(
                    "The required key-value driver cannot be provided: " + e.getMessage(), e);
            ExceptionTracer.traceIgnored(e1);
            stub = null;
        }
        return stub;
    }

    /**
     * Reads resource connection data from the configuration data.
     * 
     * @param config
     *            the configuration data
     * @return resource connection data
     */
    protected static DriverConnectionData readConnectionData(IConfiguration config) {
        final String resourceHost = ConfigUtils.resolveParameter(config,
                ConfigProperties.getString("KVStoreDriver.0"), String.class, //$NON-NLS-1$
                "localhost"); //$NON-NLS-1$
        final int resourcePort = ConfigUtils.resolveParameter(config,
                ConfigProperties.getString("KVStoreDriver.1"), Integer.class, //$NON-NLS-1$
                0);
        final String driver = ConfigUtils.resolveParameter(config,
                ConfigProperties.getString("KVStoreDriver.6"), String.class, ""); //$NON-NLS-1$ //$NON-NLS-2$
        final String user = ConfigUtils.resolveParameter(config,
                ConfigProperties.getString("KVStoreDriver.5"), String.class, //$NON-NLS-1$
                ""); //$NON-NLS-1$
        final String passwd = ConfigUtils.resolveParameter(config,
                ConfigProperties.getString("KVStoreDriver.4"), String.class, //$NON-NLS-1$
                ""); //$NON-NLS-1$
        DriverConnectionData cData;
        if ("".equals(user) && "".equals(passwd)) {
            cData = new DriverConnectionData(resourceHost, resourcePort, driver);
        } else {
            cData = new DriverConnectionData(resourceHost, resourcePort, driver, user, passwd);
        }
        return cData;
    }

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
    public KeyValueStub(IConfiguration config, KeyValueResponseTransmitter transmitter,
            AbstractKeyValueDriver driver, ZeroMqChannel commChannel) {
        super(config, transmitter, driver, commChannel);
    }

    @Override
    public synchronized void destroy() {
        synchronized (AbstractDriverStub.MONITOR) {
            final int ref = decDriverReference(this);
            if ((ref == 0)) {
                final DriverConnectionData cData = KeyValueStub
                        .readConnectionData(this.configuration);
                KeyValueStub.stubs.remove(cData);
            }
        }
        super.destroy();
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
            Class<? extends KeyValueResponseTransmitter> transmitterClass) throws IOException,
            ClassNotFoundException {
        byte[] data;
        boolean unknownMessage = false; // NOPMD by georgiana on 9/30/11 2:36 PM
        final KeyValueMessage kvMessage = (KeyValueMessage) message.specification;
        CompletionToken token = null; // NOPMD by georgiana on 9/30/11 2:37 PM
        String key;
        final String messagePrefix = "KeyValueStub - Received request for "; // NOPMD
        // by
        // georgiana
        // on
        // 10/12/11
        // 2:11
        // PM
        switch (kvMessage) {
        case ACCESS:
            this.logger.trace("Received initiation message");
            final KeyValuePayloads.InitRequest initRequest = (InitRequest) message.payload;
            token = initRequest.getToken();
            final String bucket = initRequest.getBucket();
            driver.registerClient(token.getClientId(), bucket);
            break;
        case ABORTED:
            this.logger.trace("Received termination message");
            final IdlCommon.AbortRequest abortRequest = (AbortRequest) message.payload;
            token = abortRequest.getToken();
            driver.unregisterClient(token.getClientId());
            break;
        case SET_REQUEST:
            final KeyValuePayloads.SetRequest setRequest = (SetRequest) message.payload;
            token = setRequest.getToken();
            key = setRequest.getKey();
            data = setRequest.getValue().toByteArray();
            this.logger.trace(messagePrefix + kvMessage.toString() + " key: " + key
                    + " - request id: " + token.getMessageId() + " client id: "
                    + token.getClientId());
            // NOTE: execute operation
            final DriverOperationFinishedHandler setCallback = new DriverOperationFinishedHandler(
                    token, session, driver.getClass(), transmitterClass);
            final IResult<Boolean> resultSet = driver.invokeSetOperation(token.getClientId(), key,
                    data, setCallback);
            setCallback.setDetails(KeyValueOperations.SET, resultSet);
            break;
        case GET_REQUEST:
            final KeyValuePayloads.GetRequest getRequest = (GetRequest) message.payload;
            token = getRequest.getToken();
            final DriverOperationFinishedHandler getCallback = new DriverOperationFinishedHandler(
                    token, session, driver.getClass(), transmitterClass);
            if (getRequest.getKeyCount() != 1) {
                // NOTE: error - the simple driver can handle only single-key
                // get
                this.logger.error("Basic driver can handle only single-key GET.");
                driver.handleUnsupportedOperationError(kvMessage.toString(), getCallback);
                break;
            }
            key = getRequest.getKey(0);
            this.logger.trace(messagePrefix + kvMessage.toString() + " key: " + key
                    + " - request id: " + token.getMessageId() + " client id: "
                    + token.getClientId());
            final IResult<byte[]> resultGet = driver.invokeGetOperation(token.getClientId(), key,
                    getCallback);
            getCallback.setDetails(KeyValueOperations.GET, resultGet);
            break;
        case DELETE_REQUEST:
            final KeyValuePayloads.DeleteRequest delRequest = (DeleteRequest) message.payload;
            token = delRequest.getToken();
            key = delRequest.getKey();
            this.logger.trace(messagePrefix + kvMessage.toString() + " key: " + key
                    + " - request id: " + token.getMessageId() + " client id: "
                    + token.getClientId());
            final DriverOperationFinishedHandler delCallback = new DriverOperationFinishedHandler(
                    token, session, driver.getClass(), transmitterClass);
            final IResult<Boolean> resultDelete = driver.invokeDeleteOperation(token.getClientId(),
                    key, delCallback);
            delCallback.setDetails(KeyValueOperations.DELETE, resultDelete);
            break;
        case LIST_REQUEST:
            final KeyValuePayloads.ListRequest listRequest = (ListRequest) message.payload;
            token = listRequest.getToken();
            this.logger.trace(messagePrefix + kvMessage.toString() + " - request id: "
                    + token.getMessageId() + " client id: " + token.getClientId());
            final DriverOperationFinishedHandler listCallback = new DriverOperationFinishedHandler(
                    token, session, driver.getClass(), transmitterClass);
            final IResult<List<String>> resultList = driver.invokeListOperation(
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
            handleUnknownMessage(session, driver, kvMessage.toString(), token, transmitterClass);
        }
    }

    protected void handleUnknownMessage(Session session, AbstractKeyValueDriver driver,
            String messageType, CompletionToken token,
            Class<? extends KeyValueResponseTransmitter> transmitterClass) {
        this.logger.error("Unexpected message type: " + messageType + " - request id: "
                + token.getMessageId() + " client id: " + token.getClientId());
        // NOTE: create callback
        final DriverOperationFinishedHandler failCallback = new DriverOperationFinishedHandler(
                token, session, driver.getClass(), transmitterClass);
        driver.handleUnsupportedOperationError(messageType, failCallback);
    }

    @Override
    protected void startOperation(Message message, Session session) throws IOException,
            ClassNotFoundException {
        Preconditions.checkArgument(message.specification instanceof KeyValueMessage);
        final AbstractKeyValueDriver driver = super.getDriver(this.driverClass);
        handleKVOperation(message, session, driver, KeyValueResponseTransmitter.class);
    }
}
