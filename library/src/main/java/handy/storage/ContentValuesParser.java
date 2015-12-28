package handy.storage;

import android.content.ContentValues;

import java.lang.reflect.Field;

import handy.storage.ColumnInfo.ReferenceInfo;
import handy.storage.api.ColumnType;
import handy.storage.exception.IllegalUsageException;
import handy.storage.util.ReflectionUtils;

/**
 * Converts models to {@link ContentValues}.
 *
 * @param <T> model class
 */
class ContentValuesParser<T> {

	private final DataAdapters dataAdapters;
	private final TableInfo tableInfo;

	ContentValuesParser(DataAdapters dataAdapters, TableInfo tableInfo) {
		this.dataAdapters = dataAdapters;
		this.tableInfo = tableInfo;
	}

	ContentValues parseContentValues(T model) {
		ContentValues values = new ContentValues();
		for (ColumnInfo column : tableInfo.getColumns()) {
			if (!columnIsIdAndEmpty(model, column)) {
				putColumnValue(model, values, column);
			}

		}
		return values;
	}

	private void putColumnValue(T model, ContentValues values, ColumnInfo column) {
		Object value;
		Class<?> type = null;
		if (column.isReferenceToTable()) {
			ReferenceInfo ref = column.getReference();
			Field referencedField = ref.getForeignColumn().getField();
			Field field = column.getField();
			Object object = ReflectionUtils.getFieldValue(field, model);
			if (object == null) {
				value = null;
			} else {
				if (columnIsIdAndEmpty(object, ref.getForeignColumn())) {
					throw new IllegalUsageException("you should insert the referenced object first");
				}
				value = ReflectionUtils.getFieldValue(referencedField, object);
				type = referencedField.getType();
			}
		} else {
			Field field = column.getField();
			value = ReflectionUtils.getFieldValue(field, model);
			type = field.getType();
		}
		if (value == null) {
			values.putNull(column.getName());
		} else {
			TypeAdapter<?> typeAdapter = dataAdapters.getTypeAdapter(type);
			typeAdapter.putValueObject(values, column.getName(), value);
		}
	}

	private static boolean columnIsIdAndEmpty(Object object, ColumnInfo column) {
		Field field = column.getField();
		if (field != null && column.getType() == ColumnType.INTEGER && column.isAutoIncrementFlagSet() && column.isPrimaryKeyFlagSet()) {
			Object value = ReflectionUtils.getFieldValue(field, object);
			return value == null || (Long) value <= 0;
		} else {
			return false;
		}

	}

}
