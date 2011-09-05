package mosaic.cloudlet.resources;

/**
 * Interface for all resource accessors used by cloudlets.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the cloudlet state
 */
public interface IResourceAccessor<S> {
	/**
	 * Initialize the accessor.
	 * 
	 * @param callback
	 *            handler for callbacks received from the resource
	 * @param state
	 *            cloudlet state
	 */
	void initialize(IResourceAccessorCallback<S> callback, S state);

	/**
	 * Destroys the accessor.
	 * 
	 * @param callback
	 *            handler for callbacks received from the resource
	 */
	void destroy(IResourceAccessorCallback<S> callback);

	/**
	 * Returns the current status of the accessor.
	 * 
	 * @return the current status of the accessor
	 */
	ResourceStatus getStatus();
}
