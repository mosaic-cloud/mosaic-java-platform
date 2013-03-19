/*
 * #%L
 * mosaic-drivers-stubs-riak
 * %%
 * Copyright (C) 2010 - 2013 Institute e-Austria Timisoara (Romania)
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

package eu.mosaic_cloud.drivers.kvstore;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.IOperation;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IOperationType;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;
import eu.mosaic_cloud.tools.exceptions.tools.BaseExceptionTracer;

import com.basho.riak.client.RiakBucketInfo;
import com.basho.riak.client.RiakClient;
import com.basho.riak.client.RiakObject;
import com.basho.riak.client.request.RequestMeta;
import com.basho.riak.client.response.BucketResponse;
import com.basho.riak.client.response.FetchResponse;
import com.basho.riak.client.response.HttpResponse;
import com.basho.riak.client.response.StoreResponse;
import com.basho.riak.client.util.Constants;


/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Riak key-value store.
 * 
 * @author Carmine Di Biase, Georgiana Macariu
 * @deprecated
 */
@Deprecated
public final class RiakRestOperationFactory
		implements
			IOperationFactory
{
	private RiakRestOperationFactory (final String riakHost, final int riakPort, final String bucket, final String clientId)
	{
		super ();
		final String address = "http://" + riakHost + ":" + riakPort + "/riak";
		this.riakcl = new RiakClient (address);
		this.bucket = bucket;
		this.clientId = clientId;
		this.exceptions = FallbackExceptionTracer.defaultInstance;
	}
	
	@Override
	public void destroy ()
	{
		// NOTE: nothing to do here
	}
	
	@Override
	public IOperation<?> getOperation (final IOperationType type, final Object ... parameters)
	{
		IOperation<?> operation;
		if (!(type instanceof KeyValueOperations)) {
			return new GenericOperation<Object> (new Callable<Object> () {
				@Override
				public Object call ()
						throws UnsupportedOperationException
				{
					throw new UnsupportedOperationException ("Unsupported operation: " + type.toString ());
				}
			});
		}
		final KeyValueOperations mType = (KeyValueOperations) type;
		try {
			switch (mType) {
				case SET :
					operation = this.buildSetOperation (parameters);
					break;
				case GET :
					operation = this.buildGetOperation (parameters);
					break;
				case LIST :
					operation = this.buildListOperation ();
					break;
				case DELETE :
					operation = this.buildDeleteOperation (parameters);
					break;
				default:
					operation = new GenericOperation<Object> (new Callable<Object> () {
						@Override
						public Object call ()
								throws UnsupportedOperationException
						{
							throw new UnsupportedOperationException ("Unsupported operation: " + mType.toString ());
						}
					});
			}
		} catch (final Exception e) {
			this.exceptions.traceDeferredException (e);
			operation = new GenericOperation<Object> (new Callable<Object> () {
				@Override
				public Object call ()
						throws Exception
				{
					throw e;
				}
			});
		}
		return operation;
	}
	
	private IOperation<?> buildDeleteOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
			{
				final String key = (String) parameters[0];
				final RequestMeta meta = new RequestMeta ();
				meta.setClientId (RiakRestOperationFactory.this.clientId);
				final HttpResponse res = RiakRestOperationFactory.this.riakcl.delete (RiakRestOperationFactory.this.bucket, key, meta);
				if (res.getStatusCode () == 404) {
					return false;
				}
				return true;
			}
		});
	}
	
	private IOperation<?> buildGetOperation (final Object ... parameters)
	{
		return new GenericOperation<byte[]> (new Callable<byte[]> () {
			@Override
			public byte[] call ()
			{
				final String key = (String) parameters[0];
				final RequestMeta meta = new RequestMeta ();
				meta.setClientId (RiakRestOperationFactory.this.clientId);
				meta.setAccept (Constants.CTYPE_ANY + ", " + Constants.CTYPE_MULTIPART_MIXED);
				final FetchResponse res = RiakRestOperationFactory.this.riakcl.fetch (RiakRestOperationFactory.this.bucket, key, meta);
				final RiakObject riakobj;
				if (res.hasSiblings ()) {
					RiakObject oldest = null;
					long oldestMod = Long.MIN_VALUE;
					for (final RiakObject sibling : res.getSiblings ()) {
						final long siblingMod = sibling.getLastmodAsDate ().getTime ();
						if (siblingMod >= oldestMod) {
							oldest = sibling;
							oldestMod = siblingMod;
						}
					}
					riakobj = oldest;
				} else if (res.hasObject ()) {
					riakobj = res.getObject ();
				} else {
					riakobj = null;
				}
				if (riakobj != null) {
					return riakobj.getValueAsBytes ();
				} else {
					return null;
				}
			}
		});
	}
	
	private IOperation<?> buildListOperation ()
	{
		return new GenericOperation<List<String>> (new Callable<List<String>> () {
			@Override
			public List<String> call ()
			{
				final RequestMeta meta = new RequestMeta ();
				meta.setClientId (RiakRestOperationFactory.this.clientId);
				final BucketResponse res = RiakRestOperationFactory.this.riakcl.listBucket (RiakRestOperationFactory.this.bucket, meta);
				List<String> keys = new ArrayList<String> ();
				if (res.isSuccess ()) {
					final RiakBucketInfo info = res.getBucketInfo ();
					keys = (List<String>) info.getKeys ();
					return keys;
				} else {
					return keys;
				}
			}
		});
	}
	
	private IOperation<?> buildSetOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
			{
				final String key = (String) parameters[0];
				final byte[] dataBytes = (byte[]) parameters[1];
				final RiakObject riakobj = new RiakObject (RiakRestOperationFactory.this.bucket, key, dataBytes);
				final RequestMeta meta = new RequestMeta ();
				meta.setClientId (RiakRestOperationFactory.this.clientId);
				final StoreResponse response = RiakRestOperationFactory.this.riakcl.store (riakobj, meta);
				if (response.isSuccess ()) {
					return true;
				}
				return false;
			}
		});
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
	public static RiakRestOperationFactory getFactory (final String riakHost, final int port, final String bucket, final String clientId)
	{
		return new RiakRestOperationFactory (riakHost, port, bucket, clientId);
	}
	
	private final String bucket;
	private final String clientId;
	private final BaseExceptionTracer exceptions;
	private final RiakClient riakcl;
}
