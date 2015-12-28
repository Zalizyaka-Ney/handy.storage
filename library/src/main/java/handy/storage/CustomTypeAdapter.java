package handy.storage;

import android.content.ContentValues;
import android.database.Cursor;

import handy.storage.api.ColumnType;

/**
 * Manages storing values of type <code>T</code> in the database. To store values in
 * the database they are converted to strings and during reading from the
 * database values should be parsed from strings.
 *
 * @param <T> type
 */
public abstract class CustomTypeAdapter<T> extends TypeAdapter<T> {

	@Override
	protected void putValue(ContentValues cv, String key, T value) {
		cv.put(key, valueToString(value));
	}

	@Override
	protected T getValue(Cursor cursor, int columnIndex) {
		String s = cursor.getString(columnIndex);
		return parseValue(s);
	}

	@Override
	protected ColumnType getColumnType() {
		return ColumnType.TEXT;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String convertValue(Object value) {
		try {
			return valueToString((T) value);
		} catch (ClassCastException e) {
			return super.convertValue(value);
		}
	}

	/**
	 * Converts the <code>value</code> to String.
	 *
	 * @param value value
	 */
	protected abstract String valueToString(T value);

	/**
	 * Parse a stored value from String/
	 *
	 * @param s stored value (result of {@link #valueToString(Object)} method)
	 */
	protected abstract T parseValue(String s);

}
