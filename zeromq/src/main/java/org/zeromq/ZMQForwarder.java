/*
 * #%L
 * mosaic-zeromq
 * %%
 * Copyright (C) 2010 - 2012 eAustria Research Institute (Timisoara, Romania)
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
 * ZeroMQ Forwarder Device implementation.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQForwarder implements Runnable {

    private final ZMQ.Poller poller;
    private final ZMQ.Socket inSocket;
    private final ZMQ.Socket outSocket;

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
    public ZMQForwarder(Context context, Socket inSocket, Socket outSocket) {
        this.inSocket = inSocket;
        this.outSocket = outSocket;

        this.poller = context.poller(1);
        this.poller.register(inSocket, ZMQ.Poller.POLLIN);
    }

    /**
     * Forwarding messages.
     */
    @Override
	public void run() {
        byte[] msg = null;
        boolean more = true;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                // wait while there are requests to process
                if (poller.poll(250000) < 1) {
                    continue;
                }

                msg = inSocket.recv(0);

                more = inSocket.hasReceiveMore();

                if (msg != null) {
                    outSocket.send(msg, more ? ZMQ.SNDMORE : 0);
                }
            } catch (ZMQException e) {
                // context destroyed, exit
                if (ZMQ.Error.ETERM.getCode() == e.getErrorCode()) {
                    break;
                }
                throw e;
            }
        }
    }
}
