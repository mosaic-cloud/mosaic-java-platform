package mosaic.cloudlet.resources.kvstore;

import mosaic.cloudlet.resources.IResourceAccessor;
import mosaic.core.ops.IResult;

/**
 * Basic interface for cloudlets to access key-value storages.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet
 */
public interface IKeyValueAccessor<S> extends IResourceAccessor<S> {
	/**
	 * Stores the given data and associates it with the specified key.
	 * 
	 * @param key
	 *            the key under which this data should be stored
	 * @param data
	 *            the data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> set(String key, Object value);

	/**
	 * Gets data associated with a single key.
	 * 
	 * @param key
	 *            the key
	 * 
	 * @return a result handle for the operation
	 */
	IResult<Object> get(String key);

	/**
	 * Deletes the given key.
	 * 
	 * @param key
	 *            the key to delete
	 * @return a result handle for the operation
	 */
	IResult<Boolean> delete(String key);
}
