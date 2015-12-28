package handy.storage.exception;


/**
 * Indicates a failure during opening the database. Shouldn't appear normally.
 */
public class UnableToOpenDatabaseException extends OperationException {

	private static final long serialVersionUID = -7351045336669112830L;

	public UnableToOpenDatabaseException(Throwable e) {
		super(e);
	}

}
