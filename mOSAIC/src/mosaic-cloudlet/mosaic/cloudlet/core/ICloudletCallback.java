package mosaic.cloudlet.core;

/**
 * Main interface for user cloudlets. All user cloudlets must implement this
 * interface.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            The type of the object encoding the state of the cloudlet.
 */
public interface ICloudletCallback<S> extends ICallback {

	/**
	 * Initializes the user cloudlet.
	 * @param state
	 */
	public void initialize(S state, CallbackArguments<S> arguments);

	public void initializeSucceeded(S state, CallbackArguments<S> arguments);

	public void initializeFailed(S state, CallbackArguments<S> arguments);

	public void destroy(S state, CallbackArguments<S> arguments);

	public void destroySucceeded(S state, CallbackArguments<S> arguments);

	public void destroyFailed(S state, CallbackArguments<S> arguments);

}
