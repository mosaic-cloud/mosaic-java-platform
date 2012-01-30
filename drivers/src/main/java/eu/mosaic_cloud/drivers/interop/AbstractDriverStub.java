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
package eu.mosaic_cloud.drivers.interop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.drivers.IResourceDriver;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.platform.core.exceptions.ExceptionTracer;
import eu.mosaic_cloud.platform.core.log.MosaicLogger;
import eu.mosaic_cloud.tools.callbacks.core.CallbackReference;

/**
 * Base class for driver stubs.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractDriverStub implements SessionCallbacks {

	protected IConfiguration configuration;
	protected MosaicLogger logger;
	private final ResponseTransmitter transmitter;
	private final IResourceDriver driver;
	private final List<Session> sessions;
	private final ZeroMqChannel commChannel;

	protected static final Object LOCK = new Object();
	private static Map<AbstractDriverStub, Integer> references = new IdentityHashMap<AbstractDriverStub, Integer>();

	/**
	 * Builds a driver stub.
	 * 
	 * @param config
	 *            configuration data for the driver and its stub
	 * @param transmitter
	 *            the transmitter which will serialize and send responses back
	 *            to the connector
	 * @param driver
	 *            the driver which will handle requests received by the stub
	 * @param commChannel
	 *            the channel for communicating with connectors
	 */
	protected AbstractDriverStub(IConfiguration config,
			ResponseTransmitter transmitter, IResourceDriver driver,
			ZeroMqChannel commChannel) {
		super();
		this.configuration = config;
		this.logger = MosaicLogger.createLogger(this);
		this.sessions = new ArrayList<Session>();
		this.commChannel = commChannel;

		this.transmitter = transmitter;
		this.driver = driver;
	}

	/**
	 * Destroys this stub.
	 * 
	 */
	public void destroy() {
		synchronized (this) {
			this.driver.destroy();
			this.transmitter.destroy();
			this.commChannel.terminate(500);
			this.logger.trace("DriverStub destroyed.");
		}
	}

	protected static void incDriverReference(AbstractDriverStub stub) {
		synchronized (AbstractDriverStub.class) {
			Integer ref = AbstractDriverStub.references.get(stub);
			if (ref == null) {
				ref = 0; // NOPMD by georgiana on 10/12/11 3:14 PM
			}
			ref++;
			AbstractDriverStub.references.put(stub, ref);
		}
	}

	protected static int decDriverReference(AbstractDriverStub stub) {
		synchronized (AbstractDriverStub.class) {
			Integer ref = AbstractDriverStub.references.get(stub);
			if (ref == null) {
				ref = 0; // NOPMD by georgiana on 10/12/11 3:15 PM
			}
			ref--;
			if (ref == 0) {
				AbstractDriverStub.references.remove(stub);
			}
			return ref;
		}
	}

	@Override
	public CallbackReference received(Session session, Message message) {
		try {
			startOperation(message, session);
		} catch (IOException e) {
			ExceptionTracer.traceIgnored(e);
		} catch (ClassNotFoundException e) {
			ExceptionTracer.traceIgnored(e);
		}
		return null;
	}

	@Override
	public CallbackReference created(Session session) {
		// TODO handle session created
		if (!this.sessions.contains(session)) {
			this.sessions.add(session);
		}
		return null;
	}

	@Override
	public CallbackReference destroyed(Session session) {
		// handle session destroyed
		this.logger.trace("Session destroyed.");
		this.sessions.remove(session);
		return null;
	}

	@Override
	public CallbackReference failed(Session session, Throwable exception) {
		this.logger.error("Session failed");
		return null;
	}

	/**
	 * Returns the response transmitter used by the stub.
	 * 
	 * @param <T>
	 *            the type of the transmitter
	 * @param transClass
	 *            the class object of the transmitter
	 * @return the transmitter
	 */
	protected <T extends ResponseTransmitter> T getResponseTransmitter(
			Class<T> transClass) {
		return transClass.cast(this.transmitter);
	}

	/**
	 * Returns the driver used by the stub.
	 * 
	 * @param <T>
	 *            the type of the driver
	 * @param driverClass
	 *            the class object of the driver
	 * @return the driver
	 */
	public <T extends IResourceDriver> T getDriver(Class<T> driverClass) {
		return driverClass.cast(this.driver);
	}

	/**
	 * Deserializes a message received by the stub and starts the operation
	 * requested in the message.
	 * 
	 * @param message
	 *            the received message
	 * @param session
	 *            the session
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected abstract void startOperation(Message message, Session session)
			throws IOException, ClassNotFoundException;
}
