/*
 * #%L
 * mosaic-connectors
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

package eu.mosaic_cloud.connectors.tools;

import java.util.concurrent.ConcurrentHashMap;

import eu.mosaic_cloud.connectors.core.IConnector;
import eu.mosaic_cloud.connectors.core.IConnectorFactory;
import eu.mosaic_cloud.connectors.core.IConnectorsFactory;
import eu.mosaic_cloud.tools.miscellaneous.Monitor;

import com.google.common.base.Preconditions;

public abstract class BaseConnectorsFactory extends Object implements IConnectorsFactory {

    protected final ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, IConnectorFactory<?>> factories;
    protected final IConnectorsFactory delegate;
    protected final Monitor monitor;

    protected BaseConnectorsFactory(final IConnectorsFactory delegate) {
        super();
        this.monitor = Monitor.create(this);
        this.delegate = delegate;
        this.factories = new ConcurrentHashMap<Class<? extends IConnectorFactory<?>>, IConnectorFactory<?>>();
    }

    @Override
    public <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> Factory getConnectorFactory(
            final Class<Factory> factoryClass) {
        Factory factory = factoryClass.cast(this.factories.get(factoryClass));
        if ((factory == null) && (this.delegate != null)) {
            factory = this.delegate.getConnectorFactory(factoryClass);
        }
        return factory;
    }

    protected final <Connector extends IConnector, Factory extends IConnectorFactory<? super Connector>> void registerFactory(
            final Class<Factory> factoryClass, final Factory factory) {
        Preconditions.checkNotNull(factoryClass);
        Preconditions.checkArgument(factoryClass.isInterface());
        Preconditions.checkArgument(IConnectorFactory.class.isAssignableFrom(factoryClass));
        Preconditions.checkNotNull(factory);
        Preconditions.checkArgument(factoryClass.isInstance(factory));
        synchronized (this.monitor) {
            Preconditions.checkState(!this.factories.containsKey(factoryClass));
            this.factories.put(factoryClass, factory);
        }
    }
}
