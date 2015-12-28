package handy.storage.api;

/**
 * Action to perform on delete or update of a referenced object.
 */
public enum Action {

	// XXX: The "SET DEFAULT" action is omitted because we don't support default values.

	/**
	 * Default SQLite action. Almost the same as {@link Action#RESTRICT}.
	 */
	DEFAULT("NO ACTION"),

	/**
	 * Means that the application is prohibited from deleting or modifying a
	 * parent key when there exists one or more child keys mapped to it.
	 */
	RESTRICT("RESTRICT"),

	/**
	 * When a parent key is deleted or modified, the child key columns of all
	 * rows in the child table that mapped to the parent key are set to contain
	 * SQL NULL values.
	 */
	SET_NULL("SET NULL"),

	/**
	 * This propagates the delete or update operation on the parent key to each
	 * dependent child key. For an "ON DELETE CASCADE" action, this means that
	 * each row in the child table that was associated with the deleted parent
	 * row is also deleted. For an "ON UPDATE CASCADE" action, it means that the
	 * values stored in each dependent child key are modified to match the new
	 * parent key values.
	 */
	CASCADE("CASCADE");

	private final String sql;

	Action(String sql) {
		this.sql = sql;
	}

	/**
	 * converts this action to SQLite's syntax.
	 */
	public String toSqliteSyntax() {
		return sql;
	}

	/**
	 * Whether this action is default.
	 */
	public boolean isDefault() {
		return DEFAULT == this;
	}

}
