package mosaic.cloudlet.core;

/**
 * Base class for clouldet callback arguments. This will hold a reference to the
 * cloudlet controller and operation specific information.
 * 
 * @author Georgiana Macariu
 * 
 * @param <S>
 *            the type of the state of the cloudlet
 */
public class CallbackArguments<S> {
	private ICloudletController<S> cloudlet;

	/**
	 * Creates a new argument
	 * 
	 * @param cloudlet
	 *            the cloudlet controller
	 */
	public CallbackArguments(ICloudletController<S> cloudlet) {
		super();
		this.cloudlet = cloudlet;
	}

	/**
	 * Returns the cloudlet controller.
	 * 
	 * @return the cloudlet controller
	 */
	public ICloudletController<S> getCloudlet() {
		return cloudlet;
	}
}
