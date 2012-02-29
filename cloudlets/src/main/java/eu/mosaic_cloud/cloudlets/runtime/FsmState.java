
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.cloudlets.core.CloudletState;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;

/**
 * Possible states of the finite state machine associated with the cloudlet. One
 * or more of this states can be mapped to the same cloudlet state (see
 * {@link CloudletState}).
 * 
 * @author Ciprian Craciun
 * 
 */
public enum FsmState implements StateMachine.State {
    Active(CloudletState.ACTIVE),
    CallbacksDestroyFailedPending(CloudletState.DESTROYING),
    CallbacksDestroyPending(CloudletState.DESTROYING),
    CallbacksDestroySucceededPending(CloudletState.DESTROYING),
    CallbacksInitializeFailedPending(CloudletState.INITIALIZING),
    CallbacksInitializePending(CloudletState.INITIALIZING),
    CallbacksInitializeSucceededPending(CloudletState.INITIALIZING),
    CallbacksRegisterPending(CloudletState.INITIALIZING),
    CallbacksUnregisterPending(CloudletState.DESTROYING),
    ControllerRegisterPending(CloudletState.INITIALIZING),
    ControllerUnregisterPending(CloudletState.DESTROYING),
    Created(CloudletState.CREATED),
    Destroyed(CloudletState.DESTROYED),
    Failed(CloudletState.FAILED);

    private final CloudletState mapping;

    FsmState(final CloudletState mapping) {
        this.mapping = mapping;
    }

    public CloudletState getCloudletState() {
        return this.mapping;
    }
}