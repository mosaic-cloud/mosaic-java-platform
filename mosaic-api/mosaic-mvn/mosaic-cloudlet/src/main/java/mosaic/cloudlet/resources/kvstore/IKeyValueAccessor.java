package mosaic.cloudlet.resources.kvstore;

import java.util.List;

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
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> set(String key, Object value, Object extra);

	/**
	 * Gets data associated with a single key.
	 * 
	 * @param key
	 *            the key
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Object> get(String key, Object extra);

	/**
	 * Deletes the given key.
	 * 
	 * @param key
	 *            the key to delete
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<Boolean> delete(String key, Object extra);

	/**
	 * Lists the keys in the bucket associated with the accessor.
	 * 
	 * @param extra
	 *            some application specific data
	 * @return a result handle for the operation
	 */
	IResult<List<String>> list(Object extra);
}
