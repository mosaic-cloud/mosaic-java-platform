/*
 * #%L
 * mosaic-driver
 * %%
 * Copyright (C) 2010 - 2011 mOSAIC Project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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

public final class RiakRestOperationFactory implements IOperationFactory { // NOPMD by georgiana on 10/12/11 4:46 PM

	private final RiakClient riakcl;
	private final String bucket;

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
	public static RiakRestOperationFactory getFactory(String riakHost,
			int port, String bucket) {
		return new RiakRestOperationFactory(riakHost, port, bucket);
	}

	@Override
	public IOperation<?> getOperation(final IOperationType type, // NOPMD by georgiana on 10/12/11 4:46 PM
			Object... parameters) {
		IOperation<?> operation;
		if (!(type instanceof KeyValueOperations)) {
			return new GenericOperation<Object>(new Callable<Object>() { // NOPMD by georgiana on 10/12/11 4:46 PM

						@Override
						public Object call()
								throws UnsupportedOperationException {
							throw new UnsupportedOperationException(
									"Unsupported operation: " + type.toString());
						}

					});
		}

		final KeyValueOperations mType = (KeyValueOperations) type;
		try {
			switch (mType) {
			case SET:
				operation = buildSetOperation(parameters); // NOPMD by georgiana on 10/12/11 4:46 PM
				break;
			case GET:
				operation = buildGetOperation(parameters); // NOPMD by georgiana on 10/12/11 4:45 PM
				break;
			case LIST:
				operation = buildListOperation(); // NOPMD by georgiana on 10/12/11 4:45 PM
				break;
			case DELETE:
				operation = buildDeleteOperation(parameters); // NOPMD by georgiana on 10/12/11 4:45 PM
				break;
			default:
				operation = new GenericOperation<Object>( // NOPMD by georgiana on 10/12/11 4:45 PM
						new Callable<Object>() {

							@Override
							public Object call()
									throws UnsupportedOperationException {
								throw new UnsupportedOperationException(
										"Unsupported operation: "
												+ mType.toString());
							}

						});
			}
		} catch (final Exception e) {
			operation = new GenericOperation<Object>(new Callable<Object>() { // NOPMD by georgiana on 10/12/11 4:45 PM

						@Override
						public Object call() throws Exception { // NOPMD by georgiana on 10/12/11 4:46 PM
							throw e;
						}

					});
			ExceptionTracer.traceIgnored(e);
		}
		return operation;
	}

	private IOperation<?> buildDeleteOperation(final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() {
				String key = (String) parameters[0];

				HttpResponse res = RiakRestOperationFactory.this.riakcl.delete(
						RiakRestOperationFactory.this.bucket, key);
				if (res.getStatusCode() == 404) {
					return false;
				}
				return true;
			}

		});
	}

	private IOperation<?> buildListOperation() {
		return new GenericOperation<List<String>>(new Callable<List<String>>() {

			@Override
			public List<String> call() {
				BucketResponse res = RiakRestOperationFactory.this.riakcl
						.listBucket(RiakRestOperationFactory.this.bucket);
				List<String> keys = new ArrayList<String>();
				if (res.isSuccess()) {
					RiakBucketInfo info = res.getBucketInfo();
					keys = (List<String>) info.getKeys();
					return keys;
				} else {
					return keys;
				}
			}

		});
	}

	private IOperation<?> buildGetOperation(final Object... parameters) {
		return new GenericOperation<byte[]>(new Callable<byte[]>() {

			@Override
			public byte[] call() {
				String key = (String) parameters[0];

				FetchResponse res = RiakRestOperationFactory.this.riakcl.fetch(
						RiakRestOperationFactory.this.bucket, key);
				if (res.hasObject()) {
					final RiakObject riakobj = res.getObject();
					return riakobj.getValueAsBytes();
				} else {
					return null;
				}
			}
		});
	}

	private IOperation<?> buildSetOperation(final Object... parameters) {
		return new GenericOperation<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() {
				String key = (String) parameters[0];
				byte[] dataBytes = (byte[]) parameters[1];
				RiakObject riakobj = new RiakObject(
						RiakRestOperationFactory.this.bucket, key, dataBytes);

				StoreResponse response = RiakRestOperationFactory.this.riakcl
						.store(riakobj);
				if (response.isSuccess()) {
					return true;
				}
				return false;
			}

		});
	}

	@Override
	public void destroy() {
		// nothing to do here
	}

}
