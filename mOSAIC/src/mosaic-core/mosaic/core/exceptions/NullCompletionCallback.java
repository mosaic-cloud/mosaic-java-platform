package mosaic.core.exceptions;

/**
 * Exception thrown when no operation completion callback is set.
 * 
 * @author Georgiana Macariu
 * 
 */
public class NullCompletionCallback extends Exception {

	private static final long serialVersionUID = -3388438945086356985L;

	public NullCompletionCallback() {
		super();
	}

	public NullCompletionCallback(String message) {
		super(message);
	}

	public NullCompletionCallback(Throwable message) {
		super(message);
	}

	public NullCompletionCallback(String message, Throwable exception) {
		super(message, exception);
	}

}
