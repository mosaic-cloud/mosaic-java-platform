
package eu.mosaic_cloud.tools.callbacks.tools;


import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;


public final class CallbackCompletionTrigger<_Outcome_ extends Object>
			extends Object
{
	CallbackCompletionTrigger (final CallbackCompletionTriggerBackend<_Outcome_> backend) {
		super ();
		this.backend = backend;
		this.completion = this.backend.completion;
	}
	
	public final void triggerFailed (final Throwable exception) {
		this.backend.triggerFailed (exception);
	}
	
	public final void triggerSucceeded (final _Outcome_ outcome) {
		this.backend.triggerSucceeded (outcome);
	}
	
	public final CallbackCompletion<_Outcome_> completion;
	private final CallbackCompletionTriggerBackend<_Outcome_> backend;
	
	public static final <_Outcome_ extends Object> CallbackCompletionTrigger<_Outcome_> create () {
		return (CallbackCompletionTriggerBackend.createTrigger ());
	}
}
