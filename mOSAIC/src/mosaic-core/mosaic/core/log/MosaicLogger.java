package mosaic.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MosaicLogger {
	private static MosaicLogger mLogger;

	private final Logger logger;

	private MosaicLogger() {
		logger = LoggerFactory.getLogger(MosaicLogger.class);
	}

	public final static MosaicLogger getLogger() {
		if (mLogger == null) {
			mLogger = new MosaicLogger();
		}
		return mLogger;
	}

	public void trace(String message) {
		if (logger.isTraceEnabled()) {
			logger.trace("[TRACE] " + message);
		}
	}

	public void debug(String message) {
		if (logger.isDebugEnabled()) {
			logger.debug("[DEBUG] " + message);
		}
	}

	public void info(String message) {
		if (logger.isInfoEnabled()) {
			logger.info("[INFO] " + message);
		}
	}

	public void warn(String message) {
		if (logger.isWarnEnabled()) {
			logger.warn("[WARNING] " + message);
		}
	}

	public void error(String message) {
		if (logger.isErrorEnabled()) {
			logger.error("[ERROR] " + message);
		}
	}
}
