package mosaic.connector.kvstore;

import java.util.List;
import java.util.Map;

import mosaic.core.ops.IOperationCompletionHandler;
import mosaic.core.ops.IResult;

/**
 * Interface for working with key-value stores.
 * 
 * @author Georgiana Macariu
 * 
 */
public interface IMemcachedStore {

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
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Boolean> set(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers);

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
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Boolean> add(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers);

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
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Boolean> replace(String key, int exp, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers);

	/**
	 * Adds specified data to an existing key after existing data.
	 * 
	 * @param key
	 *            the key associated with the stored data
	 * @param data
	 *            the appended data
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Boolean> append(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers);

	/**
	 * Adds specified data to an existing key before existing data.
	 * 
	 * @param key
	 *            the key associated with the stored data
	 * @param data
	 *            the pre-appended data
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Boolean> prepend(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers);

	/**
	 * Stores specified data but only if no one else has updated since I last
	 * fetched it.
	 * 
	 * @param key
	 *            the key associated with the stored data
	 * @param data
	 *            the data
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Boolean> cas(String key, Object data,
			List<IOperationCompletionHandler<Boolean>> handlers);

	/**
	 * Gets data associated with a single key.
	 * 
	 * @param key
	 *            the key
	 * @return a result handle for the operation
	 */
	IResult<Object> get(String key,
			List<IOperationCompletionHandler<Object>> handlers);

	/**
	 * Gets data associated with several keys.
	 * 
	 * @param keys
	 *            the keys
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Map<String, Object>> getBulk(List<String> keys,
			List<IOperationCompletionHandler<Map<String, Object>>> handlers);

	/**
	 * Deletes the given key.
	 * 
	 * @param key
	 *            the key to delete
	 * @param handlers
	 *            a set of handlers which shall be called when the operation
	 *            completes
	 * @return a result handle for the operation
	 */
	IResult<Boolean> delete(String key,
			List<IOperationCompletionHandler<Boolean>> handlers);
}
