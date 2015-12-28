package handy.storage;

import java.util.List;

import handy.storage.api.Select;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.QueryParams;
import handy.storage.exception.OperationException;
import handy.storage.util.Factory;

/**
 * Represents table that can be used in "in" and "not in" operations. Use
 * "columnValues" functions to get it.
 */
public class ColumnValuesTable extends Table {

	private final String column;

	ColumnValuesTable(String column, TableInfo tableInfo, DatabaseAdapter databaseAdapter, DatabaseCore databaseCore, Factory<QueryParams> queryParamsFactory) {
		super(tableInfo, databaseAdapter, databaseCore, queryParamsFactory);
		this.column = column;
	}

	/**
	 * Return the column name or SQLite entity this table was created for.
	 */
	public String getColumnEntity() {
		return column;
	}

	/**
	 * Selects the column values.
	 * 
	 * @param type
	 *            type of the column
	 */
	public <T> Select<T> select(Class<T> type) {
		return select(column, type);
	}

	/**
	 * Selects all column values.
	 * 
	 * @param type
	 *            type of the column
	 * @throws OperationException
	 *             if any error happen
	 */
	public <T> List<T> selectAll(Class<T> type) throws OperationException {
		return selectAll(column, type);
	}
}
