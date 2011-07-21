package mosaic.connector.components;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import mosaic.connector.components.ResourceComponentCallbacks.ResourceType;
import mosaic.connector.kvstore.KeyValueStoreConnector;
import mosaic.core.configuration.ConfigUtils;
import mosaic.core.configuration.IConfiguration;
import mosaic.core.configuration.PropertyTypeConfiguration;
import mosaic.core.log.MosaicLogger;
import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;
import mosaic.interop.idl.ChannelData;

import com.google.common.base.Preconditions;

public class KeyValueConnectorCompTest {
	private IConfiguration configuration;
	private KeyValueStoreConnector connector;
	private static String keyPrefix;
	private static String storeType;

	public KeyValueConnectorCompTest() throws Throwable {
		configuration = PropertyTypeConfiguration.create(
				KeyValueConnectorCompTest.class.getClassLoader(),
				"kv-conn-test.prop");
		KeyValueConnectorCompTest.storeType = ConfigUtils.resolveParameter(
				configuration, "kvstore.driver_name", String.class, "");
		KeyValueConnectorCompTest.keyPrefix = UUID.randomUUID().toString();
		ResourceFinder.getResourceFinder().findResource(ResourceType.KEY_VALUE,
				new Callback());

	}

	public void testConnection() {
		Preconditions.checkNotNull(connector, "Connector not created");
	}

	private static <T> List<IOperationCompletionHandler<T>> getHandlers(
			String testName) {
		IOperationCompletionHandler<T> handler = new TestLoggingHandler<T>(
				testName);
		List<IOperationCompletionHandler<T>> list = new ArrayList<IOperationCompletionHandler<T>>();
		list.add(handler);
		return list;
	}

	public void testSet() {
		String k1 = KeyValueConnectorCompTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers1 = getHandlers("set 1");
		IResult<Boolean> r1 = connector.set(k1, "fantastic", handlers1, null);
		Preconditions.checkNotNull(r1);

		String k2 = KeyValueConnectorCompTest.keyPrefix + "_key_famous";
		List<IOperationCompletionHandler<Boolean>> handlers2 = getHandlers("set 2");
		IResult<Boolean> r2 = connector.set(k2, "famous", handlers2, null);
		Preconditions.checkNotNull(r2);

		try {
			Preconditions.checkState(r1.getResult(), "Set 1 returned false.");
			Preconditions.checkState(r2.getResult(), "Set 2 returned false.");
		} catch (InterruptedException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		} catch (ExecutionException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		}
	}

	public void testGet() {
		String k1 = KeyValueConnectorCompTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Object>> handlers = getHandlers("get");
		IResult<Object> r1 = connector.get(k1, handlers, null);

		try {
			Preconditions.checkState(
					"fantastic".equals(r1.getResult().toString()),
					"Get returned something wrong");
		} catch (InterruptedException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		} catch (ExecutionException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		}
	}

	public void testDelete() {
		String k1 = KeyValueConnectorCompTest.keyPrefix + "_key_fantastic";
		List<IOperationCompletionHandler<Boolean>> handlers = getHandlers("delete");
		IResult<Boolean> r1 = connector.delete(k1, handlers, null);
		try {
			Preconditions.checkState(r1.getResult(), "Object not deleted.");
		} catch (InterruptedException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		} catch (ExecutionException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		}

		List<IOperationCompletionHandler<Object>> handlers1 = getHandlers("get after delete");
		IResult<Object> r2 = connector.get(k1, handlers1, null);

		try {
			Preconditions.checkState(r2.getResult() == null,
					"Object still exists after delete");
		} catch (InterruptedException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		} catch (ExecutionException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		}
	}

	public void testList() {
		List<IOperationCompletionHandler<List<String>>> handlers = new ArrayList<IOperationCompletionHandler<List<String>>>();
		handlers.add(new TestLoggingHandler<List<String>>("list"));
		IResult<List<String>> r1 = connector.list(handlers, null);
		try {
			if (KeyValueConnectorCompTest.storeType
					.equalsIgnoreCase("memcached")) {
				Preconditions.checkState(r1.getResult() == null);
			} else {
				Preconditions
						.checkNotNull(r1.getResult(), "List returned null");
				for (String key : r1.getResult()) {
					System.out.println(key);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		} catch (ExecutionException e) {
			e.printStackTrace();
			Preconditions.checkNotNull(null);
		}
	}

	public void testConnector() {
		testConnection();
		testSet();
		testGet();
		testList();
		testDelete();
	}

	public static void test() throws Throwable {
		KeyValueConnectorCompTest test = new KeyValueConnectorCompTest();
	}

	class Callback implements IFinderCallback {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * mosaic.connector.temp.IFinderCallback#resourceFound(mosaic.interop
		 * .idl.ChannelData)
		 */
		@Override
		public void resourceFound(ChannelData channel) throws Throwable {
			KeyValueConnectorCompTest.this.configuration
					.addParameter("interop.driver.identifier",
							channel.getChannelIdentifier());
			KeyValueConnectorCompTest.this.configuration.addParameter(
					"interop.channel.address", channel.getChannelEndpoint());
			KeyValueConnectorCompTest.this.connector = KeyValueStoreConnector
					.create(KeyValueConnectorCompTest.this.configuration);
			KeyValueConnectorCompTest.this.testConnector();
			KeyValueConnectorCompTest.this.connector.destroy();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see mosaic.connector.temp.IFinderCallback#resourceNotFound()
		 */
		@Override
		public void resourceNotFound() {
			MosaicLogger.getLogger().error("Callback - Resource not found");
		}
	}
}
