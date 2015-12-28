package handy.storage.api;

import handy.storage.Expression;
import handy.storage.UpdateOperation;
import handy.storage.base.OnConflictStrategy;
import handy.storage.exception.OperationException;

/**
 * Interface for update operation.
 */
public interface Update {

	/**
	 * Builds a filtering expression for this operation.
	 *
	 * @param column a column's name from the table
	 */
	ColumnCondition<UpdateOperation> where(String column);

	/**
	 * Sets a filtering expression for this operation.
	 *
	 * @param expression a {@link Expression} object built for this table.
	 * @return this object
	 * @throws IllegalArgumentException if passed expression was built for another table
	 */
	UpdateOperation where(Expression expression);

	/**
	 * Sets new value for the column
	 *
	 * @param column column's name
	 * @param value  new value
	 * @return this object
	 */
	Update setValue(String column, Object value);

	/**
	 * Adds parameter <code>value</code> to the column's value.
	 *
	 * @param column column's value
	 * @param value  value to add
	 * @return this object
	 * @throws IllegalArgumentException if this column is not numeric
	 */
	Update addValue(String column, Number value);

	/**
	 * Sets the result of a custom SQLite expression as the new column's value.
	 *
	 * @param column column's name
	 * @param entity custom SQLite expression
	 * @return this object
	 */
	Update setEntity(String column, String entity);

	/**
	 * Executes this operation with default {@link OnConflictStrategy}.
	 *
	 * @return the number of affected rows
	 * @throws OperationException if any error happen
	 */
	int execute() throws OperationException;

	/**
	 * Executes this operation with default {@link OnConflictStrategy}, returns
	 * <code>-1</code> if any error happen.
	 *
	 * @return the number of affected rows
	 */
	int executeSafely();

	/**
	 * Executes this operation with specified {@link OnConflictStrategy}.
	 *
	 * @param onConflict strategy to perform on conflicts
	 * @return the number of affected rows
	 * @throws OperationException if any error happen
	 */
	int execute(OnConflictStrategy onConflict) throws OperationException;

	/**
	 * Executes this operation with specified {@link OnConflictStrategy},
	 * returns <code>-1</code> if any error happen.
	 *
	 * @param onConflict strategy to perform on conflicts
	 * @return the number of affected rows
	 */
	int executeSafely(OnConflictStrategy onConflict);

}
