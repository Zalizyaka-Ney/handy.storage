package handy.storage;

import handy.storage.Expression.Operator;
import handy.storage.api.ColumnCondition;
import handy.storage.base.DatabaseAdapter;

/**
 * Base database operation with selection.
 *
 * @param <T> inherited class implementation
 */
@SuppressWarnings("rawtypes")
abstract class BaseOperation<T extends BaseOperation> {

	private Expression selection;
	private Table table;

	BaseOperation(Table table) {
		this.table = table;
	}

	public ColumnCondition<T> where(String column) {
		return new ColumnExpressionBuilder<T>(column, table.getQueryAdapter()) {

			@Override
			T complete() {
				selection = buildExpression();
				return thisOperation();
			}

		};
	}

	public T where(Expression expression) {
		table.checkExpressionOwner(expression);
		selection = expression;
		return thisOperation();
	}

	protected String getWhereClause() {
		Expression where = getWhereExpression();
		return where == null ? null : where.toString();
	}

	protected Expression getWhereExpression() {
		return selection;
	}

	/**
	 * Appends a new column expression to the selection with "AND" operator.
	 */
	public ColumnCondition<T> and(String column) {
		return new SelectionBuilder(selection, Operator.AND, column, table.getQueryAdapter());
	}

	/**
	 * Appends an expression to the selection with "AND" operator.
	 */
	public T and(Expression expression) {
		table.checkExpressionOwner(expression);
		selection = selection.and(expression);
		return thisOperation();
	}

	/**
	 * Appends a new column expression to the selection with "OR" operator.
	 */
	public ColumnCondition<T> or(String column) {
		return new SelectionBuilder(selection, Operator.OR, column, table.getQueryAdapter());
	}

	/**
	 * Appends an expression to the selection with "OR" operator.
	 */
	public T or(Expression expression) {
		table.checkExpressionOwner(expression);
		selection = selection.or(expression);
		return thisOperation();
	}

	Table getOwner() {
		return table;
	}

	protected DatabaseAdapter getDatabaseAdapter() {
		return table.getDatabaseAdapter();
	}

	@SuppressWarnings("unchecked")
	private T thisOperation() {
		return (T) this;
	}

	/**
	 * Helps build composite selection.
	 */
	private class SelectionBuilder extends Expression.CompositeSelectionBuilder<T> {

		SelectionBuilder(Expression leftExpression, Operator operator, String column, QueryAdapter queryAdapter) {
			super(leftExpression, operator, column, queryAdapter);
		}

		@Override
		T complete() {
			selection = buildExpression();
			return thisOperation();
		}

	}

}
