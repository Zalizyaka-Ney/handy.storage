package handy.storage;

import android.text.TextUtils;

import java.util.LinkedList;
import java.util.List;

import handy.storage.api.ColumnCondition;

/**
 * Helps build columns expressions.
 * 
 * @param <T>
 *            type of return value
 */
abstract class ColumnExpressionBuilder<T> implements ColumnCondition<T> {

	private final QueryAdapter queryAdapter;
	private final String column;
	private String expressionString;

	ColumnExpressionBuilder(String column, QueryAdapter queryAdapter) {
		this.queryAdapter = queryAdapter;
		String fullColumnName = queryAdapter.getFullColumnName(column);
		this.column = fullColumnName;
		expressionString = fullColumnName;
	}

	@Override
	public T between(Object value1, Object value2) {
		expressionString = expressionString + betweenValuesString(value1, value2);
		return complete();
	}

	@Override
	public T notBetween(Object value1, Object value2) {
		expressionString = expressionString + " NOT" + betweenValuesString(value1, value2);
		return complete();
	}

	private String betweenValuesString(Object value1, Object value2) {
		return " BETWEEN " + convertValue(value1) + " AND " + convertValue(value2);
	}

	@Override
	public T lessThan(Object argument) {
		expressionString = expressionString + " < " + convertValue(argument);
		return complete();
	}

	@Override
	public T lessThanOrEqualTo(Object argument) {
		expressionString = expressionString + " <= " + convertValue(argument);
		return complete();
	}

	@Override
	public T greaterThanOrEqualTo(Object argument) {
		expressionString = expressionString + " >= " + convertValue(argument);
		return complete();
	}

	@Override
	public T greaterThan(Object argument) {
		expressionString = expressionString + " > " + convertValue(argument);
		return complete();
	}

	@Override
	public T equalsTo(Object argument) {
		expressionString = expressionString + " = " + convertValue(argument);
		return complete();
	}

	@Override
	public T in(Iterable<?> iterable) {
		String[] values = convertValues(iterable);
		expressionString = expressionString + " IN (" + TextUtils.join(",", values) + ")";
		return complete();
	}

	@Override
	public T in(Object[] iterable) {
		String[] values = convertValues(iterable);
		expressionString = expressionString + " IN (" + TextUtils.join(",", values) + ")";
		return complete();
	}

	@Override
	public T in(ColumnValuesTable table) {
		expressionString = expressionString + " IN " + table.getTableEntity();
		return complete();
	}
	
	@Override
	public T notIn(ColumnValuesTable table) {
		expressionString = expressionString + " NOT IN " + table.getTableEntity();
		return complete();
	}

	T inTable(String tableEntity) {
		expressionString = expressionString + " IN (" + tableEntity + ")";
		return complete();
	}

	@Override
	public T notIn(Iterable<?> iterable) {
		String[] values = convertValues(iterable);
		expressionString = expressionString + " NOT IN (" + TextUtils.join(",", values) + ")";
		return complete();
	}

	@Override
	public T notIn(Object[] iterable) {
		String[] values = convertValues(iterable);
		expressionString = expressionString + " NOT IN (" + TextUtils.join(",", values) + ")";
		return complete();
	}

	T notInTable(String tableEntity) {
		expressionString = expressionString + " NOT IN (" + tableEntity + ")";
		return complete();
	}

	@Override
	public T differsFrom(Object value) {
		expressionString = expressionString + " != " + convertValue(value);
		return complete();
	}
	
	@Override
	public T isNull() {
		expressionString = expressionString + " IS NULL";
		return complete();
	}

	@Override
	public T isNotNull() {
		expressionString = expressionString + " IS NOT NULL";
		return complete();
	}

	@Override
	public T like(String argument) {
		expressionString = expressionString + " LIKE " + QueryAdapter.wrapString(argument);
		return complete();
	}

	private String convertValue(Object value) {
		return queryAdapter.convertToDatabaseValue(column, value);
	}

	private String[] convertValues(Object... iterable) {
		String[] values = new String[iterable.length];
		for (int i = 0; i < iterable.length; i++) {
			values[i] = convertValue(iterable[i]);
		}
		return values;
	}

	private String[] convertValues(Iterable<?> iterable) {
		List<Object> valuesList = new LinkedList<>();
		for (Object value : iterable) {
			valuesList.add(value);
		}
		return convertValues(valuesList.toArray(new Object[valuesList.size()]));
	}

	Expression buildExpression() {
		return new Expression(expressionString, queryAdapter);
	}

	abstract T complete();

}
