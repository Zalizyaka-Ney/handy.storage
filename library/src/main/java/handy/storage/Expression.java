package handy.storage;

import android.text.TextUtils;

import java.util.LinkedList;
import java.util.List;

import handy.storage.api.ColumnCondition;

/**
 * Selection expression. Can be get a factory for it using {@link Table#expressions()} method.
 */
public final class Expression {

	/**
	 * Type of an operation.
	 */
	public enum Operator {

		/**
		 * All of the expressions must be fulfilled.
		 */
		AND,

		/**
		 * At least one of the expressions must be fulfilled.
		 */
		OR

	}

	private final QueryAdapter queryAdapter;
	private final String selection;

	Expression(String selection, QueryAdapter queryAdapter) {
		this.selection = selection;
		this.queryAdapter = queryAdapter;
	}

	/**
	 * Returns SQLite's string representation of this expression.
	 */
	@Override
	public String toString() {
		return selection;
	}

	/**
	 * Returns hashcode of the table this expression was created for.
	 */
	protected int getTableHashcode() {
		return queryAdapter.getTableHashCode();
	}

	/**
	 * Returns new Expression which is fulfilled only if both expressions (this
	 * one and the argument) are fulfilled.
	 */
	public Expression and(Expression anotherExpression) {
		return join(Operator.AND, this, anotherExpression);
	}

	/**
	 * Appends a new column expression to this one with "AND" operator.
	 */
	public ColumnCondition<Expression> and(String column) {
		return new CompositeExpressionBuilder(this, Operator.AND, column, queryAdapter);
	}

	/**
	 * Returns new Expression which is fulfilled if any of expressions (this one
	 * and the argument) is fulfilled.
	 */
	public Expression or(Expression anotherExpression) {
		return join(Operator.OR, this, anotherExpression);
	}

	/**
	 * Appends a new column expression to this one with "OR" operator.
	 */
	public ColumnCondition<Expression> or(String column) {
		return new CompositeExpressionBuilder(this, Operator.OR, column, queryAdapter);
	}

	/**
	 * Returns new Expression opposite to this one.
	 */
	public Expression invert() {
		return new Expression("NOT (" + selection + ")", queryAdapter);
	}

	/**
	 * Combines several expression into one. All expressions must be obtained
	 * from the same table.
	 */
	public static Expression join(Operator operator, Expression... expressions) {
		int n = expressions.length;
		if (n < 2) {
			throw new IllegalArgumentException("Expression.join() needs at least 2 expressions.");
		}
		int tableHashcode = expressions[0].getTableHashcode();
		for (int i = 1; i < n; i++) {
			if (expressions[i].getTableHashcode() != tableHashcode) {
				throw new IllegalArgumentException("Can't join expressions for different tables.");
			}
		}
		QueryAdapter queryAdapter = expressions[0].queryAdapter;
		switch (operator) {
			case AND:
				return new Expression(joinSelection(" AND ", expressions), queryAdapter);
			case OR:
				return new Expression(joinSelection(" OR ", expressions), queryAdapter);
			default:
				throw new IllegalArgumentException("Unknown operator");
		}
	}

	private static String joinSelection(String op, Expression[] expressions) {
		List<String> stringRepresentations = new LinkedList<>();
		for (Expression expression : expressions) {
			stringRepresentations.add("(" + expression.toString() + ")");
		}
		return TextUtils.join(op, stringRepresentations);
	}

	/**
	 * Helps build composite selection.
	 *
	 * @param <T> type of return value
	 */
	abstract static class CompositeSelectionBuilder<T> extends ColumnExpressionBuilder<T> {

		private final Expression leftExpression;
		private final Operator operator;

		CompositeSelectionBuilder(Expression leftExpression, Operator operator, String column, QueryAdapter queryAdapter) {
			super(column, queryAdapter);
			this.leftExpression = leftExpression;
			this.operator = operator;
		}

		@Override
		Expression buildExpression() {
			Expression rightExpression = super.buildExpression();
			if (operator == Operator.OR) {
				return leftExpression.or(rightExpression);
			} else { // possible values are only AND and OR
				return leftExpression.and(rightExpression);
			}
		}

	}

	/**
	 * Helps build composite expression.
	 */
	private static class CompositeExpressionBuilder extends CompositeSelectionBuilder<Expression> {

		CompositeExpressionBuilder(Expression leftExpression, Operator operator, String column, QueryAdapter queryAdapter) {
			super(leftExpression, operator, column, queryAdapter);
		}

		@Override
		Expression complete() {
			return buildExpression();
		}

	}


}
