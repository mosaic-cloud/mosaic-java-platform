
package eu.mosaic_cloud.connectors.tools;


import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.connectors.core.IConnector;
import eu.mosaic_cloud.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;

import com.google.common.base.Preconditions;


public abstract class BaseConnectorsFactory
		extends Object
		implements
			IConnectorsFactory
{
	protected BaseConnectorsFactory ()
	{
		super ();
		this.monitor = Monitor.create (this);
		this.factories = new ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, IConnectorFactory<?>> ();
	}
	
	@Override
	public <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> Factory getConnectorFactory (final Class<Factory> factoryClass)
	{
		return (factoryClass.cast (this.factories.get (factoryClass)));
	}
	
	protected final <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> void registerFactory (final Class<Factory> factoryClass, final Factory factory)
	{
		Preconditions.checkNotNull (factoryClass);
		Preconditions.checkArgument (factoryClass.isInterface ());
		Preconditions.checkArgument (IConnectorFactory.class.isAssignableFrom (factoryClass));
		Preconditions.checkNotNull (factory);
		Preconditions.checkArgument (factoryClass.isInstance (factory));
		synchronized (this.monitor) {
			Preconditions.checkState (!this.factories.containsKey (factoryClass));
			this.factories.put (factoryClass, factory);
		}
	}
	
	final ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, IConnectorFactory<?>> factories;
	final Monitor monitor;
}
