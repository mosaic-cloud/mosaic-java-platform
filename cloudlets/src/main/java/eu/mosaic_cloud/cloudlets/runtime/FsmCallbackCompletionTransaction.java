package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;

import com.google.common.base.Preconditions;

public abstract class FsmCallbackCompletionTransaction<Context> extends
        FsmTransaction<CallbackCompletion<Void>, Void, Context> {


    private final class Observer extends Object implements CallbackCompletionObserver,
            CallbackProxy, Runnable {

        final CallbackCompletion<Void> completion;

        Observer(final CallbackCompletion<Void> completion) {
            super();
            this.completion = completion;
        }

        @Override
        public final CallbackCompletion<Void> completed(final CallbackCompletion<?> completion1) {
            Preconditions.checkState(this.completion == completion1);
            return (cloudlet.isolate.enqueue(this));
        }

        @Override
        public final void run() {
            trigger(this.completion);
        }
    }

    protected FsmCallbackCompletionTransaction(final Cloudlet<Context> cloudlet,final FsmTransition transition) {
        super(cloudlet, transition);
    }

    final void observe(final CallbackCompletion<Void> completion) {
        completion.observe(new Observer(completion));
    }
}