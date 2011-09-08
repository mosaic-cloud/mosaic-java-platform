package mosaic.core.exceptions;

import java.util.concurrent.ConcurrentLinkedQueue;

import mosaic.core.log.MosaicLogger;

public final class ExceptionTracer {

	private final ConcurrentLinkedQueue<Throwable> ignoredExceptions;
	private final MosaicLogger logger;

	public static final ExceptionTracer defaultInstance = new ExceptionTracer(
			ExceptionTracer.defaultInstanceUseLogger);
	private static final boolean defaultInstanceUseLogger = true;

	public ExceptionTracer(final boolean useLogger) {
		super();
		if (useLogger) {
			this.logger = MosaicLogger.getLogger();// LoggerFactory.getLogger("mosaic.exceptions");
		} else {
			this.logger = null;
		}
		this.ignoredExceptions = new ConcurrentLinkedQueue<Throwable>();
	}

	public final void resetIgnoredExceptions() {
		this.ignoredExceptions.clear();
	}

	public final Iterable<Throwable> selectIgnoredExceptions() {
		return (this.ignoredExceptions);
	}

	public final void trace(ExceptionResolution resolution,
			Throwable exception, String message_) throws Throwable {
		final String message = message_ != null ? message_
				: "encountered exception";
		if (this.logger != null) {
			switch (resolution) {
			case Handled:
				this.logger.trace(message, exception);
				break;
			case Rethrown:
				this.logger.trace(message, exception);
				break;
			case Ignored:
				this.logger.warn(message, exception);
				break;
			case Deferred:
				this.logger.trace(message, exception);
				break;
			default:
				this.logger.trace(message, exception);
				break;
			}
		} else {
			synchronized (System.err) {
				switch (resolution) {
				case Handled:
					System.err.println("[  ] " + message + " / "
							+ exception.toString());
					break;
				case Rethrown:
					System.err.println("[  ] " + message + " / "
							+ exception.toString());
					break;
				case Ignored:
					System.err.println("[ww] " + message + " / "
							+ exception.toString());
					break;
				case Deferred:
					System.err.println("[  ] " + message + " / "
							+ exception.toString());
					break;
				default:
					System.err.println("[??] " + message + " / "
							+ exception.toString());
					break;
				}
				exception.printStackTrace(System.err);
			}
		}
		switch (resolution) {
		case Ignored:
			this.ignoredExceptions.add(exception);
			break;
		case Rethrown:
			throw exception;
		default:
			break;
		}
	}

	public final void trace(ExceptionResolution resolution,
			Throwable exception, String format, Object... tokens) {
		try {
			this.trace(resolution, exception, String.format(format, tokens));
		} catch (Throwable e) {
		}
	}

	public static final void traceDeferred(Throwable exception) {
		try {
			ExceptionTracer.defaultInstance.trace(ExceptionResolution.Deferred,
					exception, null);
		} catch (Throwable e) {
		}
	}

	public static final void traceDeferred(Throwable exception,
			final String format, final Object... tokens) {
		ExceptionTracer.defaultInstance.trace(ExceptionResolution.Deferred,
				exception, format, tokens);
	}

	public static final void traceHandled(Throwable exception) {
		try {
			ExceptionTracer.defaultInstance.trace(ExceptionResolution.Handled,
					exception, null);
		} catch (Throwable e) {
		}
	}

	public static final void traceHandled(Throwable exception, String format,
			Object... tokens) {
		ExceptionTracer.defaultInstance.trace(ExceptionResolution.Handled,
				exception, format, tokens);
	}

	public static final void traceIgnored(Throwable exception) {
		try {
			ExceptionTracer.defaultInstance.trace(ExceptionResolution.Ignored,
					exception, null);
		} catch (Throwable e) {
		}
	}

	public static final void traceIgnored(Throwable exception,
			final String format, final Object... tokens) {
		ExceptionTracer.defaultInstance.trace(ExceptionResolution.Ignored,
				exception, format, tokens);
	}

	public static final void traceRethrown(Throwable exception)
			throws Throwable {
		ExceptionTracer.defaultInstance.trace(ExceptionResolution.Rethrown,
				exception, null);
	}

	public static final void traceRethrown(Throwable exception,
			final String format, final Object... tokens) {
		ExceptionTracer.defaultInstance.trace(ExceptionResolution.Rethrown,
				exception, format, tokens);
	}

}
