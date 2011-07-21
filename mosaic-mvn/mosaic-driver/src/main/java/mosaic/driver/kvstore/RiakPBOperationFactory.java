package mosaic.driver.kvstore;

import static com.google.protobuf.ByteString.copyFromUtf8;

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

import com.basho.riak.pbc.KeySource;
import com.basho.riak.pbc.RiakClient;
import com.basho.riak.pbc.RiakObject;
import com.google.protobuf.ByteString;

/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Riak key-value store.
 * 
 * @author Carmine Di Biase, Georgiana Macariu
 * 
 */
public class RiakPBOperationFactory implements IOperationFactory {
	private RiakClient riakcl = null;
	private String bucket;

	private RiakPBOperationFactory(RiakClient client, String bucket) {
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
	 * @param bucket
	 *            the bucket associted with the connection
	 * @return the factory
	 */
	public final static RiakPBOperationFactory getFactory(RiakClient client,
			String bucket) {
		return new RiakPBOperationFactory(client, bucket);
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
		final Object data;
		final byte[] dataBytes;
		try {
			switch (mType) {
			case SET:
				key = (String) parameters[0];
				data = parameters[1];
				dataBytes = SerDesUtils.toBytes(data);

				ByteString keyBS = ByteString.copyFromUtf8(key);
				ByteString bucketBS = ByteString.copyFromUtf8(bucket);
				ByteString dataBS = ByteString.copyFrom(dataBytes);

				final RiakObject riakobj = new RiakObject(bucketBS, keyBS,
						dataBS);

				operation = new GenericOperation<Boolean>(
						new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								RiakObject[] fetched = RiakPBOperationFactory.this.riakcl
										.fetch(bucket, key);
								if (fetched.length == 0) {
									RiakPBOperationFactory.this.riakcl
											.store(riakobj);
									return true;
								} else
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
								RiakObject[] riakobj = RiakPBOperationFactory.this.riakcl
										.fetch(bucket, key);
								if (riakobj.length == 1)
									return SerDesUtils.toObject(riakobj[0]
											.getValue().toByteArray());
								else
									return null;
							}

						});
				break;
			case LIST:
				operation = new GenericOperation<List<String>>(
						new Callable<List<String>>() {

							@Override
							public List<String> call() throws Exception {
								KeySource ks;
								ks = RiakPBOperationFactory.this.riakcl
										.listKeys(copyFromUtf8(bucket));
								List<String> keys = new ArrayList<String>();
								while (ks.hasNext()) {
									keys.add(ks.next().toStringUtf8());
								}
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
								RiakObject[] res = RiakPBOperationFactory.this.riakcl
										.fetch(bucket, key);
								if (res.length == 1) {
									RiakPBOperationFactory.this.riakcl.delete(
											bucket, key);
									return true;
								} else
									return false;

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
