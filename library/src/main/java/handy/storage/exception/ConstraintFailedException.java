package handy.storage.exception;


/**
 * Indicates that an operation failed due to a database schema constraint.
 */
public class ConstraintFailedException extends OperationException {

	private static final long serialVersionUID = -1868936351276145251L;

	public ConstraintFailedException(Throwable e) {
		super(e);
	}

}
