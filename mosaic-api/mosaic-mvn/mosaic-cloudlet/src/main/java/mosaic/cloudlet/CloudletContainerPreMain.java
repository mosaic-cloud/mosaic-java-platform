package mosaic.cloudlet;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

public class CloudletContainerPreMain {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] arguments) throws Exception {
		Preconditions.checkArgument(arguments != null);
		Preconditions.checkArgument(arguments.length == 2,
				"invalid arguments: <clodlet jar> <cloudlet descriptor>");
		System.out.println("Container callbacks created with " + arguments[0]
				+ " and " + arguments[1]);
		BasicComponentHarnessPreMain
				.main(new String[] { "mosaic.cloudlet.runtime.ContainerComponentCallbacks" });

	}

}
