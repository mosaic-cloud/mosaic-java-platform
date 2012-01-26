
package eu.mosaic_cloud.tools.callbacks.core.v2;


import java.lang.ref.Reference;

import com.google.common.base.Preconditions;
import eu.mosaic_cloud.tools.threading.core.Joinable;
import eu.mosaic_cloud.tools.threading.tools.Threading;


public final class CallbackIsolate
		extends Object
		implements
			Joinable
{
	private CallbackIsolate (final Reference<? extends CallbackReactor> reactor, final CallbackCompletion completion)
	{
		super ();
		Preconditions.checkNotNull (reactor);
		Preconditions.checkNotNull (completion);
		this.reactorReference = reactor;
		this.completion = completion;
	}
	
	@Override
	public final boolean await ()
	{
		return (Threading.awaitOrCatch (this.completion, null, null) == Boolean.TRUE);
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		return (Threading.awaitOrCatch (this.completion, timeout, null, null) == Boolean.TRUE);
	}
	
	public final CallbackReference destroy ()
	{
		return (this.getReactor ().destroyIsolate (this));
	}
	
	public final CallbackReactor getReactor ()
	{
		final CallbackReactor reactor = this.reactorReference.get ();
		Preconditions.checkState (reactor != null);
		return (reactor);
	}
	
	public final CallbackCompletion getCompletion ()
	{
		return (this.completion);
	}
	
	private final Reference<? extends CallbackReactor> reactorReference;
	private final CallbackCompletion completion;
	
	public static final CallbackIsolate create (final Reference<? extends CallbackReactor> reactor, final CallbackCompletion completion)
	{
		return (new CallbackIsolate (reactor, completion));
	}
}
