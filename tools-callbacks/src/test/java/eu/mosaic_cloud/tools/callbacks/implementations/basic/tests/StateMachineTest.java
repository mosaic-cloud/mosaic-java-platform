
package eu.mosaic_cloud.tools.callbacks.implementations.basic.tests;


import eu.mosaic_cloud.tools.callbacks.tools.StateMachine;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine.AccessorOperation;
import eu.mosaic_cloud.tools.callbacks.tools.StateMachine.TransactionOperation;

import org.junit.Test;


public final class StateMachineTest
{
	@Test
	public final void test ()
	{
		final TestStateMachine machine = new TestStateMachine ();
		{
			machine.new Accessor ().begin ().release ();
		}
		{
			machine.new Accessor ().execute (new AccessorOperation<TestStateMachine.Accessor> () {
				@Override
				public final void execute (final TestStateMachine.Accessor accessor)
				{}
			});
		}
		{
			machine.new Transaction (TestStateMachine.Transition.Executing).begin ().commit (TestStateMachine.State.Ready);
		}
		{
			machine.new Transaction (TestStateMachine.Transition.Executing).execute (new TransactionOperation<TestStateMachine.State, TestStateMachine.Transaction> () {
				@Override
				public final TestStateMachine.State execute (final TestStateMachine.Transaction transaction)
				{
					return (TestStateMachine.State.Ready);
				}
			});
		}
		{
			machine.new Transaction (TestStateMachine.Transition.Executing).begin ().rollback ();
		}
		{
			machine.new Transaction (TestStateMachine.Transition.Executing).execute (new TransactionOperation<TestStateMachine.State, TestStateMachine.Transaction> () {
				@Override
				public final TestStateMachine.State execute (final TestStateMachine.Transaction transaction)
				{
					return (null);
				}
			});
		}
		/*{
			try (final TestStateMachine.Transaction transaction = machine.new Transaction ()) {
				transaction.commit (TestStateMachine.State.Created);
			}
		}*/
	}
	
	public static final class TestStateMachine
			extends StateMachine<TestStateMachine.State, TestStateMachine.Transition>
	{
		TestStateMachine ()
		{
			super (State.class, Transition.class);
			this.defineStates (TestStateMachine.State.class);
			this.defineTransition (Transition.Executing, State.Ready, State.Ready);
			this.initialize (State.Ready);
		}
		
		public enum State
				implements
					StateMachine.State
		{
			Ready ();
		}
		
		public enum Transition
				implements
					StateMachine.Transition
		{
			Executing ();
		}
	}
}
