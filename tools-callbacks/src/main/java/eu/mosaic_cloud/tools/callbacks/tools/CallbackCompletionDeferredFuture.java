
package eu.mosaic_cloud.tools.callbacks.tools;


import com.google.common.util.concurrent.ForwardingFuture;
import com.google.common.util.concurrent.ListenableFuture;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;


public final class CallbackCompletionDeferredFuture<_Outcome_ extends Object>
		extends ForwardingFuture<_Outcome_>
{
	private CallbackCompletionDeferredFuture (final Class<_Outcome_> outcomeClass)
	{
		super ();
		this.future = DeferredFuture.create (outcomeClass);
		this.trigger = this.future.trigger;
		this.completion = CallbackCompletion.createDeferred (this.future);
	}
	
	@Override
	protected ListenableFuture<_Outcome_> delegate ()
	{
		return (this.future);
	}
	
	public final CallbackCompletion<_Outcome_> completion;
	public final DeferredFuture<_Outcome_> future;
	public final DeferredFuture.Trigger<_Outcome_> trigger;
	
	public static final <_Outcome_ extends Object> CallbackCompletionDeferredFuture<_Outcome_> create (final Class<_Outcome_> outcomeClass)
	{
		return (new CallbackCompletionDeferredFuture<_Outcome_> (outcomeClass));
	}
}
