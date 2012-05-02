/*
 * #%L
 * mosaic-tools-callbacks
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
// $codepro.audit.disable emptyCatchClause
// $codepro.audit.disable logExceptions

package eu.mosaic_cloud.tools.callbacks.core;


import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.mosaic_cloud.tools.threading.core.Joinable;

import com.google.common.base.Preconditions;


public final class CallbackFuture<_Outcome_ extends Object>
		extends Object
		implements
			Future<_Outcome_>,
			Joinable
{
	private CallbackFuture (final CallbackCompletion<_Outcome_> completion)
	{
		super ();
		Preconditions.checkArgument (completion != null);
		this.completion = completion;
	}
	
	@Override
	public final boolean await ()
	{
		return (this.await (-1));
	}
	
	@Override
	public final boolean await (final long timeout)
	{
		if (this.completion.backend == null)
			return (true);
		return (this.completion.backend.awaitCompletion (this.completion, timeout));
	}
	
	@Override
	public final boolean cancel (final boolean interrupt)
	{
		throw (new UnsupportedOperationException ());
	}
	
	@Override
	public final _Outcome_ get ()
			throws InterruptedException
	{
		while (true)
			try {
				return (this.get (Long.MAX_VALUE, TimeUnit.DAYS));
			} catch (final TimeoutException exception) {
				// NOTE: intentional
			}
	}
	
	@Override
	public _Outcome_ get (final long timeout, final TimeUnit unit)
			throws InterruptedException,
				TimeoutException
	{
		if (!this.await (unit.toMillis (timeout))) {
			if (Thread.interrupted ())
				throw (new InterruptedException ());
			throw (new TimeoutException ());
		}
		return (this.completion.getOutcome ());
	}
	
	@Override
	public final boolean isCancelled ()
	{
		return (false);
	}
	
	@Override
	public final boolean isDone ()
	{
		return (this.completion.isCompleted ());
	}
	
	public static final <_Outcome_ extends Object> CallbackFuture<_Outcome_> create (final CallbackCompletion<_Outcome_> completion)
	{
		return (new CallbackFuture<_Outcome_> (completion));
	}
	
	public final CallbackCompletion<_Outcome_> completion;
}
