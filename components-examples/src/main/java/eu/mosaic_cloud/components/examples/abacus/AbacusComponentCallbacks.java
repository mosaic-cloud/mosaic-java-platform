
package eu.mosaic_cloud.components.examples.abacus;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import eu.mosaic_cloud.callbacks.core.CallbackHandler;
import eu.mosaic_cloud.callbacks.core.CallbackReference;
import eu.mosaic_cloud.components.core.Component;
import eu.mosaic_cloud.components.core.ComponentCallReply;
import eu.mosaic_cloud.components.core.ComponentCallRequest;
import eu.mosaic_cloud.components.core.ComponentCallbacks;
import eu.mosaic_cloud.components.core.ComponentCastRequest;
import eu.mosaic_cloud.json.tools.DefaultJsonMapper;
import eu.mosaic_cloud.transcript.core.Transcript;
import eu.mosaic_cloud.transcript.tools.TranscriptExceptionTracer;


public final class AbacusComponentCallbacks
		extends Object
		implements
			ComponentCallbacks,
			CallbackHandler<ComponentCallbacks>
{
	public AbacusComponentCallbacks ()
	{
		super ();
		this.transcript = Transcript.create (this);
		this.exceptions = TranscriptExceptionTracer.create (this.transcript);
		this.component = null;
		this.status = Status.Created;
	}
	
	@Override
	public final CallbackReference called (final Component component, final ComponentCallRequest request)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		Preconditions.checkArgument (request.data.remaining () == 0);
		final RequestMetaData requestMetaData;
		try {
			requestMetaData = DefaultJsonMapper.defaultInstance.decode (request.metaData, RequestMetaData.class);
			Preconditions.checkNotNull (requestMetaData);
		} catch (final Throwable exception) {
			this.exceptions.traceIgnoredException (exception);
			return (null);
		}
		this.transcript.traceInformation ("called `%s` with `%s`", requestMetaData.operator, Joiner.on ("`, `").join (requestMetaData.operands));
		double outcome;
		if ("+".equals (requestMetaData.operator)) {
			outcome = 0;
			for (final Number operand : requestMetaData.operands)
				outcome += operand.doubleValue ();
		} else
			throw (new IllegalArgumentException ());
		final ReplyMetaData replyMetaData = new ReplyMetaData (outcome);
		final ComponentCallReply reply;
		try {
			reply = ComponentCallReply.create (DefaultJsonMapper.defaultInstance.encode (replyMetaData, ReplyMetaData.class), ByteBuffer.allocate (0), request.reference);
		} catch (final Throwable exception) {
			this.exceptions.traceIgnoredException (exception);
			return (null);
		}
		component.reply (reply);
		return (null);
	}
	
	@Override
	public final CallbackReference casted (final Component component, final ComponentCastRequest request)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final void deassigned (final ComponentCallbacks trigger, final ComponentCallbacks newCallbacks)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public final CallbackReference failed (final Component component, final Throwable exception)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final CallbackReference initialized (final Component component)
	{
		Preconditions.checkState (this.status == Status.Registered);
		Preconditions.checkState (this.component == null);
		this.component = component;
		this.status = Status.Initialized;
		this.transcript.traceInformation ("initialized;");
		return (null);
	}
	
	@Override
	public final void reassigned (final ComponentCallbacks trigger, final ComponentCallbacks oldCallbacks)
	{
		throw (new IllegalStateException ());
	}
	
	@Override
	public final void registered (final ComponentCallbacks trigger)
	{
		Preconditions.checkState (this.status == Status.Created);
		Preconditions.checkState (this.component == null);
		this.status = Status.Registered;
	}
	
	@Override
	public final CallbackReference replied (final Component component, final ComponentCallReply reply)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final CallbackReference terminated (final Component component)
	{
		Preconditions.checkState (this.status == Status.Initialized);
		Preconditions.checkState (this.component == component);
		this.component = null;
		this.status = Status.Terminated;
		this.transcript.traceInformation ("terminated;");
		return (null);
	}
	
	@Override
	public final void unregistered (final ComponentCallbacks trigger)
	{
		Preconditions.checkState ((this.status == Status.Registered) || (this.status == Status.Initialized) || (this.status == Status.Terminated));
		this.component = null;
		this.status = Status.Unregistered;
	}
	
	private Component component;
	private final TranscriptExceptionTracer exceptions;
	private Status status;
	private final Transcript transcript;
	
	public static final class ReplyMetaData
			extends Object
	{
		public ReplyMetaData ()
		{
			super ();
		}
		
		public ReplyMetaData (final double value)
		{
			super ();
			this.ok = Boolean.TRUE;
			this.outcome = Double.valueOf (value);
		}
		
		public Boolean ok;
		public Number outcome;
	}
	
	public static final class RequestMetaData
			extends Object
	{
		public RequestMetaData ()
		{
			super ();
		}
		
		public RequestMetaData (final String operator, final double ... operands)
		{
			super ();
			this.operator = operator;
			this.operands = new ArrayList<Number> (operands.length);
			for (final double operand : operands)
				this.operands.add (Double.valueOf (operand));
		}
		
		public List<Number> operands;
		public String operator;
	}
	
	private static enum Status
	{
		Created,
		Initialized,
		Registered,
		Terminated,
		Unregistered;
	}
}
