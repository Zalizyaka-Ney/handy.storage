package handy.storage.api;

/**
 * Result of SQL function.
 */
public class Result extends Value {

	protected Result(String entity) {
		super(entity);
	}

	/**
	 * Sets an alias.
	 *
	 * @return this object
	 */
	public Result as(String alias) {
		setAlias(alias);
		return this;
	}

	/**
	 * Creates a {@link Result} object representing a result of SQL function.
	 */
	public static Result of(Function function, String... arguments) {
		return new Result(function.toSQLEntity(arguments));
	}

	/**
	 * Creates a {@link Result} object representing a result of raw SQL function. If this function contains whitespaces,
	 * you might need to wrap the parameter in parentheses (for example, <code>"(name IS NULL)"</code>).
	 */
	public static Result of(String rawFunction) {
		return new Result(rawFunction);
	}

}
