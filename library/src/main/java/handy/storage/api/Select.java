package handy.storage.api;

import java.util.List;

import handy.storage.Expression;
import handy.storage.SelectOperation;
import handy.storage.exception.OperationException;

/**
 * Interface for select operation.
 *
 * @param <T> model
 */
public interface Select<T> extends Iterable<T>, DataSelection<Select<T>> {

	/**
	 * Executes this select operation and returns a single object. Use this
	 * method if the operation is supposed to return not more than one object.
	 * The method returns the first element in the database satisfying the
	 * filters or <code>null</code>, if there is no such element.
	 *
	 * @throws OperationException if any error happen
	 */
	T executeSingle() throws OperationException;

	/**
	 * The same as {@link #executeSingle()}, but returns <code>null</code>
	 * instead of throwing exceptions.
	 */
	T executeSingleAndSafely();

	/**
	 * Executes the operation.
	 *
	 * @return the list of objects stored in the database satisfying the
	 * filters.
	 * @throws OperationException if any error happen
	 */
	List<T> execute() throws OperationException;

	/**
	 * Executes the operation, returns an empty list if any error happen.
	 *
	 * @return the list of objects stored in the database satisfying the
	 * filters.
	 */
	List<T> executeSafely();

	/**
	 * Marks that only unique objects should be returned.
	 *
	 * @return this object
	 */
	Select<T> distinct();

	/**
	 * Builds a filtering expression for this operation.
	 *
	 * @param column a column's name from the table
	 */
	ColumnCondition<SelectOperation<T>> where(String column);

	/**
	 * Sets a filtering expression for this operation.
	 *
	 * @param expression a {@link Expression} object built for this table.
	 * @return this object
	 * @throws IllegalArgumentException if passed expression was built for another table
	 */
	SelectOperation<T> where(Expression expression);

}
