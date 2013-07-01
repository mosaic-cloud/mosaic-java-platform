/*
 * #%L
 * mosaic-drivers-stubs-memcache
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


import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.mosaic_cloud.drivers.ops.GenericOperation;
import eu.mosaic_cloud.drivers.ops.IOperation;
import eu.mosaic_cloud.drivers.ops.IOperationFactory;
import eu.mosaic_cloud.drivers.ops.IOperationType;
import eu.mosaic_cloud.platform.core.utils.EncodingMetadata;
import eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage;
import eu.mosaic_cloud.tools.exceptions.core.FallbackExceptionTracer;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;

import net.spy.memcached.CASResponse;
import net.spy.memcached.MemcachedClient;


/**
 * Factory class which builds the asynchronous calls for the operations defined
 * in the memcached protocol.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class MemcachedOperationFactory
		implements
			IOperationFactory
{
	private MemcachedOperationFactory (final List<?> servers, final String user, final String password, final String bucket, final boolean useBucket)
			throws IOException
	{
		super ();
		if (useBucket) {
			@SuppressWarnings ("unchecked") final List<URI> nodes = (List<URI>) servers;
			this.mcClient = new CouchbaseClient (nodes, bucket, user, password);
		} else {
			@SuppressWarnings ("unchecked") final List<URI> nodes = (List<URI>) servers;
			final CouchbaseConnectionFactory factory = new CouchbaseConnectionFactory (nodes, "default", "");
			this.mcClient = new CouchbaseClient (factory);
		}
	}
	
	@Override
	public void destroy ()
	{
		this.mcClient.shutdown (30, TimeUnit.SECONDS);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.mosaic_cloud.platform.core.IOperationFactory#getOperation(eu.mosaic_cloud
	 * .platform.core.IOperationType, java.lang.Object[])
	 */
	@Override
	public IOperation<?> getOperation (final IOperationType type, final Object ... parameters)
	{
		IOperation<?> operation = null;
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
		switch (mType) {
			case SET :
				operation = this.buildSetOperation (parameters);
				break;
			case ADD :
				operation = this.buildAddOperation (parameters);
				break;
			case REPLACE :
				operation = this.buildReplaceOperation (parameters);
				break;
			case APPEND :
				operation = this.buildAppendOperation (parameters);
				break;
			case PREPEND :
				operation = this.buildPrependOperation (parameters);
				break;
			case CAS :
				operation = this.buildCasOperation (parameters);
				break;
			case GET :
				operation = this.buildGetOperation (parameters);
				break;
			case GET_BULK :
				operation = this.buildGetBulkOperation (parameters);
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
		return operation;
	}
	
	private IOperation<?> buildAddOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws ExecutionException,
						InterruptedException
			{
				final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
				final int exp = (Integer) parameters[1];
				final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.add (kvMessage.getKey (), exp, kvMessage.getData ());
				return opResult.get ();
			}
		});
	}
	
	private IOperation<?> buildAppendOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws ExecutionException,
						InterruptedException
			{
				final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
				final long cas = MemcachedOperationFactory.this.mcClient.gets (kvMessage.getKey ()).getCas ();
				final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.append (cas, kvMessage.getKey (), kvMessage.getData ());
				return opResult.get ();
			}
		});
	}
	
	private IOperation<?> buildCasOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws ExecutionException,
						InterruptedException
			{
				final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
				final long cas = MemcachedOperationFactory.this.mcClient.gets (kvMessage.getKey ()).getCas ();
				final Future<CASResponse> opResult = MemcachedOperationFactory.this.mcClient.asyncCAS (kvMessage.getKey (), cas, kvMessage.getData ());
				return (opResult.get () == CASResponse.OK);
			}
		});
	}
	
	private IOperation<?> buildDeleteOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws ExecutionException,
						InterruptedException
			{
				final String key = (String) parameters[0];
				final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.delete (key);
				return opResult.get ();
			}
		});
	}
	
	private IOperation<?> buildGetBulkOperation (final Object ... parameters)
	{
		return new GenericOperation<Map<String, KeyValueMessage>> (new Callable<Map<String, KeyValueMessage>> () {
			@Override
			public Map<String, KeyValueMessage> call ()
					throws ExecutionException,
						InterruptedException
			{
				final String[] keys = (String[]) parameters[0];
				final EncodingMetadata expectedEncoding = (EncodingMetadata) parameters[1];
				final Future<Map<String, Object>> opResult = MemcachedOperationFactory.this.mcClient.asyncGetBulk (keys);
				final Map<String, KeyValueMessage> result = new HashMap<String, KeyValueMessage> ();
				KeyValueMessage kvMessage = null;
				for (final Map.Entry<String, Object> entry : opResult.get ().entrySet ()) {
					kvMessage = null;
					kvMessage = new KeyValueMessage (entry.getKey (), (byte[]) entry.getValue (), expectedEncoding.getContentEncoding (), expectedEncoding.getContentType ());
					result.put (entry.getKey (), kvMessage);
				}
				return result;
			}
		});
	}
	
	private IOperation<?> buildGetOperation (final Object ... parameters)
	{
		return new GenericOperation<KeyValueMessage> (new Callable<KeyValueMessage> () {
			@Override
			public KeyValueMessage call ()
					throws ExecutionException,
						InterruptedException
			{
				final String key = (String) parameters[0];
				final EncodingMetadata expectedEncoding = (EncodingMetadata) parameters[1];
				final Future<Object> opResult = MemcachedOperationFactory.this.mcClient.asyncGet (key);
				final byte[] data = (byte[]) opResult.get ();
				KeyValueMessage kvMessage = null;
				kvMessage = new KeyValueMessage (key, data, expectedEncoding.getContentEncoding (), expectedEncoding.getContentType ());
				return kvMessage;
			}
		});
	}
	
	private IOperation<?> buildPrependOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws ExecutionException,
						InterruptedException
			{
				final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
				final long cas = MemcachedOperationFactory.this.mcClient.gets (kvMessage.getKey ()).getCas ();
				final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.prepend (cas, kvMessage.getKey (), kvMessage.getData ());
				return opResult.get ();
			}
		});
	}
	
	private IOperation<?> buildReplaceOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws ExecutionException,
						InterruptedException
			{
				final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
				final int exp = (Integer) parameters[1];
				final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.replace (kvMessage.getKey (), exp, kvMessage.getData ());
				return opResult.get ();
			}
		});
	}
	
	private IOperation<?> buildSetOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
					throws ExecutionException,
						InterruptedException
			{
				final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
				final String key = kvMessage.getKey ();
				int exp = 0;
				final byte[] data = kvMessage.getData ();
				if (parameters.length == 2) {
					exp = (Integer) parameters[1];
				}
				final Future<Boolean> opResult = MemcachedOperationFactory.this.mcClient.set (key, exp, data);
				return opResult.get ();
			}
		});
	}
	
	/**
	 * Creates a new factory.
	 * 
	 * @param hosts
	 *            the hostname and port of the Memcached servers
	 * @param user
	 *            the username for connecting to the server
	 * @param passwd
	 *            the password for connecting to the server
	 * @param bucket
	 *            the bucket where all operations are applied
	 * @param useBucket
	 *            whether to connect to the specified bucket or to the default
	 * @return the factory
	 */
	public static MemcachedOperationFactory getFactory (final List<?> hosts, final String user, final String password, final String bucket, final boolean useBucket)
	{
		try {
			return new MemcachedOperationFactory (hosts, user, password, bucket, useBucket);
		} catch (final IOException e) {
			FallbackExceptionTracer.defaultInstance.traceIgnoredException (e);
		}
		return null;
	}
	
	private final MemcachedClient mcClient;
}
