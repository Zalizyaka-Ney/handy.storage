package handy.storage;

import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import handy.storage.api.Model;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.QueryParams;
import handy.storage.exception.OperationException;
import handy.storage.log.PerformanceTimer;
import handy.storage.util.ReflectionUtils;

/**
 * Reads all data from some source and accumulates it via {@link DataCollector}.
 */
abstract class DataReader {

	abstract void readData(DataCollector dataCollector) throws OperationException;

	/**
	 * Base implementation.
	 */
	private abstract static class BaseDataReader extends DataReader {

		private final DatabaseAdapter databaseAdapter;
		private final QueryParams queryParams;

		protected BaseDataReader(DatabaseAdapter databaseAdapter, QueryParams queryParams) {
			this.databaseAdapter = databaseAdapter;
			this.queryParams = queryParams;
		}

		@Override
		final void readData(DataCollector dataCollector) throws OperationException {
			PerformanceTimer.startInterval("read data");
			// XXX: the query is wrapped into a transaction, because it might do additional queries to the database
			DatabaseAdapter.TransactionControl transaction = databaseAdapter.startTransaction();
			Cursor cursor = null;
			try {
				cursor = databaseAdapter.performQuery(queryParams);
				acceptData(cursor, dataCollector);
				transaction.setSuccessful();
			} finally {
				if (cursor != null) {
					cursor.close();
				}
				transaction.end();
			}
			PerformanceTimer.endInterval();
		}

		abstract void acceptData(Cursor cursor, DataCollector dataCollector) throws OperationException;
	}

	/**
	 * Implementation that reads complete model objects.
	 */
	static class ModelDataReader extends BaseDataReader {

		private final DatabaseCore databaseCore;
		private final CursorReader cursorReader;
		private final Map<ColumnInfo, Integer> indexes;

		ModelDataReader(
			DatabaseCore databaseCore,
			DatabaseAdapter databaseAdapter,
			CursorReader cursorReader,
			QueryParams queryParams,
			Map<ColumnInfo, Integer> indexes) {

			super(databaseAdapter, queryParams);
			this.databaseCore = databaseCore;
			this.cursorReader = cursorReader;
			this.indexes = indexes;
		}

		@Override
		void acceptData(Cursor cursor, DataCollector dataCollector) throws OperationException {
			ReferencedObjectsBundle referencedObjectsBundle = readReferencedObjects(cursor);
			cursorReader.readData(cursor, indexes, referencedObjectsBundle, dataCollector);
		}

		private ReferencedObjectsBundle readReferencedObjects(Cursor cursor) throws OperationException {
			Collection<ReferencedObjectsReader> referenceReaders = createReferencedObjectsReaders();
			ReferencedObjectsBundle bundle = new ReferencedObjectsBundle();
			if (!referenceReaders.isEmpty() && cursor.moveToFirst()) {
				do {
					for (ReferencedObjectsReader reader : referenceReaders) {
						reader.readKeyValues(cursor);
					}
				} while (cursor.moveToNext());
				for (ReferencedObjectsReader reader : referenceReaders) {
					reader.readReferencedObjects(bundle);
				}
			}
			return bundle;
		}

		private Collection<ReferencedObjectsReader> createReferencedObjectsReaders() {
			Map<Class<?>, ReferencedObjectsReader> readers = new HashMap<>();
			for (ColumnInfo column : indexes.keySet()) {
				if (column.isReferenceToTable()) {
					Class<?> referencedClass = column.getReferencedTable();
					ReferencedObjectsReader reader = readers.get(referencedClass);
					if (reader == null) {
						reader = new ReferencedObjectsReader(databaseCore, column, indexes.get(column));
						readers.put(referencedClass, reader);
					} else {
						reader.addColumnIndex(indexes.get(column));
					}
				}
			}
			return readers.values();
		}
	}

	/**
	 * Reads referenced objects of one type.
	 */
	private static class ReferencedObjectsReader {

		private final DatabaseCore core;
		private final List<Integer> columnIndexes = new LinkedList<>();
		private final Set<Object> keyValues = new HashSet<>();
		private final TypeAdapter<?> typeAdapter;
		private final ColumnInfo.ReferenceInfo reference;

		ReferencedObjectsReader(DatabaseCore core, ColumnInfo column, Integer columnIndex) {
			this.core = core;
			columnIndexes.add(columnIndex);
			typeAdapter = core.getDataAdapters().getTypeAdapter(column.getFieldType());
			reference = column.getReference();
		}

		void addColumnIndex(Integer columnIndex) {
			columnIndexes.add(columnIndex);
		}

		void readKeyValues(Cursor cursor) {
			for (int index : columnIndexes) {
				if (!cursor.isNull(index)) {
					keyValues.add(typeAdapter.getValue(cursor, index));
				}
			}
		}

		void readReferencedObjects(ReferencedObjectsBundle bundle) throws OperationException {
			if (!keyValues.isEmpty()) {
				Class<? extends Model> referencedClass = reference.getModelClass();

				WritableTable<?> table = core.getTablesFactory().createTable(referencedClass);
				List<?> objects = table.select().where(reference.getForeignColumn().getName()).in(keyValues).execute();
				Field keyField = reference.getForeignColumn().getField();
				for (Object object : objects) {
					bundle.put(referencedClass, ReflectionUtils.getFieldValue(keyField, object), object);
				}
			}
		}

	}

	/**
	 * Implementation that reads values of one column (doesn't work with references).
	 */
	static class ColumnDataReader extends BaseDataReader {

		private final TypeAdapter<?> typeAdapter;
		private final Object defaultValue;
		private final String columnName;

		protected ColumnDataReader(DatabaseAdapter databaseAdapter, QueryParams queryParams, TypeAdapter<?> typeAdapter, Object defaultValue, String columnName) {
			super(databaseAdapter, queryParams);
			this.typeAdapter = typeAdapter;
			this.columnName = columnName;
			this.defaultValue = defaultValue;
		}

		@Override
		void acceptData(Cursor cursor, DataCollector dataCollector) throws OperationException {
			try {
				if (!cursor.moveToFirst()) {
					dataCollector.init(0);
					return;
				}
				dataCollector.init(cursor.getCount());
				do {
					Object value = cursor.isNull(0) ? defaultValue : typeAdapter.getValue(cursor, 0);
					CursorValuesImpl cursorValues = new CursorValuesImpl();
					cursorValues.addValue(columnName, value);
					dataCollector.accept(cursorValues);
				} while (cursor.moveToNext());
			} finally {
				cursor.close();
			}
		}
	}
}
