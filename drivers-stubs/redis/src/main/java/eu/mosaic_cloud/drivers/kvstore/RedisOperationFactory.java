/*
 * #%L
 * mosaic-drivers-stubs-redis
 * %%
 * Copyright (C) 2010 - 2012 Institute e-Austria Timisoara (Romania)
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
import java.util.Set;
import java.util.concurrent.Callable;

import eu.mosaic_cloud.platform.core.ops.GenericOperation;
import eu.mosaic_cloud.platform.core.ops.IOperation;
import eu.mosaic_cloud.platform.core.ops.IOperationFactory;
import eu.mosaic_cloud.platform.core.ops.IOperationType;
import eu.mosaic_cloud.platform.interop.common.kv.KeyValueMessage;

import redis.clients.jedis.Jedis;
import redis.clients.util.SafeEncoder;


/**
 * Factory class which builds the asynchronous calls for the operations defined
 * on the Redis key-value store.
 * 
 * @author Georgiana Macariu
 * 
 */
public final class RedisOperationFactory
		implements
			IOperationFactory
{
	private RedisOperationFactory (final String host, final int port, final String passwd, final String bucket)
	{
		super ();
		this.redisClient = new Jedis (host, port, 0);
		if (!"".equals (passwd)) { // $NON-NLS-1$
			this.redisClient.auth (passwd);
		}
		final int iBucket = Integer.parseInt (bucket);
		if (iBucket > -1) {
			this.redisClient.select (iBucket);
		}
		this.redisClient.connect ();
	}
	
	@Override
	public void destroy ()
	{
		this.redisClient.disconnect ();
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
		return operation;
	}
	
	private IOperation<?> buildDeleteOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
			{
				final byte[] keyBytes = SafeEncoder.encode ((String) parameters[0]);
				final long opResult = RedisOperationFactory.this.redisClient.del (keyBytes);
				if (opResult == 0) {
					return false;
				}
				return true;
			}
		});
	}
	
	private IOperation<?> buildGetOperation (final Object ... parameters)
	{
		return new GenericOperation<KeyValueMessage> (new Callable<KeyValueMessage> () {
			@Override
			public KeyValueMessage call ()
			{
				final String key = (String) parameters[0];
				final byte[] keyBytes = SafeEncoder.encode (key);
				final byte[] result = RedisOperationFactory.this.redisClient.get (keyBytes);
				KeyValueMessage kvMessage = null;
				if (null != result) {
					kvMessage = new KeyValueMessage (key, result, RedisDriver.REDIS_DEFAULT_CONTENT_TYPE, RedisDriver.REDIS_DEFAULT_CONTENT_ENCODING);
				}
				return kvMessage;
			}
		});
	}
	
	private IOperation<?> buildListOperation ()
	{
		return new GenericOperation<List<String>> (new Callable<List<String>> () {
			@Override
			public List<String> call ()
			{
				final Set<String> opResult = RedisOperationFactory.this.redisClient.keys ("*");
				final List<String> result = new ArrayList<String> ();
				for (final String key : opResult) {
					result.add (key);
				}
				return result;
			}
		});
	}
	
	private IOperation<?> buildSetOperation (final Object ... parameters)
	{
		return new GenericOperation<Boolean> (new Callable<Boolean> () {
			@Override
			public Boolean call ()
			{
				final KeyValueMessage kvMessage = (KeyValueMessage) parameters[0];
				final byte[] keyBytes = SafeEncoder.encode ((String) kvMessage.getKey ());
				final byte[] dataBytes = kvMessage.getData ();
				String opResult = RedisOperationFactory.this.redisClient.set (keyBytes, dataBytes);
				opResult = opResult.trim ();
				if (opResult.equalsIgnoreCase ("OK")) {
					return true;
				}
				return false;
			}
		});
	}
	
	/**
	 * Creates a new factory.
	 * 
	 * @param host
	 *            the hostname of the Redis server
	 * @param port
	 *            the port for the Redis server
	 * @param user
	 *            the username for connecting to the server
	 * @param passwd
	 *            the password for connecting to the serve
	 * @param bucket
	 *            the bucket where all operations are applied
	 * @return the factory
	 */
	public static RedisOperationFactory getFactory (final String host, final int port, final String passwd, final String bucket)
	{
		return new RedisOperationFactory (host, port, passwd, bucket);
	}
	
	private final Jedis redisClient;
}
