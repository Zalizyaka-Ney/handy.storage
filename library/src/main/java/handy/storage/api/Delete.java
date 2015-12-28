package handy.storage.api;

import handy.storage.DeleteOperation;
import handy.storage.Expression;
import handy.storage.base.Order;
import handy.storage.exception.OperationException;

/**
 * Interface for delete operation.
 *
 * @param <T> model class
 */
public interface Delete<T extends Model> {

	/**
	 * Executes the operation.
	 *
	 * @return the number of the deleted rows.
	 * @throws OperationException if any error happen
	 */
	int execute() throws OperationException;

	/**
	 * Executes the operation. Returns <code>-1</code> if any error happen.
	 *
	 * @return the number of the deleted rows.
	 */
	int executeSafely();

	/**
	 * Builds a filtering expression for this operation.
	 *
	 * @param column a column's name from the table
	 */
	handy.storage.api.ColumnCondition<DeleteOperation<T>> where(String column);

	/**
	 * Sets a filtering expression for this operation.
	 *
	 * @param expression a {@link Expression} object built for this table.
	 * @return this object
	 * @throws IllegalArgumentException if passed expression was built for another table
	 */
	DeleteOperation<T> where(Expression expression);

	/**
	 * Limits the number of rows to delete. Use {@link #orderBy(String)} and
	 * {@link #orderBy(String, Order)} to set a rule which rows should be
	 * deleted first.
	 *
	 * @param limit mux number of items to delete
	 * @return this object
	 */
	Delete<T> limit(int limit);

	/**
	 * Adds a column to order by during deleting. Use it only simultaneously
	 * with {@link #limit(int)}.
	 *
	 * @param column column to order by
	 * @return this object
	 */
	Delete<T> orderBy(String column);

	/**
	 * Adds a column to order by during deleting. Use it only simultaneously
	 * with {@link #limit(int)}.
	 *
	 * @param column column to order by
	 * @param order  ascending or descending
	 * @return this object
	 */
	Delete<T> orderBy(String column, Order order);

}
