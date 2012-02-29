
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;

public final class Fsm<Context> extends StateMachine<FsmState, FsmTransition> {

    public Fsm() {
        super(FsmState.class, FsmTransition.class);

        this.defineStates(FsmState.class);

        this.defineTransition(FsmTransition.ExternalInitialize, FsmState.Created,
                FsmState.ControllerRegisterPending);
        this.defineTransition(FsmTransition.ControllerRegisterCompleted,
                FsmState.ControllerRegisterPending, new FsmState[] {
                        FsmState.CallbacksRegisterPending, FsmState.Failed });
        this.defineTransition(FsmTransition.CallbacksRegisterCompleted,
                FsmState.CallbacksRegisterPending, new FsmState[] {
                        FsmState.CallbacksInitializePending, FsmState.ControllerUnregisterPending });
        this.defineTransition(FsmTransition.CallbacksInitializeCompleted,
                FsmState.CallbacksInitializePending, new FsmState[] {
                        FsmState.CallbacksInitializeSucceededPending,
                        FsmState.CallbacksInitializeFailedPending });
        this.defineTransition(FsmTransition.CallbacksInitializeSucceededCompleted,
                FsmState.CallbacksInitializeSucceededPending, new FsmState[] {
                        FsmState.Active, FsmState.CallbacksUnregisterPending });
        this.defineTransition(FsmTransition.CallbacksInitializeFailedCompleted,
                FsmState.CallbacksInitializeFailedPending, FsmState.CallbacksDestroyPending);
        this.defineTransition(FsmTransition.ExternalDestroy, FsmState.Active,
                FsmState.CallbacksDestroyPending);
        this.defineTransition(FsmTransition.CallbacksDestroyCompleted,
                FsmState.CallbacksDestroyPending, new FsmState[] {
                        FsmState.CallbacksDestroySucceededPending,
                        FsmState.CallbacksDestroyFailedPending });
        this.defineTransition(FsmTransition.CallbacksDestroySucceededCompleted,
                FsmState.CallbacksDestroySucceededPending, FsmState.CallbacksUnregisterPending);
        this.defineTransition(FsmTransition.CallbacksDestroyFailedCompleted,
                FsmState.CallbacksDestroyFailedPending, FsmState.CallbacksUnregisterPending);
        this.defineTransition(FsmTransition.CallbacksUnregisterCompleted,
                FsmState.CallbacksUnregisterPending, FsmState.ControllerUnregisterPending);
        this.defineTransition(FsmTransition.ControllerUnregisterCompleted,
                FsmState.ControllerUnregisterPending, new FsmState[] {
                        FsmState.Destroyed, FsmState.Failed });
        this.defineTransition(FsmTransition.InternalFailure, FsmState.values(), new FsmState[] {
            FsmState.Failed });

        this.initialize(FsmState.Created);
    }
}