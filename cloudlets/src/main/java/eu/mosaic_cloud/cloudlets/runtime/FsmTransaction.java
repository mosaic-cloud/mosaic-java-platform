
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine.StateAndOutput;

public abstract class FsmTransaction<Input, Output, Context> implements
        StateMachine.TransactionOperation<Fsm.Transaction, FsmState, Input, Output> {

    protected final Cloudlet<Context> cloudlet;

    protected final FsmTransition transition;

    protected FsmTransaction(final Cloudlet<Context> cloudlet, final FsmTransition transition) {
        super();
        this.cloudlet = cloudlet;
        this.transition = transition;
    }

    @Override
    public final StateAndOutput<FsmState, Output> execute(final Fsm.Transaction transaction,
            final Input input) {
        return (this.execute(input));
    }

    protected abstract StateAndOutput<FsmState, Output> execute(Input input);

    public final Output trigger(final Input input) {
        return this.cloudlet.fsm.execute(this.transition, this, input);
    }
}