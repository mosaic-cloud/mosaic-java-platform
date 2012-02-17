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

package eu.mosaic_cloud.tools.miscellaneous;


import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;


public final class DeferredFuture<_Outcome_ extends Object>
		extends AbstractFuture<_Outcome_>
{
	private DeferredFuture (final Class<_Outcome_> outcomeClass)
	{
		super ();
		this.outcomeClass = outcomeClass;
		this.trigger = new Trigger<_Outcome_> (this);
	}
	
	@Override
	protected final void interruptTask ()
	{
		throw (new UnsupportedOperationException ());
	}
	
	final void triggerFailed (final Throwable exception)
	{
		Preconditions.checkNotNull (exception);
		this.setException (exception);
	}
	
	final void triggerSucceeded (final _Outcome_ outcome)
	{
		this.set (this.outcomeClass.cast (outcome));
	}
	
	public final Class<_Outcome_> outcomeClass;
	public final Trigger<_Outcome_> trigger;
	
	public static final <_Outcome_ extends Object> DeferredFuture<_Outcome_> create (final Class<_Outcome_> outcomeClass)
	{
		return (new DeferredFuture<_Outcome_> (outcomeClass));
	}
	
	public static final class Trigger<_Outcome_ extends Object>
			extends Object
	{
		Trigger (final DeferredFuture<_Outcome_> future)
		{
			super ();
			this.future = future;
		}
		
		public final void triggerFailed (final Throwable exception)
		{
			this.future.triggerFailed (exception);
		}
		
		public final void triggerSucceeded (final _Outcome_ outcome)
		{
			this.future.triggerSucceeded (outcome);
		}
		
		private final DeferredFuture<_Outcome_> future;
	}
}
