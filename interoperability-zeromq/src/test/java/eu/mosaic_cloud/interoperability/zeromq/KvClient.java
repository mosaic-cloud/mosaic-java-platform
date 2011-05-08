
package eu.mosaic_cloud.interoperability.zeromq;


import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class KvClient
		implements
			SessionCallbacks
{
	public KvClient (final ZeroMqChannel channel, final String serverIdentifier)
	{
		this.logger = LoggerFactory.getLogger (this.getClass ());
		this.futures = new HashMap<Long, KvClient.OutcomeFuture<?>> ();
		this.session = null;
		this.sequence = 0;
		channel.register (KvSession.Client);
		channel.create (serverIdentifier, KvSession.Client, new Message (KvMessage.Access, null), this);
		try {
			Thread.sleep (100);
		} catch (final InterruptedException exception) {}
	}
	
	@Override
	public final synchronized void created (final Session session)
	{
		Preconditions.checkState (this.session == null);
		this.session = session;
	}
	
	@Override
	public final synchronized void destroyed (final Session session)
	{
		Preconditions.checkState (this.session == session);
		for (final OutcomeFuture<?> future : this.futures.values ())
			future.cancel (true);
		this.futures.clear ();
	}
	
	public final synchronized Future<String> get (final String key)
	{
		Preconditions.checkState (this.session != null);
		final long sequence = this.sequence++;
		final OutcomeFuture<String> future = OutcomeFuture.create ();
		this.futures.put (Long.valueOf (sequence), future);
		this.session.send (new Message (KvMessage.GetRequest, new KvMessage.GetRequest (sequence, key)));
		return (future);
	}
	
	public final synchronized Future<Boolean> put (final String key, final String value)
	{
		Preconditions.checkState (this.session != null);
		final long sequence = this.sequence++;
		final OutcomeFuture<Boolean> future = OutcomeFuture.create ();
		this.futures.put (Long.valueOf (sequence), future);
		this.session.send (new Message (KvMessage.PutRequest, new KvMessage.PutRequest (sequence, key, value)));
		return (future);
	}
	
	@Override
	public void received (final Session session, final Message message)
	{
		switch ((KvMessage) message.specification) {
			case GetReply : {
				final KvMessage.GetReply reply = (KvMessage.GetReply) message.payload;
				this.logger.info ("get replied: {}", reply.value);
				final OutcomeFuture<?> future = this.futures.get (Long.valueOf (reply.sequence));
				if (future != null)
					((OutcomeFuture<String>) future).completed (reply.value);
			}
				break;
			case Ok : {
				final KvMessage.Ok reply = (KvMessage.Ok) message.payload;
				this.logger.info ("ok replied: {}");
				final OutcomeFuture<?> future = this.futures.get (Long.valueOf (reply.sequence));
				if (future != null)
					((OutcomeFuture<Boolean>) future).completed (Boolean.TRUE);
			}
				break;
			case Error : {
				final KvMessage.Error reply = (KvMessage.Error) message.payload;
				this.logger.info ("error replied: {}");
				final OutcomeFuture<?> future = this.futures.get (Long.valueOf (reply.sequence));
				if (future != null)
					((OutcomeFuture<Boolean>) future).completed (Boolean.FALSE);
			}
				break;
			default: {
				this.logger.error ("unexpected message: {}", message.specification);
				session.send (new Message (KvMessage.Aborted, null));
			}
				break;
		}
	}
	
	private final HashMap<Long, OutcomeFuture<?>> futures;
	private final Logger logger;
	private long sequence;
	private Session session;
	
	private static final class OutcomeCallable<_Outcome_ extends Object>
			implements
				Callable<_Outcome_>
	{
		@Override
		public final _Outcome_ call ()
		{
			return (this.outcome);
		}
		
		_Outcome_ outcome;
	}
	
	private static final class OutcomeFuture<_Outcome_ extends Object>
			extends FutureTask<_Outcome_>
	{
		OutcomeFuture (final OutcomeCallable<_Outcome_> callable)
		{
			super (callable);
			this.callable = callable;
		}
		
		final void completed (final _Outcome_ outcome)
		{
			this.callable.outcome = outcome;
			this.run ();
		}
		
		final OutcomeCallable<_Outcome_> callable;
		
		static final <_Outcome_ extends Object> OutcomeFuture<_Outcome_> create ()
		{
			return (new OutcomeFuture<_Outcome_> (new OutcomeCallable<_Outcome_> ()));
		}
	}
}
