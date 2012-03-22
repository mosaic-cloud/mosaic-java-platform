/*
 * #%L
 * mosaic-platform-core
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

package eu.mosaic_cloud.platform.core.exceptions;

import eu.mosaic_cloud.tools.exceptions.core.ExceptionResolution;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;
import eu.mosaic_cloud.tools.transcript.tools.TranscriptExceptionTracer;

public final class ExceptionTracer {

    private final TranscriptExceptionTracer transcriptTracer;
    private static final ExceptionTracer DEFAULT_INSTANCE = new ExceptionTracer();

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

    public ExceptionTracer() {
        super();
        final Transcript transcript = Transcript.create(this);
        this.transcriptTracer = TranscriptExceptionTracer.create(transcript,
                FallbackExceptionTracer.defaultInstance);
    }

    public void trace(ExceptionResolution resolution, Throwable exception,
            String message_) {
        final String message = message_ == null ? "encountered exception" // NOPMD
                                                                          // by
                                                                          // georgiana
                                                                          // on
                                                                          // 9/27/11
                                                                          // 7:11
                                                                          // PM
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
}
