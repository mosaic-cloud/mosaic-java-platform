/*
 * #%L
 * mosaic-tools-zeromq
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
