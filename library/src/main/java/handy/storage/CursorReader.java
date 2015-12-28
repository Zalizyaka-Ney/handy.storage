package handy.storage;

import android.database.Cursor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import handy.storage.api.CursorValues;
import handy.storage.exception.IllegalUsageException;
import handy.storage.log.PerformanceTimer;
import handy.storage.util.ClassCast;

/**
 * Reads data from a cursor and wraps it in {@link CursorValues}.
 */
class CursorReader {

	private final List<ColumnInfo> columns;
	private final DataAdapters dataAdapters;

	public CursorReader(List<ColumnInfo> columns, DataAdapters dataAdapters) {
		this.columns = columns;
		this.dataAdapters = dataAdapters;
	}

	void readData(Cursor cursor, Map<ColumnInfo, Integer> requestedIndexes, ReferencedObjectsBundle bundle, DataCollector dataCollector) {
		if (cursor == null || !cursor.moveToFirst()) {
			dataCollector.init(0);
			return;
		}
		Map<ColumnInfo, Integer> indexes = requestedIndexes;
		if (indexes == null) {
			indexes = new HashMap<>();
			for (ColumnInfo column : columns) {
				int index = cursor.getColumnIndex(column.getName());
				if (index == -1) {
					throw new IllegalUsageException("the cursor doesn't have a column with name " + column.getName());
				}
				indexes.put(column, index);
			}
		}
		PerformanceTimer.startInterval("parse models from cursor");

		cursor.moveToFirst();
		dataCollector.init(cursor.getCount());
		do {
			CursorValues values = readValues(cursor, indexes, bundle);
			dataCollector.accept(values);
		} while (cursor.moveToNext());
		PerformanceTimer.endInterval("parsed " + dataCollector.getSize() + " models");
	}

	private CursorValues readValues(Cursor cursor, Map<ColumnInfo, Integer> columnIndexes, ReferencedObjectsBundle bundle) {
		CursorValuesImpl values = new CursorValuesImpl();
		for (ColumnInfo column : columns) {
			Integer columnIndex = columnIndexes.get(column);
			Class<?> fieldType = column.getFieldType();
			if (columnIndex == null || fieldType == null) {
				continue;
			}
			String columnName = column.getName();
			if (cursor.isNull(columnIndex)) {
				values.addValue(columnName, column.isReferenceToTable() ? null : ClassCast.getDefaultValueForType(fieldType));
			} else {
				Object value;
				TypeAdapter<?> typeAdapter = dataAdapters.getTypeAdapter(fieldType);
				Object cursorValue = typeAdapter.getValue(cursor, columnIndex);
				if (column.isReferenceToTable()) {
					value = bundle.get(column.getReference().getModelClass(), cursorValue);
				} else {
					value = cursorValue;
				}
				values.addValue(columnName, value);
			}
		}
		return values;
	}

}
