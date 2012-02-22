/*
 * #%L
 * mosaic-examples-interoperability
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

package eu.mosaic_cloud.examples.interoperability.kv;


import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.interoperability.implementations.zeromq.ZeroMqChannel;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.threading.tools.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class KvServer
		implements
			SessionCallbacks
{
	public KvServer (final ExceptionTracer exceptions, final long maxDelay)
	{
		this.exceptions = exceptions;
		this.logger = LoggerFactory.getLogger (this.getClass ());
		this.bucket = new ConcurrentHashMap<String, String> ();
		this.maxDelay = maxDelay;
	}
	
	@Override
	public final CallbackCompletion<Void> created (final Session session)
	{
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> destroyed (final Session session)
	{
		return (null);
	}
	
	@Override
	public final CallbackCompletion<Void> failed (final Session session, final Throwable Exception)
	{
		return (null);
	}
	
	public final void initialize (final ZeroMqChannel channel)
	{
		channel.accept (KvSession.Server, this);
	}
	
	@Override
	public final CallbackCompletion<Void> received (final Session session, final Message message)
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
				Threading.sleep ((this.maxDelay / 5) * 1);
				this.logger.info ("get replied [{}]: {}", request.sequence, value);
				session.send (new Message (KvMessage.GetReply, new KvPayloads.GetReply (request.sequence, value)));
				Threading.sleep ((this.maxDelay / 5) * 3);
				this.logger.info ("get finished [{}]", request.sequence);
			}
				break;
			case PutRequest : {
				final KvPayloads.PutRequest request = (KvPayloads.PutRequest) message.payload;
				this.logger.info ("put requested [{}]: {} -> {}", new Object[] {request.sequence, request.key, request.value});
				this.bucket.put (request.key, request.value);
				session.continueDispatch ();
				Threading.sleep ((this.maxDelay / 5) * 1);
				this.logger.info ("put replied [{}]", request.sequence);
				session.send (new Message (KvMessage.Ok, new KvPayloads.Ok (request.sequence)));
				Threading.sleep ((this.maxDelay / 5) * 3);
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
	private final ExceptionTracer exceptions;
	private final Logger logger;
	private final long maxDelay;
}
