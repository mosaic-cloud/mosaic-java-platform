
package eu.mosaic_cloud.tools.callbacks.core;


public final class CallbackBypass<_Outcome_ extends Object>
		extends Object
{
	private CallbackBypass (final _Outcome_ outcome)
	{
		super ();
		this.outcome = outcome;
	}
	
	public final _Outcome_ outcome;
	
	public static final <_Outcome_ extends Object> CallbackBypass<_Outcome_> create (final _Outcome_ outcome)
	{
		return (new CallbackBypass<_Outcome_> (outcome));
	}
}
