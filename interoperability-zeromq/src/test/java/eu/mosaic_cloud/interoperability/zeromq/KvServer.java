
package eu.mosaic_cloud.interoperability.zeromq;


import java.util.HashMap;

import eu.mosaic_cloud.interoperability.core.Message;
import eu.mosaic_cloud.interoperability.core.Session;
import eu.mosaic_cloud.interoperability.core.SessionCallbacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class KvServer
		implements
			SessionCallbacks
{
	public KvServer (final ZeroMqChannel channel)
	{
		this.logger = LoggerFactory.getLogger (this.getClass ());
		this.bucket = new HashMap<String, String> ();
		channel.register (KvSession.Server);
		channel.accept (KvSession.Server, this);
	}
	
	@Override
	public final synchronized void created (final Session session)
	{}
	
	@Override
	public final synchronized void destroyed (final Session session)
	{}
	
	@Override
	public final synchronized void received (final Session session, final Message message)
	{
		switch ((KvMessage) message.specification) {
			case Access : {
				this.logger.info ("access requested");
			}
				break;
			case GetRequest : {
				final KvMessage.GetRequest request = (KvMessage.GetRequest) message.payload;
				this.logger.info ("get requested: {}", request.key);
				final String value = this.bucket.get (request.key);
				session.send (new Message (KvMessage.GetReply, new KvMessage.GetReply (request.sequence, value)));
			}
				break;
			case PutRequest : {
				final KvMessage.PutRequest request = (KvMessage.PutRequest) message.payload;
				this.logger.info ("put requested: {} -> {}", request.key, request.value);
				this.bucket.put (request.key, request.value);
				session.send (new Message (KvMessage.Ok, new KvMessage.Ok (request.sequence)));
			}
				break;
			default: {
				this.logger.error ("unexpected message: {}", message.specification);
				session.send (new Message (KvMessage.Aborted, null));
			}
				break;
		}
	}
	
	private final HashMap<String, String> bucket;
	private final Logger logger;
}
