/*
 * #%L
 * mosaic-tools-miscellaneous
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

package eu.mosaic_cloud.tools;


import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


public final class OutcomeFuture<_Outcome_ extends Object>
		extends FutureTask<_Outcome_>
{
	private OutcomeFuture (final OutcomeFuture.OutcomeCallable<_Outcome_> callable)
	{
		super (callable);
		this.trigger = new OutcomeTrigger<_Outcome_> (this);
		this.callable = callable;
	}
	
	public final OutcomeFuture.OutcomeTrigger<_Outcome_> trigger;
	final OutcomeFuture.OutcomeCallable<_Outcome_> callable;
	
	public static final <_Outcome_ extends Object> OutcomeFuture<_Outcome_> create ()
	{
		return (new OutcomeFuture<_Outcome_> (new OutcomeFuture.OutcomeCallable<_Outcome_> ()));
	}
	
	public static final class OutcomeTrigger<_Outcome_ extends Object>
			extends Object
	{
		OutcomeTrigger (final OutcomeFuture<_Outcome_> future)
		{
			super ();
			this.future = future;
		}
		
		public final void succeeded (final _Outcome_ outcome)
		{
			this.future.callable.outcome = outcome;
			this.future.run ();
		}
		
		private final OutcomeFuture<_Outcome_> future;
	}
	
	private static final class OutcomeCallable<_Outcome_ extends Object>
			extends Object
			implements
				Callable<_Outcome_>
	{
		OutcomeCallable ()
		{
			super ();
			this.outcome = null;
		}
		
		@Override
		public final _Outcome_ call ()
		{
			return (this.outcome);
		}
		
		_Outcome_ outcome;
	}
}
