
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;

public abstract class FsmAccess<tInput extends Object, tOutput extends Object, tContext> implements
        StateMachine.AccessorOperation<Fsm.Accessor, tInput, tOutput> {

    private final Fsm<tContext> fsm;

    public FsmAccess(Fsm<tContext> fsm) {
        super();
        this.fsm = fsm;
    }

    @Override
    public final tOutput execute(final Fsm.Accessor access, final tInput input) {
        return (this.execute(input));
    }

    protected abstract tOutput execute(tInput input);

    public final tOutput trigger(final tInput input) {
        return (this.fsm.execute(this, input));
    }
}