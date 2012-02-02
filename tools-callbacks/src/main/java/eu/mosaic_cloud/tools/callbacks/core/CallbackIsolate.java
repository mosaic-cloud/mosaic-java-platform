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

package eu.mosaic_cloud.tools.callbacks.core;


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
	
	public final boolean destroy (final long timeout)
	{
		this.destroy ();
		return (this.await (timeout));
	}
	
	public final CallbackCompletion getCompletion ()
	{
		return (this.completion);
	}
	
	public final CallbackReactor getReactor ()
	{
		final CallbackReactor reactor = this.reactorReference.get ();
		Preconditions.checkState (reactor != null);
		return (reactor);
	}
	
	private final CallbackCompletion completion;
	private final Reference<? extends CallbackReactor> reactorReference;
	
	public static final CallbackIsolate create (final Reference<? extends CallbackReactor> reactor, final CallbackCompletion completion)
	{
		return (new CallbackIsolate (reactor, completion));
	}
}
