package handy.storage;

import java.lang.reflect.Field;

import handy.storage.api.ColumnType;
import handy.storage.api.Value;
import handy.storage.log.DatabaseLog;
import handy.storage.util.ReflectionUtils;

/**
 * Converts column's names and values for database queries.
 */
class QueryAdapter {

	private final TableInfo tableInfo;
	private final DataAdapters dataAdapters;
	private final int tableHashCode;

	QueryAdapter(TableInfo tableInfo, DataAdapters dataAdapters, int tableHashCode) {
		this.tableInfo = tableInfo;
		this.dataAdapters = dataAdapters;
		this.tableHashCode = tableHashCode;
	}

	int getTableHashCode() {
		return tableHashCode;
	}

	String convertToDatabaseValue(String fullColumnName, Object value) {
		if (value != null && value instanceof Value) {
			return ((Value) value).getName();
		}
		ColumnInfo columnInfo = tableInfo.getColumnInfo(fullColumnName);
		if (columnInfo != null && columnInfo.getFieldType() != null) {
			Object databaseValue = value;
			if (columnInfo.isReferenceToTable()) {
				Field uniqueField = columnInfo.getReference().getForeignColumn().getField();
				databaseValue = ReflectionUtils.getFieldValue(uniqueField, value);
			}
			return convertValue(databaseValue, columnInfo.getFieldType());
		} else {
			DatabaseLog.d("covert a value of column '" + fullColumnName + "' relying on the type of the passed value");
			if (value != null) {
				Class<?> valueClass = value.getClass();
				if (dataAdapters.hasTypeAdapter(valueClass)) {
					return convertValue(value, valueClass);
				}
			}
			return String.valueOf(value);
		}
	}

	private String convertValue(Object value, Class<?> valueType) {
		TypeAdapter<?> typeAdapter = dataAdapters.getTypeAdapter(valueType);
		String convertedValue = typeAdapter.convertValue(value);
		if (typeAdapter.getColumnType() == ColumnType.TEXT) {
			convertedValue = wrapString(convertedValue);
		}
		return convertedValue;
	}

	static String wrapString(String value) {
		if (value == null) {
			return null;
		} else {
			return "'" + value.replaceAll("'", "''") + "'";
		}
	}

	String getFullColumnName(String column) {
		ColumnInfo columnInfo = tableInfo.getColumnInfo(column);
		if (columnInfo == null) {
			DatabaseLog.d("can not resolve a full name for column '" + column + "'");
			return column;
		} else {
			return columnInfo.getFullName();
		}
	}

}
