package mosaic.cloudlet.tests;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.DefaultCloudletCallback;
import mosaic.cloudlet.core.ICloudletController;
import mosaic.core.log.MosaicLogger;

public class HelloWorldCloudlet {
	public static final class LifeCycleHandler extends
			DefaultCloudletCallback<HelloCloudletState> {

		@Override
		public void initialize(HelloCloudletState state,
				CallbackArguments<HelloCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"HelloWorld cloudlet is initializing...");
		}

		@Override
		public void initializeSucceeded(HelloCloudletState state,
				CallbackArguments<HelloCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"HelloWorld cloudlet was initialized successfully.");
			System.out.println("Hello world!");
			ICloudletController<HelloCloudletState> cloudlet = arguments
					.getCloudlet();
			cloudlet.destroy();
		}

		@Override
		public void destroy(HelloCloudletState state,
				CallbackArguments<HelloCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"HelloWorld cloudlet is being destroyed.");
		}

		public void destroySucceeded(HelloCloudletState state,
				CallbackArguments<HelloCloudletState> arguments) {
			MosaicLogger.getLogger().info(
					"HelloWorld cloudlet was destroyed successfully.");
		}

	}

	public static final class HelloCloudletState {

	}
}
