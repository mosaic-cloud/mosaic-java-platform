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

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import eu.mosaic_cloud.cloudlets.core.CloudletState;
import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletFsm.FsmState;
import eu.mosaic_cloud.cloudlets.implementation.cloudlet.CloudletFsm.FsmTransition;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;
import eu.mosaic_cloud.tools.callbacks.core.CallbackProxy;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

final class CloudletFsm extends StateMachine<FsmState, FsmTransition> {

    protected abstract class FsmAccess<Input extends Object, Output extends Object>
            implements StateMachine.AccessorOperation<Accessor, Input, Output> {

        protected FsmAccess() {
            super();
        }

        @Override
        public final Output execute(final Accessor access, final Input input) {
            return (this.execute(input));
        }

        protected abstract Output execute(Input input);

        protected final Output trigger(final Input input) {
            return (CloudletFsm.this.execute(this, input));
        }
    }

    protected abstract class FsmCallbackAccess extends
            FsmAccess<Void, CallbackCompletion<Void>> {

        protected FsmCallbackAccess() {
            super();
        }

        protected abstract CallbackCompletion<Void> execute();

        @Override
        protected final CallbackCompletion<Void> execute(final Void input) {
            return (this.execute());
        }

        protected final CallbackCompletion<Void> trigger() {
            return (this.trigger(null));
        }
    }

    protected abstract class FsmCallbackCompletionTransaction extends
            FsmTransaction<CallbackCompletion<Void>, Void> {

        protected final class Observer extends Object implements
                CallbackCompletionObserver, CallbackProxy, Runnable {

            protected final CallbackCompletion<Void> completion;

            protected Observer(final CallbackCompletion<Void> completion) {
                super();
                this.completion = completion;
            }

            @Override
            public final CallbackCompletion<Void> completed(
                    final CallbackCompletion<?> completion1) {
                Preconditions.checkState(this.completion == completion1);
                return (CloudletFsm.this.cloudlet.isolate.enqueue(this));
            }

            @Override
            public final void run() {
                FsmCallbackCompletionTransaction.this.trigger(this.completion);
            }
        }

        protected FsmCallbackCompletionTransaction(
                final FsmTransition transition) {
            super(transition);
        }

        protected final void observe(final CallbackCompletion<Void> completion) {
            completion.observe(new Observer(completion));
        }
    }

    protected abstract class FsmCallbackTransaction extends
            FsmTransaction<Void, CallbackCompletion<Void>> {

        protected FsmCallbackTransaction(final FsmTransition transition) {
            super(transition);
        }

        protected abstract StateAndOutput<FsmState, CallbackCompletion<Void>> execute();

        @Override
        protected final StateAndOutput<FsmState, CallbackCompletion<Void>> execute(
                final Void input) {
            return (this.execute());
        }

        protected final CallbackCompletion<Void> trigger() {
            return (this.trigger(null));
        }
    }

    protected abstract class FsmFutureCompletionAccess<Outcome> extends
            FsmAccess<Future<Outcome>, Void> {

        protected final class Observer extends Object implements Executor,
                Runnable {

            protected final Future<Outcome> completion;

            protected Observer(final Future<Outcome> completion) {
                super();
                this.completion = completion;
            }

            @Override
            public final void execute(final Runnable runnable) {
                Preconditions.checkArgument(runnable == this);
                CloudletFsm.this.cloudlet.isolate.enqueue(runnable);
            }

            @Override
            public final void run() {
                FsmFutureCompletionAccess.this.trigger(this.completion);
            }
        }

        protected FsmFutureCompletionAccess() {
            super();
        }

        protected final void observe(final ListenableFuture<Outcome> completion) {
            final Observer observer = new Observer(completion);
            completion.addListener(observer, observer);
        }
    }

    protected abstract class FsmFutureCompletionTransaction<Outcome> extends
            FsmTransaction<Future<Outcome>, Void> {

        protected final class Observer extends Object implements Executor,
                Runnable {

            protected final Future<Outcome> completion;

            protected Observer(final Future<Outcome> completion) {
                super();
                this.completion = completion;
            }

            @Override
            public final void execute(final Runnable runnable) {
                Preconditions.checkArgument(runnable == this);
                CloudletFsm.this.cloudlet.isolate.enqueue(runnable);
            }

            @Override
            public final void run() {
                FsmFutureCompletionTransaction.this.trigger(this.completion);
            }
        }

        protected FsmFutureCompletionTransaction(final FsmTransition transition) {
            super(transition);
        }

        protected final void observe(final ListenableFuture<Outcome> completion) {
            final Observer observer = new Observer(completion);
            completion.addListener(observer, observer);
        }
    }

    protected enum FsmState implements StateMachine.State {
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
        CreatePending(CloudletState.CREATED),
        Destroyed(CloudletState.DESTROYED),
        Failed(CloudletState.FAILED);

        private final CloudletState mapping;

        private FsmState(final CloudletState mapping) {
            this.mapping = mapping;
        }

        protected CloudletState getCloudletState() {
            return this.mapping;
        }
    }

    protected abstract class FsmTransaction<Input, Output>
            implements
            StateMachine.TransactionOperation<Transaction, FsmState, Input, Output> {

        protected final FsmTransition transition;

        protected FsmTransaction(final FsmTransition transition) {
            super();
            this.transition = transition;
        }

        protected abstract StateAndOutput<FsmState, Output> execute(Input input);

        @Override
        public final StateAndOutput<FsmState, Output> execute(
                final Transaction transaction, final Input input) {
            return (this.execute(input));
        }

        protected final Output trigger(final Input input) {
            return CloudletFsm.this.execute(this.transition, this, input);
        }
    }

    protected enum FsmTransition implements StateMachine.Transition {
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
        CreateCompleted,
        ExternalDestroy,
        ExternalInitialize,
        InternalFailure;
    }

    protected abstract class FsmVoidAccess extends FsmAccess<Void, Void> {

        protected FsmVoidAccess() {
            super();
        }

        protected abstract Void execute();

        @Override
        protected final Void execute(final Void input) {
            return (this.execute());
        }

        protected final Void trigger() {
            return (this.trigger(null));
        }
    }

    protected abstract class FsmVoidTransaction extends
            FsmTransaction<Void, Void> {

        protected FsmVoidTransaction(final FsmTransition transition) {
            super(transition);
        }

        protected abstract StateAndOutput<FsmState, Void> execute();

        @Override
        protected final StateAndOutput<FsmState, Void> execute(final Void input) {
            return (this.execute());
        }

        protected final Void trigger() {
            return (this.trigger(null));
        }
    }

    protected final Cloudlet<?> cloudlet;

    protected CloudletFsm(final Cloudlet<?> cloudlet) {
        super(FsmState.class, FsmTransition.class);
        this.defineStates(FsmState.class);
        this.defineTransition(FsmTransition.CreateCompleted,
                FsmState.CreatePending, FsmState.Created);
        this.defineTransition(FsmTransition.ExternalInitialize,
                FsmState.Created, FsmState.ControllerRegisterPending);
        this.defineTransition(FsmTransition.ControllerRegisterCompleted,
                FsmState.ControllerRegisterPending, new FsmState[] {
                        FsmState.CallbacksRegisterPending, FsmState.Failed });
        this.defineTransition(FsmTransition.CallbacksRegisterCompleted,
                FsmState.CallbacksRegisterPending, new FsmState[] {
                        FsmState.CallbacksInitializePending,
                        FsmState.ControllerUnregisterPending });
        this.defineTransition(FsmTransition.CallbacksInitializeCompleted,
                FsmState.CallbacksInitializePending, new FsmState[] {
                        FsmState.CallbacksInitializeSucceededPending,
                        FsmState.CallbacksInitializeFailedPending });
        this.defineTransition(
                FsmTransition.CallbacksInitializeSucceededCompleted,
                FsmState.CallbacksInitializeSucceededPending, new FsmState[] {
                        FsmState.Active, FsmState.CallbacksUnregisterPending });
        this.defineTransition(FsmTransition.CallbacksInitializeFailedCompleted,
                FsmState.CallbacksInitializeFailedPending,
                FsmState.CallbacksDestroyPending);
        this.defineTransition(FsmTransition.ExternalDestroy, FsmState.Active,
                FsmState.CallbacksDestroyPending);
        this.defineTransition(FsmTransition.CallbacksDestroyCompleted,
                FsmState.CallbacksDestroyPending, new FsmState[] {
                        FsmState.CallbacksDestroySucceededPending,
                        FsmState.CallbacksDestroyFailedPending });
        this.defineTransition(FsmTransition.CallbacksDestroySucceededCompleted,
                FsmState.CallbacksDestroySucceededPending,
                FsmState.CallbacksUnregisterPending);
        this.defineTransition(FsmTransition.CallbacksDestroyFailedCompleted,
                FsmState.CallbacksDestroyFailedPending,
                FsmState.CallbacksUnregisterPending);
        this.defineTransition(FsmTransition.CallbacksUnregisterCompleted,
                FsmState.CallbacksUnregisterPending,
                FsmState.ControllerUnregisterPending);
        this.defineTransition(FsmTransition.ControllerUnregisterCompleted,
                FsmState.ControllerUnregisterPending, new FsmState[] {
                        FsmState.Destroyed, FsmState.Failed });
        this.defineTransition(FsmTransition.InternalFailure, FsmState.values(),
                new FsmState[] {
                    FsmState.Failed });
        this.initialize(FsmState.CreatePending);
        this.cloudlet = cloudlet;
    }
}
