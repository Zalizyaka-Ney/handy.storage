package handy.storage.api;

import java.util.Map;

import handy.storage.Expression;
import handy.storage.MappingOperation;
import handy.storage.exception.OperationException;

/**
 * An operation that maps values of one column to a corresponding value of another column or a model object (or to a list of such values or objects).
 *
 * @param <K> key column type
 * @param <V> value column type
 */
public interface Mapping<K, V> extends DataSelection<Mapping<K, V>> {

	/**
	 * Executes the operation.
	 *
	 * @throws OperationException if any error happen
	 */
	Map<K, V> execute() throws OperationException;

	/**
	 * Executes the operation, returns an empty map if any error happen.
	 */
	Map<K, V> executeSafely();

	/**
	 * Builds a filtering expression for this operation.
	 *
	 * @param column a column's name from the table
	 */
	ColumnCondition<MappingOperation<K, V>> where(String column);

	/**
	 * Sets a filtering expression for this operation.
	 *
	 * @param expression a {@link Expression} object built for this table.
	 * @return this object
	 * @throws IllegalArgumentException if passed expression was built for another table
	 */
	MappingOperation<K, V> where(Expression expression);

	/**
	 * Sets whether the operation should return only unique values.
	 * This option has sense only if you build a mapping to a list of values.
	 * By default the value is set to <code>true</code>.
	 *
	 * @return this object
	 */
	Mapping<K, V> setDistinct(boolean distinct);

}
