/*
 * #%L
 * mosaic-examples-interoperability
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

package eu.mosaic_cloud.examples.interoperability.kv;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import eu.mosaic_cloud.interoperability.core.Channel;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


public final class KvClient
			implements
				SessionCallbacks
{
	public KvClient () {
		this.logger = LoggerFactory.getLogger (this.getClass ());
		this.futures = Collections.synchronizedMap (new HashMap<Long, DeferredFuture<?>> ());
		this.session = null;
		this.sequence = new AtomicLong (0);
	}
	
	@Override
	public final synchronized CallbackCompletion<Void> created (final Session session) {
		Preconditions.checkState (this.session == null);
		final DeferredFuture<Boolean> future = (DeferredFuture<Boolean>) this.futures.remove (Long.valueOf (0));
		if (future != null)
			future.trigger.triggerSucceeded (Boolean.TRUE);
		this.session = session;
		return (null);
	}
	
	@Override
	public final synchronized CallbackCompletion<Void> destroyed (final Session session) {
		Preconditions.checkState (this.session == session);
		synchronized (this.futures) {
			for (final DeferredFuture<?> future : this.futures.values ())
				future.cancel (true);
			this.futures.clear ();
		}
		return (null);
	}
	
	@Override
	public final synchronized CallbackCompletion<Void> failed (final Session session, final Throwable exception) {
		Preconditions.checkState (this.session == session);
		return (null);
	}
	
	public final synchronized Future<String> get (final String key) {
		Preconditions.checkState (this.session != null);
		final long sequence = this.sequence.incrementAndGet ();
		final DeferredFuture<String> future = DeferredFuture.create (String.class);
		this.futures.put (Long.valueOf (sequence), future);
		this.session.send (new Message (KvMessage.GetRequest, new KvPayloads.GetRequest (sequence, key)));
		return (future);
	}
	
	public final synchronized Future<Boolean> initialize (final Channel channel, final String serverIdentifier) {
		Preconditions.checkState (this.session == null);
		Preconditions.checkState (this.sequence.get () == 0);
		final DeferredFuture<Boolean> future = DeferredFuture.create (Boolean.class);
		this.futures.put (Long.valueOf (0), future);
		channel.connect (serverIdentifier, KvSession.Client, new Message (KvMessage.Access, null), this);
		return (future);
	}
	
	public final synchronized Future<Boolean> put (final String key, final String value) {
		Preconditions.checkState (this.session != null);
		final long sequence = this.sequence.incrementAndGet ();
		final DeferredFuture<Boolean> future = DeferredFuture.create (Boolean.class);
		this.futures.put (Long.valueOf (sequence), future);
		this.session.send (new Message (KvMessage.PutRequest, new KvPayloads.PutRequest (sequence, key, value)));
		return (future);
	}
	
	@Override
	public final synchronized CallbackCompletion<Void> received (final Session session, final Message message) {
		Preconditions.checkState (this.session == session);
		switch ((KvMessage) message.specification) {
			case GetReply : {
				final KvPayloads.GetReply reply = (KvPayloads.GetReply) message.payload;
				this.logger.info ("get replied: {}", reply.value);
				final DeferredFuture<String> future = (DeferredFuture<String>) this.futures.remove (Long.valueOf (reply.sequence));
				if (future != null)
					future.trigger.triggerSucceeded (reply.value);
			}
				break;
			case Ok : {
				final KvPayloads.Ok reply = (KvPayloads.Ok) message.payload;
				this.logger.info ("ok replied: {}");
				final DeferredFuture<Boolean> future = (DeferredFuture<Boolean>) this.futures.remove (Long.valueOf (reply.sequence));
				if (future != null)
					future.trigger.triggerSucceeded (Boolean.TRUE);
			}
				break;
			case Error : {
				final KvPayloads.Error reply = (KvPayloads.Error) message.payload;
				this.logger.info ("error replied: {}");
				final DeferredFuture<Boolean> future = (DeferredFuture<Boolean>) this.futures.remove (Long.valueOf (reply.sequence));
				if (future != null)
					future.trigger.triggerSucceeded (Boolean.FALSE);
			}
				break;
			default : {
				this.logger.error ("unexpected message: {}", message.specification);
				session.send (new Message (KvMessage.Aborted, null));
			}
				break;
		}
		return (null);
	}
	
	private final Map<Long, DeferredFuture<?>> futures;
	private final Logger logger;
	private final AtomicLong sequence;
	private Session session;
}
