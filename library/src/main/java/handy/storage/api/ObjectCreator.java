package handy.storage.api;

/**
 * Creates object from its fields values. Use
 * {@link handy.storage.HandyStorage.Builder#setObjectCreator(Class, ObjectCreator)} to register
 * your custom objects creators.
 *
 * @param <T> model class
 */
public interface ObjectCreator<T> {

	/**
	 * Creates an object corresponding to the values read from a cursor
	 *
	 * @param values values read from a cursor
	 * @return an objects corresponding to the <code>values</code>
	 */
	T createObject(CursorValues values);

}
