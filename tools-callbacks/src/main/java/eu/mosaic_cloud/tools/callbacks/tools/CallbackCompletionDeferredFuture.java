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

package eu.mosaic_cloud.tools.callbacks.tools;


import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.miscellaneous.DeferredFuture;

import com.google.common.util.concurrent.ForwardingFuture;
import com.google.common.util.concurrent.ListenableFuture;


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
