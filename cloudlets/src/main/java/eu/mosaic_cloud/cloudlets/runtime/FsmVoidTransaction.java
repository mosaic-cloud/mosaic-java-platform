
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.tools.StateMachine.StateAndOutput;

public abstract class FsmVoidTransaction<Context> extends FsmTransaction<Void, Void, Context> {

    public FsmVoidTransaction(final Cloudlet<Context> cloudlet, final FsmTransition transition) {
        super(cloudlet, transition);
    }

    protected abstract StateAndOutput<FsmState, Void> execute();

    @Override
    protected final StateAndOutput<FsmState, Void> execute(final Void input) {
        return (this.execute());
    }

    public final Void trigger() {
        return (this.trigger(null));
    }
}