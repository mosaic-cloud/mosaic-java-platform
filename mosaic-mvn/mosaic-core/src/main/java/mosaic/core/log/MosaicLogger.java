package mosaic.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger used in the platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public class MosaicLogger {
	private static MosaicLogger mLogger;

	private final Logger logger;

	private MosaicLogger() {
		this.logger = LoggerFactory.getLogger(MosaicLogger.class);
	}

	/**
	 * Returns the mOSAIC logger.
	 * 
	 * @return the logger
	 */
	public final synchronized static MosaicLogger getLogger() {
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
	public void trace(String message) {
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
	public void debug(String message) {
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
	public void info(String message) {
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
	public void warn(String message) {
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
	public void error(String message) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(message);
		}
	}
}
