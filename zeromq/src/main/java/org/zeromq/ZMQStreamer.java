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

import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * ZeroMQ Streamer Device implementation.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQStreamer extends ZMQForwarder {

    /**
     * Class constructor.
     * 
     * @param context
     *            a 0MQ context previously created.
     * @param inSocket
     *            input socket
     * @param outSocket
     *            output socket
     */
    public ZMQStreamer(Context context, Socket inSocket, Socket outSocket) {
        super(context, inSocket, outSocket);
    }
}
