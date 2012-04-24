
package eu.mosaic_cloud.tools.callbacks.tools;


import java.util.ArrayDeque;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;

import com.google.common.base.Preconditions;


public class CallbackCompletionWorkflows
{
	public static final CallbackCompletion<Void> executeSequence (final Callable<CallbackCompletion<Void>> ... operations_)
	{
		Preconditions.checkNotNull (operations_);
		final ArrayDeque<Callable<CallbackCompletion<Void>>> operations = new ArrayDeque<Callable<CallbackCompletion<Void>>> (operations_.length);
		for (final Callable<CallbackCompletion<Void>> operation : operations_) {
			Preconditions.checkNotNull (operation);
			operations.add (operation);
		}
		final CallbackCompletionDeferredFuture<Void> future = CallbackCompletionDeferredFuture.create (Void.class);
		final Runnable chainer = new Runnable () {
			@Override
			public void run ()
			{
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
					} else
						return;
				}
				completion.observe (new CallbackCompletionObserver () {
					@Override
					public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
					{
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
