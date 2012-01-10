/*
 * #%L
 * mosaic-cloudlet
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
package eu.mosaic_cloud.cloudlet.resources.kvstore;

import java.util.List;
import java.util.Map;

import eu.mosaic_cloud.core.ops.IResult;


/**
 * Basic interface for cloudlets to access memcached-based key-value storages.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet
 */
public interface IMemcachedAccessor<S> extends IKeyValueAccessor<S> {
	/**
	 * Stores the given data and associates it with the specified key.
	 * 
	 * @param key
	 *            the key under which this data should be stored
	 * @param exp
	 *            the expiration of this object. This is passed along exactly as
	 *            given, and will be processed per the memcached protocol
	 *            specification. The actual value sent may either be Unix time
	 *            (number of seconds since January 1, 1970, as a 32-bit value),
	 *            or a number of seconds starting from current time. In the
	 *            latter case, this number of seconds may not exceed 60*60*24*30
	 *            (number of seconds in 30 days); if the number sent by a client
	 *            is larger than that, the server will consider it to be real
	 *            Unix time value rather than an offset from current time.
	 * @param data
	 *            the data
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> set(String key, Object value, int exp, Object extra);

	/**
	 * Stores specified data, but only if the server *doesn't* already hold data
	 * for given key.
	 * 
	 * @param key
	 *            the key to associate with the stored data
	 * @param exp
	 *            the expiration of this object. This is passed along exactly as
	 *            given, and will be processed per the memcached protocol
	 *            specification. The actual value sent may either be Unix time
	 *            (number of seconds since January 1, 1970, as a 32-bit value),
	 *            or a number of seconds starting from current time. In the
	 *            latter case, this number of seconds may not exceed 60*60*24*30
	 *            (number of seconds in 30 days); if the number sent by a client
	 *            is larger than that, the server will consider it to be real
	 *            Unix time value rather than an offset from current time.
	 * @param data
	 *            the data
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> add(String key, Object value, int exp, Object extra);

	/**
	 * Adds specified data to an existing key after existing data.
	 * 
	 * @param key
	 *            the key associated with the stored data
	 * @param data
	 *            the appended data
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> append(String key, Object value, Object extra);

	/**
	 * Adds specified data to an existing key before existing data.
	 * 
	 * @param key
	 *            the key associated with the stored data
	 * @param data
	 *            the pre-appended data
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> prepend(String key, Object value, Object extra);

	/**
	 * Stores specified data but only if no one else has updated since I last
	 * fetched it.
	 * 
	 * @param key
	 *            the key associated with the stored data
	 * @param data
	 *            the data
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> cas(String key, Object value, Object extra);

	/**
	 * Stores specified data, but only if the server *does* already hold data
	 * for given key.
	 * 
	 * @param key
	 *            the key associated with the stored data
	 * @param exp
	 *            the expiration of this object. This is passed along exactly as
	 *            given, and will be processed per the memcached protocol
	 *            specification. The actual value sent may either be Unix time
	 *            (number of seconds since January 1, 1970, as a 32-bit value),
	 *            or a number of seconds starting from current time. In the
	 *            latter case, this number of seconds may not exceed 60*60*24*30
	 *            (number of seconds in 30 days); if the number sent by a client
	 *            is larger than that, the server will consider it to be real
	 *            Unix time value rather than an offset from current time.
	 * @param data
	 *            the data
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> replace(String key, Object value, int exp, Object extra);

	/**
	 * Gets data associated with several keys.
	 * 
	 * @param keys
	 *            the keys
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Map<String, Object>> getBulk(List<String> keys, Object extra);
}
