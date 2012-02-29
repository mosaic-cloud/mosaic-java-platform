
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine.StateAndOutput;

public abstract class FsmCallbackTransaction<Context> extends
        FsmTransaction<Void, CallbackCompletion<Void>, Context> {

    public FsmCallbackTransaction(final Cloudlet<Context> cloudlet, final FsmTransition transition) {
        super(cloudlet, transition);
    }

    protected abstract StateAndOutput<FsmState, CallbackCompletion<Void>> execute();

    @Override
    protected final StateAndOutput<FsmState, CallbackCompletion<Void>> execute(final Void input) {
        return (this.execute());
    }

    final CallbackCompletion<Void> trigger() {
        return (this.trigger(null));
    }
}