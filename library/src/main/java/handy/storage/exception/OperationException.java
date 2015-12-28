package handy.storage.exception;

/**
 * Indicates an exception during performing an operation with the database.
 */
public class OperationException extends Exception {

	private static final long serialVersionUID = -3364170177634611796L;

	public OperationException(Throwable e) {
		super(e);
	}

	public OperationException(String message) {
		super(message);
	}

}
