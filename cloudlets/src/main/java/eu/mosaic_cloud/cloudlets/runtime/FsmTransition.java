
package eu.mosaic_cloud.cloudlets.runtime;

import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;

/**
 * Possible transitions of the finite state machine associated with the
 * cloudlet.
 * 
 * @author Ciprian Craciun
 * 
 */
public enum FsmTransition implements StateMachine.Transition {
    CallbacksDestroyCompleted,
    CallbacksDestroyFailedCompleted,
    CallbacksDestroySucceededCompleted,
    CallbacksInitializeCompleted,
    CallbacksInitializeFailedCompleted,
    CallbacksInitializeSucceededCompleted,
    CallbacksRegisterCompleted,
    CallbacksUnregisterCompleted,
    ControllerRegisterCompleted,
    ControllerUnregisterCompleted,
    ExternalDestroy,
    ExternalInitialize,
    InternalFailure;
}