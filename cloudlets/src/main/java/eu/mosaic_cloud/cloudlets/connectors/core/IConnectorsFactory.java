
package eu.mosaic_cloud.cloudlets.connectors.core;

public interface IConnectorsFactory {

    <Factory extends IConnectorFactory<?>> Factory getConnectorFactory(Class<Factory> factory);
}
