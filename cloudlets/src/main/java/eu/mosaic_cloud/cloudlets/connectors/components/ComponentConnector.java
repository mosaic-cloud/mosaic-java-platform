
package eu.mosaic_cloud.cloudlets.connectors.components;


import eu.mosaic_cloud.cloudlets.connectors.core.BaseConnector;
import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.components.core.ComponentIdentifier;
import eu.mosaic_cloud.components.core.ComponentResourceDescriptor;
import eu.mosaic_cloud.components.core.ComponentResourceSpecification;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletion;
import eu.mosaic_cloud.tools.callbacks.core.CallbackCompletionObserver;


public class ComponentConnector<TContext, TExtra>
		extends BaseConnector<eu.mosaic_cloud.cloudlets.implementation.container.IComponentConnector, IComponentConnectorCallbacks<TContext, TExtra>, TContext>
		implements
			IComponentConnector<TExtra>
{
	public ComponentConnector (final ICloudletController<?> cloudlet, final eu.mosaic_cloud.cloudlets.implementation.container.IComponentConnector connector, final IConfiguration configuration, final IComponentConnectorCallbacks<TContext, TExtra> callback, final TContext context)
	{
		super (cloudlet, connector, configuration, callback, context);
	}
	
	@Override
	public final CallbackCompletion<ComponentResourceDescriptor> acquire (final ComponentResourceSpecification resource, final TExtra extra)
	{
		this.transcript.traceDebugging ("acquiring the resource `%s`...", resource.identifier);
		final CallbackCompletion<ComponentResourceDescriptor> completion = this.connector.acquire (resource);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					if (completion.getException () != null) {
						ComponentConnector.this.transcript.traceDebugging ("triggering the callback for acquire failure for resource `%s` and extra `%{object}`...", resource.identifier, extra);
						return ComponentConnector.this.callback.acquireFailed (ComponentConnector.this.context, new ComponentRequestFailedCallbackArguments<TExtra> (ComponentConnector.this.cloudlet, completion.getException (), extra));
					}
					ComponentConnector.this.transcript.traceDebugging ("triggering the callback for acquire success for resource `%s` and extra `%{object}`...", resource.identifier, extra);
					return ComponentConnector.this.callback.acquireSucceeded (ComponentConnector.this.context, new ComponentAcquireSucceededCallbackArguments<TExtra> (ComponentConnector.this.cloudlet, completion.getOutcome (), extra));
				}
			});
		}
		return completion;
	}
	
	@Override
	public final <TInputs, TOutputs> CallbackCompletion<TOutputs> call (final ComponentIdentifier component, final String operation, final TInputs inputs, final Class<TOutputs> outputs, final TExtra extra)
	{
		this.transcript.traceDebugging ("calling to the resource `%s` with the operation `%s`...", component.string, operation);
		final CallbackCompletion<TOutputs> completion = this.connector.call (component, operation, inputs, outputs);
		if (this.callback != null) {
			completion.observe (new CallbackCompletionObserver () {
				@SuppressWarnings ("synthetic-access")
				@Override
				public CallbackCompletion<Void> completed (final CallbackCompletion<?> completion_)
				{
					assert (completion_ == completion);
					if (completion.getException () != null) {
						ComponentConnector.this.transcript.traceDebugging ("triggering the callback for call failure to the component `%s` with the operation `%s` and extra `%{object}`...", component.string, operation, extra);
						return ComponentConnector.this.callback.callFailed (ComponentConnector.this.context, new ComponentRequestFailedCallbackArguments<TExtra> (ComponentConnector.this.cloudlet, completion.getException (), extra));
					}
					ComponentConnector.this.transcript.traceDebugging ("triggering the callback for call success to the component `%s` with the operation `%s` and extra `%{object}`...", component.string, operation, extra);
					return ComponentConnector.this.callback.callSucceeded (ComponentConnector.this.context, new ComponentCallSucceededCallbackArguments<TOutputs, TExtra> (ComponentConnector.this.cloudlet, completion.getOutcome (), extra));
				}
			});
		}
		return completion;
	}
	
	@Override
	public final <TInputs> CallbackCompletion<Void> cast (final ComponentIdentifier component, final String operation, final TInputs inputs)
	{
		this.transcript.traceDebugging ("casting to the component `%s` with the operation `%s`...", component.string, operation);
		final CallbackCompletion<Void> completion = this.connector.cast (component, operation, inputs);
		return completion;
	}
	
	@Override
	public CallbackCompletion<Void> destroy ()
	{
		return this.destroy (false);
	}
	
	@Override
	public CallbackCompletion<Void> initialize ()
	{
		return this.initialize (false);
	}
}
