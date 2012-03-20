/*
 * #%L
 * mosaic-cloudlets
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package eu.mosaic_cloud.cloudlets.implementation.cloudlet;

import eu.mosaic_cloud.cloudlets.core.CloudletState;
import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletFsm.FsmState;
import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletFsm.FsmTransition;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;

import com.google.common.base.Preconditions;

final class CloudletFsm extends StateMachine<FsmState, FsmTransition> {

    abstract class FsmAccess<Input extends Object, Output extends Object> implements
            StateMachine.AccessorOperation<CloudletFsm.Accessor, Input, Output> {

        FsmAccess() {
            super();
        }

        @Override
        public final Output execute(final CloudletFsm.Accessor access, final Input input) {
            return (this.execute(input));
        }

        abstract Output execute(Input input);

        final Output trigger(final Input input) {
            return (CloudletFsm.this.execute(this, input));
        }
    }

    abstract class FsmCallbackAccess extends FsmAccess<Void, CallbackCompletion<Void>> {

        FsmCallbackAccess() {
            super();
        }

        abstract CallbackCompletion<Void> execute();

        @Override
        final CallbackCompletion<Void> execute(final Void input) {
            return (this.execute());
        }

        final CallbackCompletion<Void> trigger() {
            return (this.trigger(null));
        }
    }

    abstract class FsmCallbackCompletionTransaction extends
            FsmTransaction<CallbackCompletion<Void>, Void> {

        final class Observer extends Object implements CallbackCompletionObserver, CallbackProxy,
                Runnable {

            final CallbackCompletion<Void> completion;

            Observer(final CallbackCompletion<Void> completion) {
                super();
                this.completion = completion;
            }

            @Override
            public final CallbackCompletion<Void> completed(final CallbackCompletion<?> completion1) {
                Preconditions.checkState(this.completion == completion1);
                return (CloudletFsm.this.cloudlet.isolate.enqueue(this));
            }

            @Override
            public final void run() {
                FsmCallbackCompletionTransaction.this.trigger(this.completion);
            }
        }

        FsmCallbackCompletionTransaction(final FsmTransition transition) {
            super(transition);
        }

        final void observe(final CallbackCompletion<Void> completion) {
            completion.observe(new Observer(completion));
        }
    }

    abstract class FsmCallbackTransaction extends FsmTransaction<Void, CallbackCompletion<Void>> {

        FsmCallbackTransaction(final FsmTransition transition) {
            super(transition);
        }

        abstract StateAndOutput<FsmState, CallbackCompletion<Void>> execute();

        @Override
        final StateAndOutput<FsmState, CallbackCompletion<Void>> execute(final Void input) {
            return (this.execute());
        }

        final CallbackCompletion<Void> trigger() {
            return (this.trigger(null));
        }
    }

    enum FsmState implements StateMachine.State {
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

        final CloudletState mapping;

        FsmState(final CloudletState mapping) {
            this.mapping = mapping;
        }

        CloudletState getCloudletState() {
            return this.mapping;
        }
    }

    abstract class FsmTransaction<Input, Output> implements
            StateMachine.TransactionOperation<CloudletFsm.Transaction, FsmState, Input, Output> {

        final FsmTransition transition;

        FsmTransaction(final FsmTransition transition) {
            super();
            this.transition = transition;
        }

        @Override
        public final StateAndOutput<FsmState, Output> execute(
                final CloudletFsm.Transaction transaction, final Input input) {
            return (this.execute(input));
        }

        abstract StateAndOutput<FsmState, Output> execute(Input input);

        final Output trigger(final Input input) {
            return CloudletFsm.this.execute(this.transition, this, input);
        }
    }

    enum FsmTransition implements StateMachine.Transition {
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

    abstract class FsmVoidAccess extends FsmAccess<Void, Void> {

        FsmVoidAccess() {
            super();
        }

        abstract Void execute();

        @Override
        final Void execute(final Void input) {
            return (this.execute());
        }

        final Void trigger() {
            return (this.trigger(null));
        }
    }

    abstract class FsmVoidTransaction extends FsmTransaction<Void, Void> {

        FsmVoidTransaction(final FsmTransition transition) {
            super(transition);
        }

        abstract StateAndOutput<FsmState, Void> execute();

        @Override
        final StateAndOutput<FsmState, Void> execute(final Void input) {
            return (this.execute());
        }

        final Void trigger() {
            return (this.trigger(null));
        }
    }

    final Cloudlet<?> cloudlet;

    CloudletFsm(final Cloudlet<?> cloudlet) {
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
        this.cloudlet = cloudlet;
    }
}
