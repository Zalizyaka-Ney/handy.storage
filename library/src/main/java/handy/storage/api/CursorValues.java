package handy.storage.api;

/**
 * A bundle of values read from a cursor.
 */
public interface CursorValues {

	/**
	 * Returns a value of the column read from a cursor. Has the same type as
	 * the column's field.
	 *
	 * @param column name of column
	 * @param <T>    type of value (wrong type will cause a ClassCastException)
	 * @return value of the column read from a cursor
	 */
	<T> T getValue(String column);

}
