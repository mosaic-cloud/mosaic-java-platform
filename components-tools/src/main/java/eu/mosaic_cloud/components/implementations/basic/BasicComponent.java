
package eu.mosaic_cloud.components.implementations.basic;


import java.util.HashMap;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.AbstractService;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReactor;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Channel;
import eu.mosaic_cloud.components.core.ChannelCallbacks;
import eu.mosaic_cloud.components.core.ChannelMessage;
import eu.mosaic_cloud.components.core.ChannelMessageType;
import eu.mosaic_cloud.components.core.ComponentCallReference;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.tools.Monitor;
import eu.mosaic_cloud.transcript.core.Transcript;


public final class BasicComponent
		extends Object
		implements
			eu.mosaic_cloud.components.core.Component
{
	private BasicComponent (final Channel channel, final CallbackReactor callbackReactor, final ComponentCallbacks callbacks)
	{
		super ();
		this.delegate = new Component (this, channel, callbackReactor, callbacks);
	}
	
	@Override
	public final void assign (final ComponentCallbacks callbacks)
	{
		this.delegate.assign (callbacks);
	}
	
	@Override
	public void call (final ComponentIdentifier component, final ComponentCallRequest request)
	{
		this.delegate.call (component, request);
	}
	
	@Override
	public void cast (final ComponentIdentifier component, final ComponentCastRequest request)
	{
		this.delegate.cast (component, request);
	}
	
	public final void initialize ()
	{
		this.delegate.startAndWait ();
	}
	
	@Override
	public void reply (final ComponentCallReply reply)
	{
		this.delegate.reply (reply);
	}
	
	@Override
	public final void terminate ()
	{
		this.delegate.stop ();
	}
	
	private final Component delegate;
	
	public static final BasicComponent create (final Channel channel, final CallbackReactor reactor, final ComponentCallbacks callbacks)
	{
		return (new BasicComponent (channel, reactor, callbacks));
	}
	
	private static enum Action
	{
		Call (),
		Cast (),
		Return ();
	}
	
	private static final class Component
			extends AbstractService
			implements
				ChannelCallbacks,
				CallbackHandler<ChannelCallbacks>
	{
		Component (final BasicComponent facade, final Channel channel, final CallbackReactor callbackReactor, final ComponentCallbacks callbacks)
		{
			super ();
			Preconditions.checkNotNull (facade);
			Preconditions.checkNotNull (channel);
			Preconditions.checkNotNull (callbackReactor);
			this.facade = facade;
			this.monitor = Monitor.create (this.facade);
			synchronized (this.monitor) {
				this.transcript = Transcript.create (this.facade);
				this.channel = channel;
				this.channel.assign (this);
				this.callbackReactor = callbackReactor;
				this.callbackTrigger = this.callbackReactor.register (ComponentCallbacks.class, callbacks);
				this.inboundCalls = HashBiMap.create ();
				this.outboundCalls = HashBiMap.create ();
			}
		}
		
		@Override
		public final CallbackReference closed (final Channel channel)
		{
			Preconditions.checkState (channel == this.channel);
			synchronized (this.monitor) {
				this.callbackTrigger.terminated (this.facade);
				this.stop ();
			}
			return (null);
		}
		
		@Override
		public void deassigned (final ChannelCallbacks trigger, final ChannelCallbacks newCallbacks)
		{
			Preconditions.checkState (false);
		}
		
		@Override
		public final CallbackReference failed (final Channel channel, final Throwable exception)
		{
			Preconditions.checkState (channel == this.channel);
			synchronized (this.monitor) {
				this.callbackTrigger.failed (this.facade, exception);
				this.stop ();
			}
			return (null);
		}
		
		@Override
		public final CallbackReference opened (final Channel channel)
		{
			Preconditions.checkState (channel == this.channel);
			synchronized (this.monitor) {
				this.callbackTrigger.initialized (this.facade);
			}
			return (null);
		}
		
		@Override
		public void reassigned (final ChannelCallbacks trigger, final ChannelCallbacks oldCallbacks)
		{
			Preconditions.checkState (false);
		}
		
		@Override
		public final CallbackReference received (final Channel channel, final ChannelMessage message)
		{
			Preconditions.checkState (channel == this.channel);
			Preconditions.checkNotNull (message);
			synchronized (this.monitor) {
				switch (message.type) {
					case Exchange : {
						final Object actionValue = message.metaData.get (Token.Action.string);
						Preconditions.checkNotNull (actionValue, "missing action attribute");
						Preconditions.checkArgument (actionValue instanceof String, "invalid action attribute `%s`", actionValue);
						final Action action;
						if (Token.Call.string.equals (actionValue))
							action = Action.Call;
						else if (Token.Return.string.equals (actionValue))
							action = Action.Return;
						else if (Token.Cast.string.equals (actionValue))
							action = Action.Cast;
						else
							action = null;
						Preconditions.checkNotNull (action, "invalid action attribute `%s`", actionValue);
						switch (action) {
							case Call : {
								final Object correlationValue = message.metaData.get (Token.Correlation.string);
								Preconditions.checkNotNull (correlationValue, "missing correlation attribute");
								Preconditions.checkArgument (correlationValue instanceof String, "invalid correlation attribute `%s`", correlationValue);
								final String correlation = (String) correlationValue;
								final Object metaData = message.metaData.get (Token.MetaData.string);
								Preconditions.checkNotNull (metaData, "missing meta-data attribute");
								Preconditions.checkArgument (!this.inboundCalls.inverse ().containsKey (correlation), "coliding correlation attribute `%s`", correlation);
								final ComponentCallRequest request = ComponentCallRequest.create (metaData, message.data.asReadOnlyBuffer (), ComponentCallReference.create ());
								this.inboundCalls.put (request.reference, correlation);
								this.callbackTrigger.called (this.facade, request);
							}
								break;
							case Return : {
								final Object correlationValue = message.metaData.get (Token.Correlation.string);
								Preconditions.checkNotNull (correlationValue, "missing correlation attribute");
								Preconditions.checkArgument (correlationValue instanceof String, "invalid correlation attribute `%s`", correlationValue);
								final String correlation = (String) correlationValue;
								final Object metaData = message.metaData.get (Token.MetaData.string);
								Preconditions.checkNotNull (metaData, "missing meta-data attribute");
								Preconditions.checkArgument (this.outboundCalls.inverse ().containsKey (correlation), "mismatched correlation attribute `%s`", correlation);
								final ComponentCallReference reference = this.outboundCalls.inverse ().remove (correlation);
								final ComponentCallReply reply = ComponentCallReply.create (metaData, message.data.asReadOnlyBuffer (), reference);
								this.callbackTrigger.replied (this.facade, reply);
							}
								break;
							case Cast : {
								final Object metaData = message.metaData.get (Token.MetaData.string);
								Preconditions.checkNotNull (metaData, "missing meta-data attribute");
								final ComponentCastRequest request = ComponentCastRequest.create (metaData, message.data.asReadOnlyBuffer ());
								this.callbackTrigger.casted (this.facade, request);
							}
								break;
							default:
								Preconditions.checkState (false);
								break;
						}
					}
						break;
					default:
						Preconditions.checkState (false);
						break;
				}
			}
			return (null);
		}
		
		@Override
		public void registered (final ChannelCallbacks trigger)
		{}
		
		@Override
		public void unregistered (final ChannelCallbacks trigger)
		{
			this.callbackReactor.unregister (this.callbackTrigger);
		}
		
		@Override
		protected final void doStart ()
		{
			synchronized (this.monitor) {
				this.notifyStarted ();
			}
		}
		
		@Override
		protected final void doStop ()
		{
			synchronized (this.monitor) {
				this.notifyStopped ();
			}
		}
		
		final void assign (final ComponentCallbacks callbacks)
		{
			this.callbackReactor.assign (this.callbackTrigger, callbacks);
		}
		
		final void call (final ComponentIdentifier identifier, final ComponentCallRequest request)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (request);
			synchronized (this.monitor) {
				Preconditions.checkArgument (!this.outboundCalls.containsKey (request.reference));
				final String correlation = UUID.randomUUID ().toString ();
				final HashMap<String, Object> metaData = new HashMap<String, Object> ();
				metaData.put (Token.Action.string, Token.Call.string);
				metaData.put (Token.Component.string, identifier.string);
				metaData.put (Token.Correlation.string, correlation);
				metaData.put (Token.MetaData.string, request.metaData);
				final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, request.data);
				this.outboundCalls.put (request.reference, correlation);
				this.channel.send (message);
			}
		}
		
		final void cast (final ComponentIdentifier identifier, final ComponentCastRequest request)
		{
			Preconditions.checkNotNull (identifier);
			Preconditions.checkNotNull (request);
			synchronized (this.monitor) {
				final HashMap<String, Object> metaData = new HashMap<String, Object> ();
				metaData.put (Token.Action.string, Token.Cast.string);
				metaData.put (Token.Component.string, identifier.string);
				metaData.put (Token.MetaData.string, request.metaData);
				final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, request.data);
				this.channel.send (message);
			}
		}
		
		final void reply (final ComponentCallReply reply)
		{
			Preconditions.checkNotNull (reply);
			synchronized (this.monitor) {
				Preconditions.checkArgument (this.inboundCalls.containsKey (reply.reference));
				final String correlation = this.inboundCalls.remove (reply.reference);
				final HashMap<String, Object> metaData = new HashMap<String, Object> ();
				metaData.put (Token.Action.string, Token.Return.string);
				metaData.put (Token.Correlation.string, correlation);
				metaData.put (Token.MetaData.string, reply.metaData);
				final ChannelMessage message = ChannelMessage.create (ChannelMessageType.Exchange, metaData, reply.data.asReadOnlyBuffer ());
				this.channel.send (message);
			}
		}
		
		final CallbackReactor callbackReactor;
		final ComponentCallbacks callbackTrigger;
		final Channel channel;
		final BasicComponent facade;
		final HashBiMap<ComponentCallReference, String> inboundCalls;
		final Monitor monitor;
		final HashBiMap<ComponentCallReference, String> outboundCalls;
		final Transcript transcript;
	}
	
	private static enum Token
	{
		Action ("action"),
		Call ("call"),
		Cast ("cast"),
		Component ("component"),
		Correlation ("correlation"),
		MetaData ("meta-data"),
		Request ("request"),
		Return ("return");
		Token (final String string)
		{
			Preconditions.checkNotNull (string);
			this.string = string;
		}
		
		final String string;
	}
}
