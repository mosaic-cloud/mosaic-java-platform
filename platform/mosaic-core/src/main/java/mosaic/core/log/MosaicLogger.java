package mosaic.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * Logger used in the platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class MosaicLogger {

	private static MosaicLogger mLogger;

	private final Logger logger; // NOPMD by georgiana on 9/27/11 7:14 PM

	private MosaicLogger() {
		this.logger = LoggerFactory.getLogger(MosaicLogger.class);
		((ch.qos.logback.classic.Logger) this.logger).setLevel(Level.TRACE);
	}

	/**
	 * Returns the mOSAIC logger.
	 * 
	 * @return the logger
	 */
	public synchronized static MosaicLogger getLogger() { // NOPMD by georgiana
															// on 9/27/11 7:15
															// PM
		if (MosaicLogger.mLogger == null) {
			MosaicLogger.mLogger = new MosaicLogger();
		}
		return MosaicLogger.mLogger;
	}

	/**
	 * Logs a trace message.
	 * 
	 * @param message
	 *            the message
	 */
	public void trace(final String message) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(message);
		}
	}

	/**
	 * Logs a debug message.
	 * 
	 * @param message
	 *            the message
	 */
	public void debug(final String message) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(message);
		}
	}

	/**
	 * Logs a info message.
	 * 
	 * @param message
	 *            the message
	 */
	public void info(final String message) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(message);
		}
	}

	/**
	 * Logs a warning message.
	 * 
	 * @param message
	 *            the message
	 */
	public void warn(final String message) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(message);
		}
	}

	/**
	 * Logs an error message.
	 * 
	 * @param message
	 *            the message
	 */
	public void error(final String message) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(message);
		}
	}

	public void trace(String message, Throwable exception) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(message);
		}
	}

	public void warn(String message, Throwable exception) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(message);
		}

	}
}
