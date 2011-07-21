package mosaic.driver.kvstore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;
import mosaic.core.utils.SerDesUtils;

import com.basho.riak.client.RiakBucketInfo;
import com.basho.riak.client.RiakClient;
import com.basho.riak.client.RiakObject;
import com.basho.riak.client.response.BucketResponse;
import com.basho.riak.client.response.FetchResponse;
import com.basho.riak.client.response.HttpResponse;
import com.basho.riak.client.response.StoreResponse;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Riak key-value store.
 * 
 * @author Carmine Di Biase, Georgiana Macariu
 * 
 */

public class RiakRestOperationFactory implements IOperationFactory {
	private RiakClient riakcl = null;
	private String bucket;

	private RiakRestOperationFactory(RiakClient client, String bucket) {
		super();
		this.riakcl = client;
		this.bucket = bucket;
	}

	/**
	 * Creates a new factory.
	 * 
	 * @param client
	 *            the Riak client used for communicating with the key-value
	 *            system
	 * @return the factory
	 */
	public final static RiakRestOperationFactory getFactory(RiakClient client,
			String bucket) {
		return new RiakRestOperationFactory(client, bucket);
	}

	@Override
	public IOperation<?> getOperation(final IOperationType type,
			Object... parameters) {
		IOperation<?> operation = null;
		if (!(type instanceof KeyValueOperations)) {
			operation = new GenericOperation<Object>(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					throw new UnsupportedOperationException(
							"Unsupported operation: " + type.toString());
				}

			});
			return operation;
		}

		final KeyValueOperations mType = (KeyValueOperations) type;
		final String key;
		Object data;
		final byte[] dataBytes;
		try {
			switch (mType) {
			case SET:
				// to change
				key = (String) parameters[0];
				data = parameters[1];
				dataBytes = SerDesUtils.toBytes(data);
				final RiakObject riakobj = new RiakObject(this.bucket, key,
						dataBytes);
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								// System.out.println("Driver Set value "+dataBytes+" "+SerDesUtils.toObject(dataBytes));
								StoreResponse response = RiakRestOperationFactory.this.riakcl
										.store(riakobj);
								if (response.isSuccess())
									return true;
								return false;
							}

						});
				break;
			case GET:
				key = (String) parameters[0];
				operation = new GenericOperation<Object>(
						new Callable<Object>() {

							@Override
							public Object call() throws Exception {
								FetchResponse res = RiakRestOperationFactory.this.riakcl
										.fetch(RiakRestOperationFactory.this.bucket,
												key);
								if (res.hasObject()) {
									final RiakObject riakobj = res.getObject();
									// System.out.println("Driver Get value "+SerDesUtils.toObject(riakobj.getValueAsBytes()));
									return SerDesUtils.toObject(riakobj
											.getValueAsBytes());
								} else
									// System.out.println("Driver Get value "+SerDesUtils.toObject(riakobj.getValueAsBytes()));
									return null;
							}

						});
				break;
			case LIST:
				operation = new GenericOperation<List<String>>(
						new Callable<List<String>>() {

							@Override
							public List<String> call() throws Exception {
								BucketResponse res = RiakRestOperationFactory.this.riakcl
										.listBucket(RiakRestOperationFactory.this.bucket);
								List<String> keys = new ArrayList<String>();
								if (res.isSuccess()) {
									RiakBucketInfo info = res.getBucketInfo();
									keys = (List<String>) info.getKeys();
									// System.out.println("Driver List value "+keys);
									return keys;
								} else
									return keys;
							}

						});
				break;
			case DELETE:
				key = (String) parameters[0];
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								HttpResponse res = RiakRestOperationFactory.this.riakcl
										.delete(RiakRestOperationFactory.this.bucket,
												key);
								if (res.getStatusCode() == 404)
									return false;
								return true;
							}

						});
				break;
			default:
				operation = new GenericOperation<Object>(
						new Callable<Object>() {

							@Override
							public Object call() throws Exception {
								throw new UnsupportedOperationException(
										"Unsupported operation: "
												+ mType.toString());
							}

						});
			}
		} catch (final IOException e) {
			operation = new GenericOperation<Object>(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					throw e;
				}

			});
			ExceptionTracer.traceDeferred(e);
		}
		return operation;
	}

}
