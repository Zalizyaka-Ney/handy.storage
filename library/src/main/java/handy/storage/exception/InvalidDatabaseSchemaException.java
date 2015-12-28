package handy.storage.exception;

/**
 * Indicates a logical error during creation of the database schema.
 */
public class InvalidDatabaseSchemaException extends RuntimeException {

	private static final long serialVersionUID = 8090803891422922927L;

	public InvalidDatabaseSchemaException(String detailedMessage) {
		super(detailedMessage);
	}

}
