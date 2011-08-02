package mosaic.driver;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

public class DriverPreMain {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] arguments) throws Exception {
		Preconditions.checkArgument (arguments != null);
		Preconditions.checkArgument (arguments.length == 1, "invalid arguments; expected: <resource type: amqp | kv | memcached>");
		String clasz = DriverCallbackType.valueOf(arguments[0].toUpperCase())
				.getCallbackClass();
		BasicComponentHarnessPreMain.main (new String[] {clasz});

	}

}
