
package eu.mosaic_cloud.cloudlets.connectors.executors;


import eu.mosaic_cloud.cloudlets.core.ICloudletController;
import eu.mosaic_cloud.cloudlets.tools.BaseConnectorsFactoryInitializer;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;
import eu.mosaic_cloud.connectors.tools.ConnectorEnvironment;
import eu.mosaic_cloud.platform.core.configuration.IConfiguration;


public final class ExecutorFactoryInitializer
		extends BaseConnectorsFactoryInitializer
{
	@Override
	protected void initialize_1 (final IConnectorsFactoryBuilder builder, final ICloudletController<?> cloudlet, final ConnectorEnvironment environment, final IConnectorsFactory delegate)
	{
		builder.register (IExecutorFactory.class, new IExecutorFactory () {
			@Override
			public <TContext, TOutcome, TExtra> IExecutor<TOutcome, TExtra> create (final IConfiguration configuration, final IExecutorCallback<TContext, TOutcome, TExtra> callback, final TContext callbackContext)
			{
				return new Executor<TContext, TOutcome, TExtra> (cloudlet, environment.getThreading (), environment.getExceptions (), configuration, callback, callbackContext);
			}
		});
	}
	
	public static final ExecutorFactoryInitializer defaultInstance = new ExecutorFactoryInitializer ();
}
