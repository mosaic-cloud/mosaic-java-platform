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
		logger = LoggerFactory.getLogger(MosaicLogger.class);
	}

	/**
	 * Returns the mOSAIC logger.
	 * 
	 * @return the logger
	 */
	public final  synchronized static MosaicLogger getLogger() {
		if (mLogger == null) {
			mLogger = new MosaicLogger();
		}
		return mLogger;
	}

	/**
	 * Logs a trace message.
	 * 
	 * @param message
	 *            the message
	 */
	public void trace(String message) {
		if (logger.isTraceEnabled()) {
			logger.trace("[TRACE] " + message);
		}
	}

	/**
	 * Logs a debug message.
	 * 
	 * @param message
	 *            the message
	 */
	public void debug(String message) {
		if (logger.isDebugEnabled()) {
			logger.debug("[DEBUG] " + message);
		}
	}

	/**
	 * Logs a info message.
	 * 
	 * @param message
	 *            the message
	 */
	public void info(String message) {
		if (logger.isInfoEnabled()) {
			logger.info("[INFO] " + message);
		}
	}

	/**
	 * Logs a warning message.
	 * 
	 * @param message
	 *            the message
	 */
	public void warn(String message) {
		if (logger.isWarnEnabled()) {
			logger.warn("[WARNING] " + message);
		}
	}

	/**
	 * Logs an error message.
	 * 
	 * @param message
	 *            the message
	 */
	public void error(String message) {
		if (logger.isErrorEnabled()) {
			logger.error("[ERROR] " + message);
		}
	}
}
