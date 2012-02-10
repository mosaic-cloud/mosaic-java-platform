package mosaic.examples.feeds;

import mosaic.cloudlet.CloudletContainerPreMain.CloudletContainerParameters;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

public class IndexerCloudletPreMain {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] arguments) throws Exception {
		Preconditions.checkArgument(arguments != null);
		Preconditions.checkArgument(arguments.length == 0, "invalid arguments");
		CloudletContainerParameters.classpath = null;
		CloudletContainerParameters.configFile = "indexer-cloudlet.prop";
		BasicComponentHarnessPreMain
				.main(new String[] { "mosaic.cloudlet.runtime.ContainerComponentCallbacks" });
	}

}
