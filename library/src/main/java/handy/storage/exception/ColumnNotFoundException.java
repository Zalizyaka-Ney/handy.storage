package handy.storage.exception;

/**
 * Indicates that a requested column was not found in a cursor.
 */
public class ColumnNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1772608499330500339L;

	public ColumnNotFoundException(String message) {
		super(message);
	}

}
