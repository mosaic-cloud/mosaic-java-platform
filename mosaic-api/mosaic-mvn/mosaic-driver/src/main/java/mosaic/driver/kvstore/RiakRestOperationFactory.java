package mosaic.driver.kvstore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import mosaic.core.exceptions.ExceptionTracer;
import mosaic.core.ops.GenericOperation;
import mosaic.core.ops.IOperation;
import mosaic.core.ops.IOperationFactory;
import mosaic.core.ops.IOperationType;

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

	private RiakRestOperationFactory(String riakHost, int riakPort,
			String bucket) {
		super();
		String address = "http://" + riakHost + ":" + riakPort + "/riak";
		this.riakcl = new RiakClient(address);
		this.bucket = bucket;
	}

	/**
	 * Creates a new factory.
	 * 
	 * @param riakHost
	 *            the hostname of the Riak server
	 * @param port
	 *            the port for the Riak server
	 * @param bucket
	 *            the bucket associated with the connection
	 * @return the factory
	 */
	public final static RiakRestOperationFactory getFactory(String riakHost,
			int port, String bucket) {
		return new RiakRestOperationFactory(riakHost, port, bucket);
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
		final byte[] dataBytes;
		try {
			switch (mType) {
			case SET:
				// to change
				key = (String) parameters[0];
				// data = parameters[1];
				dataBytes = (byte[]) parameters[1];// SerDesUtils.toBytes(data);
				final RiakObject riakobj = new RiakObject(this.bucket, key,
						dataBytes);
				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								try {
									// System.out.println("Driver Set value "+dataBytes+" "+SerDesUtils.toObject(dataBytes));
									StoreResponse response = RiakRestOperationFactory.this.riakcl
											.store(riakobj);
									if (response.isSuccess())
										return true;
								} catch (Throwable e) {
									e.printStackTrace();
								}
								return false;
							}

						});
				break;
			case GET:
				key = (String) parameters[0];
				operation = new GenericOperation<byte[]>(
						new Callable<byte[]>() {

							@Override
							public byte[] call() throws Exception {
								try {
									FetchResponse res = RiakRestOperationFactory.this.riakcl
											.fetch(RiakRestOperationFactory.this.bucket,
													key);
									if (res.hasObject()) {
										final RiakObject riakobj = res
												.getObject();
										return riakobj.getValueAsBytes();
									} else
										return null;

								} catch (Throwable e) {
									e.printStackTrace();
								}
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
		} catch (final Exception e) {
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

	@Override
	public void destroy() {

	}

}
