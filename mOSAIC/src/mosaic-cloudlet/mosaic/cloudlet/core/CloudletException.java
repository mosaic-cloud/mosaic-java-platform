package mosaic.cloudlet.core;

/**
 * Defines a general exception a cloudlet can throw when it encounters
 * difficulty.
 * 
 * @author Georgiana Macariu
 * 
 */
public class CloudletException extends Exception {

	private static final long serialVersionUID = 3353582009957410019L;

	public CloudletException() {
		super();
	}

	public CloudletException(String message) {
		super(message);
	}

	public CloudletException(Throwable message) {
		super(message);
	}

	public CloudletException(String message, Throwable exception) {
		super(message, exception);
	}

}
