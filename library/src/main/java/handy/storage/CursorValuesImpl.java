package handy.storage;

import java.util.HashMap;
import java.util.Map;

import handy.storage.api.CursorValues;

/**
 * Implementation of {@link CursorValues}
 */
class CursorValuesImpl implements CursorValues {

	private Map<String, Object> values = new HashMap<>();

	void addValue(String column, Object value) {
		values.put(column, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getValue(String column) {
		return (T) values.get(column);
	}

}
