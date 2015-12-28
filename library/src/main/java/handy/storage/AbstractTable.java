package handy.storage;

import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import handy.storage.base.DatabaseAdapter;
import handy.storage.base.QueryParams;
import handy.storage.exception.IllegalUsageException;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;
import handy.storage.util.Factory;
import handy.storage.util.ReflectionUtils;

/**
 * Base class for {@link Table} implementation
 */
abstract class AbstractTable {

	private final TableInfo tableInfo;
	private final DatabaseAdapter databaseAdapter;
	private final DatabaseCore databaseCore;
	private final QueryAdapter queryAdapter;
	private final Factory<QueryParams> queryParamsFactory;

	AbstractTable(TableInfo tableInfo, DatabaseAdapter databaseAdapter, DatabaseCore databaseCore, Factory<QueryParams> queryParamsFactory) {
		this.tableInfo = tableInfo;
		this.databaseAdapter = databaseAdapter;
		this.databaseCore = databaseCore;
		this.queryAdapter = new QueryAdapter(tableInfo, databaseCore.getDataAdapters(), hashCode());
		this.queryParamsFactory = queryParamsFactory;
	}

	protected TableInfo getTableInfo() {
		return tableInfo;
	}

	protected DatabaseAdapter getDatabaseAdapter() {
		return databaseAdapter;
	}

	protected DatabaseCore getDatabaseCore() {
		return databaseCore;
	}

	protected QueryAdapter getQueryAdapter() {
		return queryAdapter;
	}

	/**
	 * Returns the SQLite entity of this table.
	 */
	public String getTableEntity() {
		return tableInfo.getEntity();
	}

	/**
	 * Returns the name of this table (can be empty).
	 */
	public String getTableName() {
		return tableInfo.getName();
	}


	/**
	 * Prints information about this table using {@link DatabaseLog#i(String)}
	 * method.
	 */
	public void logInfo() {
		tableInfo.logItself();
	}

	protected Table createNewTableEntity(TableInfo newEntityTableInfo) {
		return new Table(newEntityTableInfo, databaseAdapter, databaseCore, queryParamsFactory);
	}

	/**
	 * Prints the content of this table using {@link DatabaseLog#i(String)}
	 * method.
	 */
	public void logContent() {
		try {
			Cursor cursor = databaseAdapter.performQuery(new QueryParams().from(getTableEntity()));
			try {
				DatabaseLog.dumpCursor(cursor, "the content of \"" + getTableEntity() + "\":");
			} finally {
				cursor.close();
			}
		} catch (OperationException e) {
			DatabaseLog.logException(e);
		}
	}

	/**
	 * Returns the number of columns this table has.
	 */
	public int getColumnsCount() {
		return tableInfo.getColumns().size();
	}

	DataAdapters getDataAdapters() {
		return databaseCore.getDataAdapters();
	}

	protected Factory<QueryParams> getQueryParamsFactory() {
		return queryParamsFactory;
	}

	protected Class<?> getModelClass() {
		return null;
	}

	protected List<Object> getColumnValues(Collection<?> elements, ColumnInfo column) {
		if (getModelClass() == null) {
			throw new IllegalUsageException("the table is not bound to a model class");
		}
		List<Object> uniqueColumnValues = new ArrayList<>(elements.size());
		Field uniqueField = column.getField();
		for (Object element : elements) {
			uniqueColumnValues.add(ReflectionUtils.getFieldValue(uniqueField, element));
		}
		return uniqueColumnValues;
	}

	protected ColumnInfo getUniqueColumnOrThrow() {
		ColumnInfo uniqueColumn = getTableInfo().getUniqueColumn();
		if (uniqueColumn == null) {
			throw new IllegalUsageException("there is no unique column or primary key in table " + getTableName());
		}
		return uniqueColumn;
	}

	protected ColumnInfo getPrimaryKeyColumnOrThrow() {
		ColumnInfo uniqueColumn = getTableInfo().getPrimaryKeyColumn();
		if (uniqueColumn == null) {
			throw new IllegalUsageException("there is no primary column in table " + getTableName());
		}
		return uniqueColumn;
	}

	protected void checkExpressionOwner(Expression expression) {
		if (expression != null && expression.getTableHashcode() != hashCode()) {
			throw new IllegalArgumentException("Passed expression was built for another table.");
		}
	}
}
