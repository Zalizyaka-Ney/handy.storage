package handy.storage.exception;

/**
 * Indicates an exception during creation of a model object.
 */
public class ObjectCreationException extends RuntimeException {

	private static final long serialVersionUID = -612847346043855596L;

	public ObjectCreationException(String message) {
		super(message);
	}

}
