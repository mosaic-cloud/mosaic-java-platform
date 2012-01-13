/*
 * #%L
 * mosaic-zeromq
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
package org.zeromq;

/**
 * ZeroMQ runtime exception.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQException extends RuntimeException {
    private static final long serialVersionUID = -978820750094924644L;

    private int errorCode = 0;

    public ZMQException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * @return error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    @Override
	public String toString() {
        return super.toString() + "(0x" + Integer.toHexString(errorCode) + ")";
    }
}
