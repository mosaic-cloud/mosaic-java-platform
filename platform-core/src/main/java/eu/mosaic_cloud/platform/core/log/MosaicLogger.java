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

package eu.mosaic_cloud.platform.core.log;

import eu.mosaic_cloud.tools.transcript.core.Transcript;

/**
 * Logger used in the platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class MosaicLogger {

    private final Transcript logger;

    private MosaicLogger(Class<?> owner) {
        this.logger = Transcript.create(owner);
    }

    /**
     * Returns a mOSAIC logger.
     * 
     * @param owner
     *            logged object
     * 
     * @return the logger
     */
    public static MosaicLogger createLogger(Class<?> owner) { // NOPMD
        return new MosaicLogger(owner);
    }

    /**
     * Returns a mOSAIC logger.
     * 
     * @param owner
     *            logged object
     * 
     * @return the logger
     */
    public static MosaicLogger createLogger(Object owner) { // NOPMD by
        return new MosaicLogger(owner.getClass());
    }

    /**
     * Logs a debug message.
     * 
     * @param message
     *            the message
     */
    public void debug(final String message) {
        this.logger.traceDebugging(message);
    }

    /**
     * Logs a debug message.
     * 
     * @param format
     *            the message format
     * @param tokens
     */
    public void debug(final String format, final Object... tokens) {
        this.logger.traceDebugging(format, tokens);
    }

    /**
     * Logs an error message.
     * 
     * @param message
     *            the message
     */
    public void error(final String message) {
        this.logger.traceError(message);
    }

    /**
     * Logs an error message.
     * 
     * @param format
     *            the message format
     * @param tokens
     */
    public void error(final String format, final Object... tokens) {
        this.logger.traceError(format, tokens);
    }

    /**
     * Logs a info message.
     * 
     * @param message
     *            the message
     */
    public void info(final String message) {
        this.logger.traceInformation(message);
    }

    /**
     * Logs a info message.
     * 
     * @param format
     *            the message format
     * @param tokens
     */
    public void info(final String format, final Object... tokens) {
        this.logger.traceInformation(format, tokens);
    }

    /**
     * Logs a trace message.
     * 
     * @param message
     *            the message
     */
    public void trace(final String message) {
        this.logger.traceInformation(message);
    }

    /**
     * Logs a trace message.
     * 
     * @param format
     *            the message format
     * @param tokens
     */
    public void trace(final String format, final Object... tokens) {
        this.logger.traceInformation(format, tokens);
    }

    /**
     * Logs a warning message.
     * 
     * @param message
     *            the message
     */
    public void warn(final String message) {
        this.logger.traceWarning(message);
    }

    /**
     * Logs a warning message.
     * 
     * @param format
     *            the message format
     * @param tokens
     */
    public void warn(final String format, final Object... tokens) {
        this.logger.traceWarning(format, tokens);
    }

}
