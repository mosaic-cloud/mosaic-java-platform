/*
 * #%L
 * interoperability-examples
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
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

package eu.mosaic_cloud.interoperability.examples.kv;


import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class KvServer
		implements
			SessionCallbacks
{
	public KvServer ()
	{
		this.logger = LoggerFactory.getLogger (this.getClass ());
		this.bucket = new ConcurrentHashMap<String, String> ();
	}
	
	@Override
	public final CallbackReference created (final Session session)
	{
		return (null);
	}
	
	@Override
	public final CallbackReference destroyed (final Session session)
	{
		return (null);
	}
	
	@Override
	public final CallbackReference failed (final Session session, final Throwable Exception)
	{
		return (null);
	}
	
	public final void initialize (final ZeroMqChannel channel)
	{
		channel.accept (KvSession.Server, this);
	}
	
	@Override
	public final CallbackReference received (final Session session, final Message message)
	{
		switch ((KvMessage) message.specification) {
			case Access : {
				this.logger.info ("access requested");
			}
				break;
			case GetRequest : {
				final KvPayloads.GetRequest request = (KvPayloads.GetRequest) message.payload;
				this.logger.info ("get requested [{}]: {}", request.sequence, request.key);
				final String value = this.bucket.get (request.key);
				session.continueDispatch ();
				try {
					Thread.sleep (500);
				} catch (final InterruptedException exception) {}
				this.logger.info ("get replied [{}]: {}", request.sequence, value);
				session.send (new Message (KvMessage.GetReply, new KvPayloads.GetReply (request.sequence, value)));
				try {
					Thread.sleep (2000);
				} catch (final InterruptedException exception) {}
				this.logger.info ("get finished [{}]", request.sequence);
			}
				break;
			case PutRequest : {
				final KvPayloads.PutRequest request = (KvPayloads.PutRequest) message.payload;
				this.logger.info ("put requested [{}]: {} -> {}", new Object[] {request.sequence, request.key, request.value});
				this.bucket.put (request.key, request.value);
				session.continueDispatch ();
				try {
					Thread.sleep (500);
				} catch (final InterruptedException exception) {}
				this.logger.info ("put replied [{}]", request.sequence);
				session.send (new Message (KvMessage.Ok, new KvPayloads.Ok (request.sequence)));
				try {
					Thread.sleep (2000);
				} catch (final InterruptedException exception) {}
				this.logger.info ("put finished: [{}]", request.sequence);
			}
				break;
			default: {
				this.logger.error ("unexpected message: {}", message.specification);
				session.send (new Message (KvMessage.Aborted, null));
			}
				break;
		}
		return (null);
	}
	
	private final ConcurrentHashMap<String, String> bucket;
	private final Logger logger;
}
