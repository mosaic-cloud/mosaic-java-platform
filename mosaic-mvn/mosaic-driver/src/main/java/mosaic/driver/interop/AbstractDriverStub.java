package mosaic.driver.interop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import mosaic.core.configuration.IConfiguration;
import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.log.MosaicLogger;
import mosaic.driver.IResourceDriver;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;

/**
 * Base class for driver stubs.
 * 
 * @author Georgiana Macariu
 * 
 */
public abstract class AbstractDriverStub implements SessionCallbacks {

	protected IConfiguration configuration;

	private ResponseTransmitter transmitter;
	private IResourceDriver driver;
	private List<Session> sessions;
	private ZeroMqChannel commChannel;

	protected static final Object lock = new Object();
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
		this.sessions = new ArrayList<Session>();
		this.commChannel = commChannel;

		this.transmitter = transmitter;
		this.driver = driver;
	}

	/**
	 * Destroys this stub.
	 * 
	 */
	public synchronized void destroy() {
		this.driver.destroy();
		this.transmitter.destroy();
		try {
			this.commChannel.terminate(500);
		} catch (InterruptedException e) {
			ExceptionTracer.traceDeferred(e);
		}
		MosaicLogger.getLogger().trace("DriverStub destroyed.");
	}

	protected static synchronized void incDriverReference(
			AbstractDriverStub stub) {
		Integer ref = AbstractDriverStub.references.get(stub);
		if (ref == null) {
			ref = 0;
		}
		ref++;
		AbstractDriverStub.references.put(stub, ref);
	}

	protected static synchronized int decDriverReference(AbstractDriverStub stub) {
		Integer ref = AbstractDriverStub.references.get(stub);
		if (ref == null) {
			ref = 0;
		}
		ref--;
		if (ref == 0) {
			AbstractDriverStub.references.remove(stub);
		}
		return ref;
	}

	@Override
	public CallbackReference received(Session session, Message message) {
		try {
			startOperation(message, session);
		} catch (IOException e) {
			ExceptionTracer.traceDeferred(e);
		} catch (ClassNotFoundException e) {
			ExceptionTracer.traceDeferred(e);
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
		MosaicLogger.getLogger().trace("Session destroyed.");
		this.sessions.remove(session);
		return null;
	}

	@Override
	public CallbackReference failed(Session session, Throwable exception) {
		// TODO handle session fail
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
	protected <T extends IResourceDriver> T getDriver(Class<T> driverClass) {
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