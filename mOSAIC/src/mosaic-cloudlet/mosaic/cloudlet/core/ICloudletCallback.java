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
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void initialize(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is successfully initialized.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void initializeSucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is unsuccessfully initialized.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void initializeFailed(S state, CallbackArguments<S> arguments);

	/**
	 * Destrozs the user cloudlet.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void destroy(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is successfully destroyed.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void destroySucceeded(S state, CallbackArguments<S> arguments);

	/**
	 * Operation called after the cloudlet is unsuccessfully destroyed.
	 * 
	 * @param state
	 *            the state of the cloudlet
	 * @param arguments
	 *            here, this argument just gives access to the cloudlet
	 *            controller
	 */
	public void destroyFailed(S state, CallbackArguments<S> arguments);

}
