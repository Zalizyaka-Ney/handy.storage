package handy.storage.api;

import com.google.gson.Gson;

import handy.storage.CustomTypeAdapter;
import handy.storage.annotation.GsonSerializable;
import handy.storage.exception.ObjectCreationException;

/**
 * Serializer for fields whose value could be an instance of sub-class of fields' type.
 * Do the same as {@link GsonSerializable} annotation, but it is type safe.
 *
 * @param <T> base objects type
 */
public class TypeSafeGsonAdapter<T> extends CustomTypeAdapter<T> {

	private static final char DELIMITER = '/';
	private final Gson gson;

	public TypeSafeGsonAdapter() {
		this(new Gson());
	}

	public TypeSafeGsonAdapter(Gson gson) {
		this.gson = gson;
	}

	@Override
	protected String valueToString(T value) {
		String objectClassName = value.getClass().getName();
		String objectJsonValue = gson.toJson(value);
		return objectClassName + DELIMITER + objectJsonValue;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T parseValue(String s) {
		int pos = s.indexOf(DELIMITER);
		if (pos < 0) {
			throw new ObjectCreationException("Wrong data format");
		}
		String objectClassName = s.substring(0, pos);
		try {
			Class<?> objectClass = Class.forName(objectClassName);
			String objectJsonValue = s.substring(pos + 1);
			return (T) gson.fromJson(objectJsonValue, objectClass);
		} catch (Exception e) {
			throw new ObjectCreationException(e.getMessage());
		}
	}

}
