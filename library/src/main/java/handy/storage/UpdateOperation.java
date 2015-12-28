package handy.storage;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import handy.storage.api.ColumnType;
import handy.storage.api.Update;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.DatabaseAdapter.TransactionControl;
import handy.storage.base.OnConflictStrategy;
import handy.storage.exception.ColumnNotFoundException;
import handy.storage.exception.IllegalUsageException;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;
import handy.storage.log.PerformanceTimer;
import handy.storage.util.ClassCast;

/**
 * Updates table rows.
 */
public class UpdateOperation extends BaseOperation<UpdateOperation> implements Update {

	private final TableInfo tableInfo;
	private final DataAdapters dataAdapters;
	private final Expression limitingExpression;

	private UpdateValues updateValues = new UpdateValues();

	UpdateOperation(ReadableTable<?> table) {
		this(table, null);
	}

	UpdateOperation(ReadableTable<?> table, Expression limitingExpression) {
		super(table);
		Table owner = getOwner();
		tableInfo = owner.getTableInfo();
		dataAdapters = owner.getDataAdapters();
		this.limitingExpression = limitingExpression;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Update setValue(String column, Object value) {
		updateValues.putValue(column, value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int execute() throws OperationException {
		return execute(OnConflictStrategy.DEFAULT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int execute(OnConflictStrategy onConflictStrategy) throws OperationException {
		if (updateValues.isEmpty()) {
			throw new IllegalUsageException("No columns were set to update");
		}
		PerformanceTimer.startInterval("update");
		DatabaseAdapter databaseAdapter = getDatabaseAdapter();
		int result = 0;
		Cursor cursor = null;
		TransactionControl transaction = databaseAdapter.startTransaction();
		try {
			String updateQuery = createUpdateQuery(onConflictStrategy);
			DatabaseLog.d(updateQuery);
			databaseAdapter.executeSql(updateQuery);
			cursor = databaseAdapter.rawQuery("SELECT CHANGES() FROM " + tableInfo.getEntity());
			if (cursor.moveToFirst()) {
				result = cursor.getInt(0);
			}
			transaction.setSuccessful();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			transaction.end();
			PerformanceTimer.endInterval();
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int executeSafely() {
		try {
			return execute();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int executeSafely(OnConflictStrategy onConflict) {
		try {
			return execute(onConflict);
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Update addValue(String column, Number value) {
		ColumnInfo columnInfo = findColumnInfo(column);
		Class<?> type = columnInfo.getFieldType();
		if (!ClassCast.isNumber(type)) {
			throw new IllegalArgumentException("type of column \"" + column + "\" is not a number class");
		}
		StringBuilder entity = new StringBuilder(column);
		if (ClassCast.isFractionalNumber(type)) {
			double doubleValue = value.doubleValue();
			entity.append(Math.signum(doubleValue) < 0 ? '-' : '+');
			entity.append(Math.abs(doubleValue));
		} else {
			long longValue = value.longValue();
			entity.append(Math.signum(longValue) < 0 ? '-' : '+');
			entity.append(Math.abs(longValue));
		}
		updateValues.putEntity(column, entity.toString());
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Update setEntity(String column, String entity) {
		updateValues.putEntity(column, entity);
		return this;
	}

	private String createUpdateQuery(OnConflictStrategy onConflictStrategy) {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE ");
		if (onConflictStrategy != OnConflictStrategy.DEFAULT) {
			sb.append("OR ");
			sb.append(onConflictStrategy.name());
			sb.append(' ');
		}
		sb.append(getOwner().getTableEntity());
		sb.append(" SET ");
		boolean firstValue = true;
		for (String column : updateValues.getColumns()) {
			if (firstValue) {
				firstValue = false;
			} else {
				sb.append(", ");
			}
			sb.append(column);
			sb.append(" = ");
			sb.append(updateValues.getNewValue(column));
		}
		String where = getWhereClause();
		if (!TextUtils.isEmpty(where)) {
			sb.append(" WHERE ");
			sb.append(where);
		}
		return sb.toString();
	}

	private ColumnInfo findColumnInfo(String column) {
		ColumnInfo columnInfo = tableInfo.getColumnInfo(column);
		if (columnInfo == null) {
			throw new ColumnNotFoundException("there is no column \"" + column + "\" in table \"" + getOwner().getTableEntity() + "\"");
		}
		return columnInfo;
	}


	@Override
	protected Expression getWhereExpression() {
		Expression superSelection = super.getWhereExpression();
		if (limitingExpression == null) {
			return superSelection;
		} else {
			return superSelection == null
				? limitingExpression
				: limitingExpression.and(superSelection);
		}
	}


	/**
	 * Encapsulates update parameters.
	 */
	private class UpdateValues {
		private final Map<String, Boolean> entitiesMap = new HashMap<>();
		private final ContentValues contentValues = new ContentValues();

		void putValue(String column, Object value) {
			ColumnInfo columnInfo = findColumnInfo(column);
			if (columnInfo.getType() == ColumnType.BLOB) {
				throw new RuntimeException("Sorry, updating BLOB columns is not supported");
			}
			Class<?> type = columnInfo.getFieldType();
			TypeAdapter<?> typeAdapter = dataAdapters.getTypeAdapter(type);
			typeAdapter.putValueObject(contentValues, columnInfo.getName(), ClassCast.castObject(value, type));
			entitiesMap.put(column, Boolean.FALSE);
		}

		void putEntity(String column, String entity) {
			findColumnInfo(column);
			contentValues.put(column, entity);
			entitiesMap.put(column, Boolean.TRUE);
		}

		boolean isEmpty() {
			return contentValues.size() == 0;
		}

		Set<String> getColumns() {
			return entitiesMap.keySet();
		}

		String getNewValue(String column) {
			String value = contentValues.getAsString(column);
			if (entitiesMap.get(column)) {
				return value;
			} else {
				return QueryAdapter.wrapString(value);
			}
		}

	}

}
