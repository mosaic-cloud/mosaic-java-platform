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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger used in the platform.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class MosaicLogger {

    private final Logger logger; // NOPMD by georgiana on 9/27/11 7:14 PM

    private MosaicLogger(Class<?> owner) {
        this.logger = LoggerFactory.getLogger(owner);
    }

    /**
     * Returns a mOSAIC logger.
     * 
     * @param owner
     *            logged object
     * 
     * @return the logger
     */
    public static MosaicLogger createLogger(Class<?> owner) { // NOPMD by
                                                              // georgiana
        // on 9/27/11 7:15
        // PM
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
                                                            // georgiana
        // on 9/27/11 7:15
        // PM
        return new MosaicLogger(owner.getClass());
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

    public void trace(String message, Throwable exception) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(message);
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

    public void warn(String message, Throwable exception) {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(message);
        }
    }
}
