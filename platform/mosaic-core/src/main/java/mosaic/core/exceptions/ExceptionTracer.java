package mosaic.core.exceptions;

import eu.mosaic_cloud.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.exceptions.tools.NullExceptionTracer;
import eu.mosaic_cloud.exceptions.tools.QueueingExceptionTracer;
import eu.mosaic_cloud.transcript.core.Transcript;
import eu.mosaic_cloud.transcript.tools.TranscriptExceptionTracer;

public final class ExceptionTracer {

	private final TranscriptExceptionTracer transcriptTracer;

	private static final ExceptionTracer DEFAULT_INSTANCE = new ExceptionTracer();

	public ExceptionTracer() {
		super();
		Transcript transcript = Transcript.create(this);
		final QueueingExceptionTracer exceptions = QueueingExceptionTracer
				.create(NullExceptionTracer.defaultInstance);
		this.transcriptTracer = TranscriptExceptionTracer.create(transcript,
				exceptions);
	}

	public void trace(ExceptionResolution resolution, Throwable exception,
			String message_) {
		final String message = message_ == null ? "encountered exception" // NOPMD by georgiana on 9/27/11 7:11 PM
				: message_;
		switch (resolution) {
		case Handled:
			this.transcriptTracer.traceHandledException(exception, message);
			break;
		case Ignored:
			this.transcriptTracer.traceIgnoredException(exception, message);
			break;
		case Deferred:
			this.transcriptTracer.traceDeferredException(exception, message);
			break;
		default:
			break;
		}
	}

	public void trace(ExceptionResolution resolution, Throwable exception,
			String format, Object... tokens) {
		this.trace(resolution, exception, String.format(format, tokens));
	}

	public static void traceDeferred(Throwable exception) {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Deferred,
				exception, null);
	}

	public static void traceDeferred(Throwable exception, final String format,
			final Object... tokens) {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Deferred,
				exception, format, tokens);
	}

	public static void traceHandled(Throwable exception) {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Handled,
				exception, null);
	}

	public static void traceHandled(Throwable exception, String format,
			Object... tokens) {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Handled,
				exception, format, tokens);
	}

	public static void traceIgnored(Throwable exception) {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Ignored,
				exception, null);
	}

	public static void traceIgnored(Throwable exception, final String format,
			final Object... tokens) {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Ignored,
				exception, format, tokens);
	}

	public static void traceRethrown(Throwable exception) throws Throwable {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Ignored,
				exception, null);
		throw exception;
	}

	public static void traceRethrown(Throwable exception, final String format,
			final Object... tokens) throws Throwable {
		ExceptionTracer.DEFAULT_INSTANCE.trace(ExceptionResolution.Ignored,
				exception, format, tokens);
		throw exception;
	}

}
