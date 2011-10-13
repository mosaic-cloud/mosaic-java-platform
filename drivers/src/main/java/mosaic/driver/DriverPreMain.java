package mosaic.driver;

import java.lang.reflect.InvocationTargetException;

import com.google.common.base.Preconditions;

import eu.mosaic_cloud.components.implementations.basic.BasicComponentHarnessPreMain;

public final class DriverPreMain {

	private DriverPreMain() {
	}

	/**
	 * @param args
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws Exception
	 */
	public static void main(String[] arguments) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException  {
		Preconditions.checkArgument(arguments != null);
		Preconditions
				.checkArgument(arguments.length == 1,
						"invalid arguments; expected: <resource type: amqp | kv | memcached>");
		String clasz = DriverCallbackType.valueOf(arguments[0].toUpperCase())
				.getCallbackClass();
		BasicComponentHarnessPreMain.main(new String[] { clasz });
	}

}
