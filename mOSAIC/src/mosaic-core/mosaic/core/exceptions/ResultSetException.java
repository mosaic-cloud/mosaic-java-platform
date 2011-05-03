package mosaic.core.exceptions;

/**
 * Exception thrown when a the result of an operation cannot be set.
 * 
 * @author Georgiana Macariu
 * 
 */
public class ResultSetException extends Exception {

	private static final long serialVersionUID = 3353582009957410019L;

	public ResultSetException() {
		super();
	}

	public ResultSetException(String message) {
		super(message);
	}

	public ResultSetException(Throwable message) {
		super(message);
	}

	public ResultSetException(String message, Throwable exception) {
		super(message, exception);
	}

}
