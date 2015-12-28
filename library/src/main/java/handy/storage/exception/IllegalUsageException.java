package handy.storage.exception;

/**
 * Indicates a usage of the framework in unsupported way (for example: calling a
 * method requiring a primary key on a table that doesn't have a primary key).
 */
public class IllegalUsageException extends RuntimeException {

	private static final long serialVersionUID = 51521691459179954L;

	public IllegalUsageException(String message) {
		super(message);
	}

}
