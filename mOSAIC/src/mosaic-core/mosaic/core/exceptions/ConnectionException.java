package mosaic.core.exceptions;

/**
 * Exception thrown when a queue-based connection cannot be set.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ConnectionException extends Exception {

	private static final long serialVersionUID = 3353582009957410019L;

	public ConnectionException() {
		super();
	}

	public ConnectionException(String message) {
		super(message);
	}

	public ConnectionException(Throwable message) {
		super(message);
	}

	public ConnectionException(String message, Throwable exception) {
		super(message, exception);
	}

}
