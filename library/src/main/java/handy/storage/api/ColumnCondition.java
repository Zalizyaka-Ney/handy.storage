package handy.storage.api;

import handy.storage.ColumnValuesTable;

/**
 * Builds a condition for the column. All the arguments are supposed to have the
 * same type as the column.
 *
 * @param <T> operation
 */
public interface ColumnCondition<T> {

	/**
	 * Builds an expression "x &lt; value", where x is column's value.
	 *
	 * @param value value to compare with
	 */
	T lessThan(Object value);

	/**
	 * Builds an expression "x &lt;= value", where x is column's value.
	 *
	 * @param value value to compare with
	 */
	T lessThanOrEqualTo(Object value);

	/**
	 * Builds an expression "x &gt;= value", where x is column's value.
	 *
	 * @param value value to compare with
	 */
	T greaterThanOrEqualTo(Object value);

	/**
	 * Builds an expression "x &gt; value", where x is column's value.
	 *
	 * @param value value to compare with
	 */
	T greaterThan(Object value);

	/**
	 * Builds an expression "x == value", where x is column's value.
	 *
	 * @param value value to compare with
	 */
	T equalsTo(Object value);

	/**
	 * Builds an expression limiting column's value to be one of <code>iterable</code> elements.
	 *
	 * @param iterable {@link Iterable} instance to get column's values to include
	 */
	T in(Iterable<?> iterable);

	/**
	 * Builds an expression limiting column's value to be one of <code>values</code>.
	 *
	 * @param values column's values to include
	 */
	T in(Object[] values);

	/**
	 * Builds an expression limiting column's value to be one of
	 * <code>table</code>'s values.
	 *
	 * @param table {@link ColumnValuesTable} instance referencing to column's values to include
	 */
	T in(ColumnValuesTable table);

	/**
	 * Builds an expression limiting column's value not to be one of
	 * <code>iterable</code> elements.
	 *
	 * @param iterable {@link Iterable} instance to get column's values to exclude
	 */
	T notIn(Iterable<?> iterable);

	/**
	 * Builds an expression limiting column's value not to be one of
	 * <code>values</code>.
	 *
	 * @param values column's values to exclude
	 */
	T notIn(Object[] values);

	/**
	 * Builds an expression limiting column's value not to be one of
	 * <code>table</code>'s values.
	 *
	 * @param table {@link ColumnValuesTable} instance referencing to column's values to exclude
	 */
	T notIn(ColumnValuesTable table);

	/**
	 * Builds an expression "x != value", where x is column's value.
	 *
	 * @param value value to compare with
	 */
	T differsFrom(Object value);

	/**
	 * Builds an expression "x IS NULL", where x is column's value.
	 */
	T isNull();

	/**
	 * Builds an expression "x IS NOT NULL", where x is column's value.
	 */
	T isNotNull();

	/**
	 * Builds an expression "x LIKE 'value'", where x is column's value.
	 *
	 * @param value string to use in LIKE expression
	 */
	T like(String value);

	/**
	 * Builds an expression "value1 &lt;= x &lt;= value2", where x is column's value.
	 *
	 * @param value1 left value
	 * @param value2 right value
	 */
	T between(Object value1, Object value2);

	/**
	 * Builds an expression inverted to
	 * {@link ColumnCondition#between(Object, Object)} (i.e. column's value is
	 * less than <code>value1</code> or greater than <code>value2</code>).
	 *
	 * @param value1 left value
	 * @param value2 right value
	 */
	T notBetween(Object value1, Object value2);

}