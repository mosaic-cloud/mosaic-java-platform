package mosaic.cloudlet.resources.kvstore;

import java.util.ArrayList;
import java.util.List;

import mosaic.cloudlet.core.CallbackArguments;
import mosaic.cloudlet.core.ICloudletController;

/**
 * The arguments of the cloudlet callback methods for the operations on
 * key-value storages.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the state of the cloudlet
 */
public class KeyValueCallbackArguments<S> extends CallbackArguments<S> {
	private final List<String> keys;
	private final Object value;

	/**
	 * Creates a new argument
	 * 
	 * @param cloudlet
	 *            the cloudlet
	 * @param key
	 *            the key used in the operation
	 * @param value
	 *            the value associated with the key (if this callback is used
	 *            for failed operations this value should contain the error)
	 */
	public KeyValueCallbackArguments(ICloudletController<S> cloudlet,
			String key, Object value) {
		super(cloudlet);
		this.keys = new ArrayList<String>();
		this.keys.add(key);
		this.value = value;
	}

	/**
	 * Creates a new argument for the callbacks of operations using more than
	 * one key.
	 * 
	 * @param cloudlet
	 *            the cloudlet
	 * @param keys
	 *            the keys used in the operation
	 * @param value
	 *            the value associated with the key (if this callback is used
	 *            for failed operations this value should contain the error)
	 */
	public KeyValueCallbackArguments(ICloudletController<S> cloudlet,
			List<String> keys, Object value) {
		super(cloudlet);
		this.keys = keys;
		this.value = value;
	}

	/**
	 * Returns the value field of the argument.
	 * 
	 * @return the value field of the argument
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Returns the key used in single-key operations.
	 * 
	 * @return the key used in single-key operations
	 */
	public String getKey() {
		return this.keys.get(0);
	}

	/**
	 * Returns the keys used in multiple-key operations.
	 * 
	 * @return the key used in multiple-key operations
	 */
	public List<String> getKeys() {
		return this.keys;
	}

}
