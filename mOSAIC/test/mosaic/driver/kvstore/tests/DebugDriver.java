package mosaic.driver.kvstore.tests;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.core.TestLoggingHandler;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.driver.kvstore.MemcachedDriver;

public class DebugDriver {

	public static void main(String[] args) {
		try {
			MemcachedDriver wrapper = MemcachedDriver
					.create(PropertyTypeConfiguration
							.create(new FileInputStream(
									"test/resources/memcached-test.prop")));
			String keyPrefix = UUID.randomUUID().toString();

			String k1 = keyPrefix + "_key_fantastic";
			IOperationCompletionHandler<Boolean> handler = new TestLoggingHandler<Boolean>();
			IResult<Boolean> r1 = wrapper.invokeSetOperation(k1, 30,
					"fantastic", handler);
			System.out.println("Result: " + r1.getResult());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}
}
