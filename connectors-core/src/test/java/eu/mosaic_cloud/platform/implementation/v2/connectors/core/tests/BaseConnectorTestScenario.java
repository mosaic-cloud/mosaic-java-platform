
package eu.mosaic_cloud.platform.implementation.v2.connectors.core.tests;


import java.util.HashSet;

import eu.mosaic_cloud.platform.v2.connectors.core.Connector;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.exceptions.core.ExceptionTracer;
import eu.mosaic_cloud.tools.transcript.core.Transcript;

import org.junit.Assert;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorTestScenario<TEnvironment extends BaseConnectorTestEnvironment>
			extends Object
{
	protected BaseConnectorTestScenario (final TEnvironment environment) {
		super ();
		Preconditions.checkNotNull (environment);
		this.environment = environment;
		this.transcript = this.environment.transcript;
		this.exceptions = this.environment.exceptions;
		this.connectors = new HashSet<Connector> ();
	}
	
	public void destroy () {
		this.destroy_ ();
		while (!this.connectors.isEmpty ()) {
			final Connector connector = this.connectors.iterator ().next ();
			this.destroyConnector (connector);
		}
	}
	
	public void execute () {
		this.execute_ ();
	}
	
	public void initialize () {
		this.initialize_ ();
	}
	
	protected void await (final CallbackCompletion<?> completion) {
		Assert.assertTrue (completion.await (this.environment.poolTimeout));
	}
	
	protected boolean awaitBooleanOutcome (final CallbackCompletion<Boolean> completion) {
		this.await (completion);
		return (this.getBooleanOutcome (completion));
	}
	
	protected Throwable awaitFailure (final CallbackCompletion<?> completion) {
		this.await (completion);
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertNotNull (completion.getException ());
		return (completion.getException ());
	}
	
	protected <Outcome> Outcome awaitOutcome (final CallbackCompletion<Outcome> completion) {
		this.await (completion);
		return (this.getOutcome (completion));
	}
	
	protected boolean awaitSuccess (final CallbackCompletion<?> completion) {
		this.await (completion);
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return (true);
	}
	
	protected void destroy_ () {}
	
	protected void destroyConnector (final Connector connector) {
		Preconditions.checkNotNull (connector);
		this.connectors.remove (connector);
		final CallbackCompletion<Void> completion = connector.destroy ();
		this.awaitSuccess (completion);
	}
	
	protected abstract void execute_ ();
	
	protected boolean getBooleanOutcome (final CallbackCompletion<Boolean> completion) {
		final Boolean value = this.getOutcome (completion);
		Assert.assertNotNull (value);
		return (value.booleanValue ());
	}
	
	protected <Outcome> Outcome getOutcome (final CallbackCompletion<Outcome> completion) {
		Assert.assertTrue (completion.isCompleted ());
		Assert.assertEquals (null, completion.getException ());
		return (completion.getOutcome ());
	}
	
	protected abstract void initialize_ ();
	
	protected void initializeConnector (final Connector connector) {
		this.initializeConnector (connector, true);
	}
	
	protected void initializeConnector (final Connector connector, final boolean register) {
		Preconditions.checkState (!this.connectors.contains (connector));
		Preconditions.checkNotNull (connector);
		final CallbackCompletion<Void> completion = connector.initialize ();
		this.awaitSuccess (completion);
		if (register)
			this.connectors.add (connector);
	}
	
	protected final TEnvironment environment;
	protected final ExceptionTracer exceptions;
	protected final Transcript transcript;
	private final HashSet<Connector> connectors;
}
