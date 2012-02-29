
package eu.mosaic_cloud.cloudlets.runtime;

public abstract class FsmVoidAccess<Context> extends FsmAccess<Void, Void, Context> {

    public FsmVoidAccess(Fsm<Context> fsm) {
        super(fsm);
    }

    protected abstract Void execute();

    @Override
    protected final Void execute(final Void input) {
        return (this.execute());
    }

    public final Void trigger() {
        return (this.trigger(null));
    }
}