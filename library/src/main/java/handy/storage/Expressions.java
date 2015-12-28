package handy.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import handy.storage.annotation.Reference;
import handy.storage.api.ColumnCondition;
import handy.storage.api.Select;
import handy.storage.base.QueryParams;
import handy.storage.exception.IllegalUsageException;

/**
 * Factory of table expressions.
 *
 * @param <T> type of model the table object bound to
 */
public class Expressions<T> {

	private final Table ownerTable;
	private final QueryAdapter queryAdapter;

	Expressions(Table table) {
		ownerTable = table;
		queryAdapter = table.getQueryAdapter();
	}

	/**
	 * Builds a new {@link Expression} for the table, representing a condition
	 * for a column.
	 *
	 * @param column column's name
	 */
	public ColumnCondition<Expression> column(String column) {
		return newColumnExpressionBuilder(column);
	}

	/**
	 * Builds a new {@link Expression} for the table, representing a custom
	 * SQLite expression.
	 *
	 * @param rawExpression expression in SQLite syntax
	 */
	public Expression raw(String rawExpression) {
		return new Expression(rawExpression, queryAdapter);
	}

	/**
	 * Creates a new {@link Expression} instance limiting the objects <b>not</b>
	 * to be <b>one of the <code>objects</code></b>.
	 *
	 * @throws IllegalUsageException if the table is not bound to a model class or the table has no unique or primary key column
	 */
	@SafeVarargs
	public final Expression exclude(T... objects) {
		return exclude(Arrays.asList(objects));
	}

	/**
	 * Creates a new {@link Expression} instance limiting the objects <b>not</b>
	 * to be <b>one of the <code>objects</code></b>.
	 *
	 * @throws IllegalUsageException if the table is not bound to a model class or the table has no unique or primary key column
	 */
	public Expression exclude(Collection<? extends T> objects) {
		ColumnInfo uniqueColumn = ownerTable.getUniqueColumnOrThrow();
		List<Object> uniqueColumnValues = ownerTable.getColumnValues(objects, uniqueColumn);
		ColumnInfo.ColumnId uniqueColumnId = uniqueColumn.getColumnId();
		return column(uniqueColumnId.getFullName()).notIn(uniqueColumnValues);
	}

	/**
	 * Creates a new {@link Expression} instance limiting the objects <b>not</b>
	 * to be <b>in the <code>table</code></b> (selection of values from another
	 * table referencing to this one). This <code>table</code> must be obtained
	 * by calling {@link Table#columnValues(String)} with a name of a column
	 * declared as {@link Reference} to this table as the argument.
	 *
	 * @throws IllegalUsageException if the table has no unique or primary key column
	 */
	public Expression exclude(ColumnValuesTable table) {
		ColumnInfo uniqueColumn = ownerTable.getUniqueColumnOrThrow();
		String uniqueColumnName = uniqueColumn.getColumnId().getFullName();
		return column(uniqueColumnName).notIn(table);
	}

	/**
	 * Creates a new {@link Expression} instance limiting the objects to be
	 * <b>one of the <code>objects</code></b>.
	 *
	 * @throws IllegalUsageException if the table is not bound to a model class or the table has no unique or primary key column
	 */
	@SafeVarargs
	public final Expression oneOf(T... objects) {
		return oneOf(Arrays.asList(objects));
	}

	/**
	 * Creates a new {@link Expression} instance limiting the objects to be
	 * <b>one of the <code>objects</code></b>.
	 *
	 * @throws IllegalUsageException if the table is not bound to a model class or the table has no unique or primary key column
	 */
	public Expression oneOf(Collection<? extends T> objects) {
		ColumnInfo uniqueColumn = ownerTable.getUniqueColumnOrThrow();
		List<Object> uniqueColumnValues = ownerTable.getColumnValues(objects, uniqueColumn);
		ColumnInfo.ColumnId uniqueColumnId = uniqueColumn.getColumnId();
		return column(uniqueColumnId.getFullName()).in(uniqueColumnValues);
	}

	/**
	 * Creates a new {@link Expression} instance limiting the objects to be
	 * <b>in the <code>table</code></b> (selection of values from another table
	 * referencing to this one). This <code>table</code> must be obtained by
	 * calling {@link Table#columnValues(String)} with a name of a column
	 * declared as {@link Reference} to this table as the argument.
	 *
	 * @throws IllegalUsageException if the table has no unique or primary key column
	 */
	public Expression oneOf(ColumnValuesTable table) {
		ColumnInfo uniqueColumn = ownerTable.getUniqueColumnOrThrow();
		String uniqueColumnName = uniqueColumn.getColumnId().getFullName();
		return column(uniqueColumnName).in(table);
	}

	Expression oneOf(Select<T> select) {
		return createExpressionForSelect(select, true);
	}

	Expression exclude(Select<T> select) {
		return createExpressionForSelect(select, false);
	}

	private Expression createExpressionForSelect(Select<T> select, boolean including) {
		ColumnInfo uniqueColumn = ownerTable.getUniqueColumnOrThrow();
		String uniqueColumnName = uniqueColumn.getColumnId().getFullName();
		QueryParams query = select.asQueryParams();
		query.columns(uniqueColumnName);
		return including
			? newColumnExpressionBuilder(uniqueColumnName).inTable(query.toRawSqlQuery())
			: newColumnExpressionBuilder(uniqueColumnName).notInTable(query.toRawSqlQuery());
	}

	ColumnExpressionBuilder<Expression> newColumnExpressionBuilder(String column) {
		return new ColumnExpressionBuilder<Expression>(column, queryAdapter) {

			@Override
			Expression complete() {
				return buildExpression();
			}

		};
	}

}
