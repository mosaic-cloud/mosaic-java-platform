
package eu.mosaic_cloud.connectors.tools;


import java.util.concurrent.atomic.AtomicBoolean;

import eu.mosaic_cloud.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactoryBuilder;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorsFactoryBuilder<TFactory extends BaseConnectorsFactory>
		extends Object
		implements
			IConnectorsFactoryBuilder
{
	protected BaseConnectorsFactoryBuilder (final TFactory factory)
	{
		super ();
		Preconditions.checkNotNull (factory);
		this.factory = factory;
		this.environment = this.factory.environment;
		this.delegate = this.factory.delegate;
		this.built = new AtomicBoolean (false);
	}
	
	@Override
	public final TFactory build ()
	{
		Preconditions.checkState (this.built.compareAndSet (false, true));
		this.build_1 ();
		return (this.factory);
	}
	
	@Override
	public final <TConnectorFactory extends IConnectorFactory<?>> void register (final Class<TConnectorFactory> factoryClass, final TConnectorFactory factory)
	{
		Preconditions.checkState (!this.built.get ());
		Preconditions.checkNotNull (factoryClass);
		Preconditions.checkArgument (factoryClass.isInterface ());
		Preconditions.checkArgument (IConnectorFactory.class.isAssignableFrom (factoryClass));
		Preconditions.checkNotNull (factory);
		Preconditions.checkArgument (factoryClass.isInstance (factory));
		this.factory.registerFactory (factoryClass, factory);
	}
	
	protected void build_1 ()
	{}
	
	protected void initialize ()
	{
		this.initialize_1 ();
	}
	
	protected void initialize_1 ()
	{}
	
	protected <TConnectorFactory extends IConnectorFactory<?>> void register_1 (final Class<TConnectorFactory> factoryClass, final TConnectorFactory factory)
	{
		this.factory.registerFactory (factoryClass, factory);
	}
	
	protected final IConnectorsFactory delegate;
	protected final ConnectorEnvironment environment;
	protected final TFactory factory;
	private final AtomicBoolean built;
}
