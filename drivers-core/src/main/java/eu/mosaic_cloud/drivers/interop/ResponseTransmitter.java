/*
 * #%L
 * mosaic-drivers-core
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.drivers.interop;


import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.slf4j.Logger;


/**
 * Base class for driver response transmitter.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResponseTransmitter
{
	public ResponseTransmitter ()
	{
		this.logger = Transcript.create (this, true).adaptAs (Logger.class);
	}
	
	/**
	 * Destroys the transmitter.
	 */
	public void destroy ()
	{
		this.logger.trace ("ResponseTransmitter destroyed.");
	}
	
	/**
	 * Sends result to the connector's proxy.
	 * 
	 * @param session
	 *            the session to which the message belongs
	 * @param message
	 *            the message
	 */
	protected void publishResponse (final Session session, final Message message)
	{
		session.send (message);
	}
	
	protected Logger logger;
}
