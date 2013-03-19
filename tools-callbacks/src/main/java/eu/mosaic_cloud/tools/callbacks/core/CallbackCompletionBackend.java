/*
 * #%L
 * mosaic-tools-callbacks
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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


public interface CallbackCompletionBackend
{
	public abstract boolean awaitCompletion (final CallbackCompletion<?> completion, final long timeout);
	
	public abstract Throwable getCompletionException (final CallbackCompletion<?> completion);
	
	public abstract Object getCompletionOutcome (final CallbackCompletion<?> completion);
	
	public abstract CallbackReactor getReactor ();
	
	public abstract void observeCompletion (final CallbackCompletion<?> completion, final CallbackCompletionObserver observer);
}
