
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;

public abstract class FsmCallbackAccess<Context> extends
        FsmAccess<Void, CallbackCompletion<Void>, Context> {

    public FsmCallbackAccess(Fsm<Context> fsm) {
        super(fsm);
    }

    protected abstract CallbackCompletion<Void> execute();

    @Override
    protected final CallbackCompletion<Void> execute(final Void input) {
        return (this.execute());
    }

    public final CallbackCompletion<Void> trigger() {
        return (this.trigger(null));
    }
}