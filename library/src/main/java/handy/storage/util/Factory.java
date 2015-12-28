package handy.storage.util;

/**
 * Factory for objects of some type.
 *
 * @param <T> type of objects to produce
 */
public interface Factory<T> {
	T newObject();
}
