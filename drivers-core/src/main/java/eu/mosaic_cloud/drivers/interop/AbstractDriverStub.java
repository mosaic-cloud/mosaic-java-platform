/*
 * #%L
 * mosaic-drivers-core
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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
import eu.mosaic_cloud.platform.v2.configuration.Configuration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;


/**
 * Base class for driver stubs.
 * 
 * @author Georgiana Macariu
 */
public abstract class AbstractDriverStub
			implements
				SessionCallbacks
{
	/**
	 * Builds a driver stub.
	 * 
	 * @param config
	 *            configuration data for the driver and its stub
	 * @param transmitter
	 *            the transmitter which will serialize and send responses back to the connector
	 * @param driver
	 *            the driver which will handle requests received by the stub
	 * @param commChannel
	 *            the channel for communicating with connectors
	 */
	protected AbstractDriverStub (final Configuration config, final ResponseTransmitter transmitter, final IResourceDriver driver, final ZeroMqChannel commChannel) {
		super ();
		this.configuration = config;
		this.sessions = new ArrayList<Session> ();
		this.commChannel = commChannel;
		this.transmitter = transmitter;
		this.driver = driver;
		this.exceptions = FallbackExceptionTracer.defaultInstance;
	}
	
	@Override
	public CallbackCompletion<Void> created (final Session session) {
		// FIXME: handle session created
		if (!this.sessions.contains (session)) {
			this.sessions.add (session);
		}
		return null;
	}
	
	/**
	 * Destroys this stub.
	 */
	public synchronized void destroy () {
		this.driver.destroy ();
		this.transmitter.destroy ();
		this.commChannel.terminate (500);
		AbstractDriverStub.logger.trace ("DriverStub destroyed.");
	}
	
	@Override
	public CallbackCompletion<Void> destroyed (final Session session) {
		// NOTE: handle session destroyed
		AbstractDriverStub.logger.trace ("Session destroyed.");
		this.sessions.remove (session);
		return null;
	}
	
	@Override
	public CallbackCompletion<Void> failed (final Session session, final Throwable exception) {
		AbstractDriverStub.logger.error ("Session failed");
		return null;
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
	public <T extends IResourceDriver> T getDriver (final Class<T> driverClass) {
		return driverClass.cast (this.driver);
	}
	
	@Override
	public CallbackCompletion<Void> received (final Session session, final Message message) {
		try {
			this.startOperation (message, session);
		} catch (final IOException e) {
			this.exceptions.traceIgnoredException (e);
		} catch (final ClassNotFoundException e) {
			this.exceptions.traceIgnoredException (e);
		}
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
	protected <T extends ResponseTransmitter> T getResponseTransmitter (final Class<T> transClass) {
		return transClass.cast (this.transmitter);
	}
	
	/**
	 * Deserializes a message received by the stub and starts the operation requested in the message.
	 * 
	 * @param message
	 *            the received message
	 * @param session
	 *            the session
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected abstract void startOperation (Message message, Session session)
				throws IOException, ClassNotFoundException;
	
	protected final Configuration configuration;
	protected final BaseExceptionTracer exceptions;
	private final ZeroMqChannel commChannel;
	private final IResourceDriver driver;
	private final List<Session> sessions;
	private final ResponseTransmitter transmitter;
	
	protected static int decDriverReference (final AbstractDriverStub stub) {
		synchronized (AbstractDriverStub.MONITOR) {
			Integer ref = AbstractDriverStub.references.get (stub);
			if (ref == null) {
				ref = 0;
			}
			ref--;
			if (ref == 0) {
				AbstractDriverStub.references.remove (stub);
			}
			return ref;
		}
	}
	
	protected static void incDriverReference (final AbstractDriverStub stub) {
		synchronized (AbstractDriverStub.MONITOR) {
			Integer ref = AbstractDriverStub.references.get (stub);
			if (ref == null) {
				ref = 0;
			}
			ref++;
			AbstractDriverStub.references.put (stub, ref);
		}
	}
	
	protected static final Object MONITOR = Monitor.create (AbstractDriverStub.class);
	private static final Logger logger = Transcript.create (AbstractDriverStub.class).adaptAs (Logger.class);
	private static Map<AbstractDriverStub, Integer> references = new IdentityHashMap<AbstractDriverStub, Integer> ();
}
