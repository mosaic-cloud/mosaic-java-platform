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

package eu.mosaic_cloud.tools.callbacks.tools;


import java.util.ArrayDeque;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;

import com.google.common.base.Preconditions;


public class CallbackCompletionWorkflows
{
	public static final CallbackCompletion<Void> executeSequence (final Callable<CallbackCompletion<Void>> ... operations_) {
		Preconditions.checkNotNull (operations_);
		final ArrayDeque<Callable<CallbackCompletion<Void>>> operations = new ArrayDeque<Callable<CallbackCompletion<Void>>> (operations_.length);
		for (final Callable<CallbackCompletion<Void>> operation : operations_) {
			Preconditions.checkNotNull (operation);
			operations.add (operation);
		}
		final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture.create (Void.class);
		final Runnable chainer = new Runnable () {
			@Override
			public void run () {
				final Runnable chainer = this;
				if (operations.isEmpty ()) {
					future.trigger.triggerSucceeded (null);
					return;
				}
				final CallbackCompletion<Void> completion;
				try {
					final Callable<CallbackCompletion<Void>> operation = operations.removeFirst ();
					completion = operation.call ();
				} catch (final Throwable exception) {
					// FIXME: log...
					future.trigger.triggerFailed (exception);
					return;
				}
				if (completion.isCompleted ()) {
					final Throwable exception = completion.getException ();
					if (exception != null) {
						future.trigger.triggerFailed (exception);
						return;
					} else {
						chainer.run ();
						return;
					}
				}
				completion.observe (new CallbackCompletionObserver () {
					@Override
					public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_) {
						Preconditions.checkArgument (completion_ == completion);
						final Throwable exception = completion.getException ();
						if (exception != null)
							future.trigger.triggerFailed (exception);
						else
							chainer.run ();
						return (null);
					}
				});
			}
		};
		chainer.run ();
		return (future.completion);
	}
}
