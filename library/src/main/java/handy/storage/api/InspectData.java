package handy.storage.api;

import handy.storage.Expression;
import handy.storage.InspectDataOperation;
import handy.storage.exception.OperationException;

/**
 * A batch of operations selecting the most common aggregated values from a table.
 */
public interface InspectData {

	/**
	 * Counts the data rows satisfying the filter.
	 *
	 * @return number of data rows
	 * @throws OperationException if any error happen
	 */
	int count() throws OperationException;

	/**
	 * Counts the data rows satisfying the filter. Returns <code>-1</code> if any error happen.
	 *
	 * @return number of data rows
	 */
	int countSafely();

	/**
	 * Checks if there are any data satisfying the filter.
	 *
	 * @throws OperationException if any error happen
	 */
	boolean exists() throws OperationException;

	/**
	 * Checks if there are any data satisfying the filter. Returns <code>false</code> if any error happen.
	 */
	boolean existsSafely();

	/**
	 * Returns the largest value of column satisfying the filter. Returns <code>null</code> if there is no such values (or they all are <code>null</code>).
	 *
	 * @param column      name of column
	 * @param columnClass type of column
	 * @param <T>         type of column
	 * @throws OperationException if any error happen
	 */
	<T> T getLargestValueOf(String column, Class<T> columnClass) throws OperationException;

	/**
	 * Returns the largest value of column satisfying the filter. Returns <code>defaultValue</code> if this value can't be calculated.
	 *
	 * @param column       name of column
	 * @param columnClass  type of column
	 * @param defaultValue default value
	 * @param <T>          type of column
	 */
	<T> T getLargestValueOf(String column, Class<T> columnClass, T defaultValue);

	/**
	 * Returns the smallest value of column satisfying the filter. Returns <code>null</code> if there is no such values (or they all are <code>null</code>).
	 *
	 * @param column      name of column
	 * @param columnClass type of column
	 * @param <T>         type of column
	 * @throws OperationException if any error happen
	 */
	<T> T getSmallestValueOf(String column, Class<T> columnClass) throws OperationException;

	/**
	 * Returns the smallest value of column satisfying the filter. Returns <code>defaultValue</code> if this value can't be calculated.
	 *
	 * @param column       name of column
	 * @param columnClass  type of column
	 * @param defaultValue default value
	 * @param <T>          type of column
	 */
	<T> T getSmallestValueOf(String column, Class<T> columnClass, T defaultValue);


	/**
	 * Returns the average value of column satisfying the filter. Returns <code>null</code> if there is no such values (or they all are <code>null</code>).
	 *
	 * @param column      name of column
	 * @param columnClass type of column
	 * @param <T>         type of column
	 * @throws OperationException if any error happen
	 */
	<T> T getAverageValueOf(String column, Class<T> columnClass) throws OperationException;

	/**
	 * Returns the average value of column satisfying the filter. Returns <code>defaultValue</code> if this value can't be calculated.
	 *
	 * @param column       name of column
	 * @param columnClass  type of column
	 * @param defaultValue default value
	 * @param <T>          type of column
	 */
	<T> T getAverageValueOf(String column, Class<T> columnClass, T defaultValue);

	/**
	 * Returns the sum of values of column satisfying the filter. Returns <code>0</code> if there is no such values (or they all are <code>null</code>).
	 *
	 * @param column      name of column
	 * @param columnClass type of column
	 * @param <T>         type of column
	 * @throws OperationException if any error happen
	 */
	<T> T getSumOf(String column, Class<T> columnClass) throws OperationException;

	/**
	 * Returns the sum of values of column satisfying the filter. Returns <code>defaultValue</code> if this value can't be calculated.
	 *
	 * @param column       name of column
	 * @param columnClass  type of column
	 * @param defaultValue default value
	 * @param <T>          type of column
	 */
	<T> T getSumOf(String column, Class<T> columnClass, T defaultValue);

	/**
	 * Builds a filtering expression for this operation.
	 *
	 * @param column a column's name from the table
	 * @return this object
	 */
	ColumnCondition<InspectDataOperation> where(String column);

	/**
	 * Sets a filtering expression for this operation.
	 *
	 * @param expression a {@link Expression} object built for this table.
	 * @return this object
	 * @throws IllegalArgumentException if passed expression was built for another table
	 */
	InspectDataOperation where(Expression expression);

}
