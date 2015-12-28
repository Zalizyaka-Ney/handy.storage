package handy.storage.api;

import handy.storage.Expression;
import handy.storage.ReadableTable;
import handy.storage.Table;
import handy.storage.base.Order;
import handy.storage.base.QueryParams;

/**
 * Base interface for {@link Select} and {@link Mapping} operations.
 *
 * @param <Interface>
 */
public interface DataSelection<Interface> {
	/**
	 * Limits the number of objects in the result.
	 *
	 * @param limit selection limit
	 * @return this object
	 */
	Interface limit(int limit);

	/**
	 * Sets offset. This value will be ignored, if limit is not set.
	 *
	 * @return this object
	 */
	Interface offset(int offset);


	/**
	 * Adds column to group by. If a model contains a column representing an aggregation function, it's better to build
	 * an aggregated {@link handy.storage.ReadableTable} using {@link ReadableTable#aggregate()} instead of grouping by that column every time.
	 *
	 * @param columns columns to group by
	 * @return this object
	 */
	Interface groupBy(String... columns);

	/**
	 * Sets having expression. Use only simultaneously with
	 * {@link Select#groupBy(String...)}.
	 *
	 * @param expression a {@link Expression} object built for this table.
	 * @return this object
	 * @throws IllegalArgumentException if passed expression was built for another table
	 */
	Interface having(Expression expression);

	/**
	 * Adds a column to order by.
	 *
	 * @param column column to group by
	 * @param order  ascending or descending
	 * @return this instance
	 */
	Interface orderBy(String column, Order order);

	/**
	 * Adds a column to order by in ascending order.
	 *
	 * @param column column to order by
	 * @return this instance
	 */
	Interface orderBy(String column);

	/**
	 * Builds a {@link Table} representing the result of this select. The
	 * resulting table will have empty name.
	 */
	Table asTable();

	/**
	 * Builds a {@link Table} representing the result of this select.
	 *
	 * @param tableName name for the built table
	 */
	Table asTable(String tableName);

	/**
	 * Converts this select to {@link QueryParams} object.
	 */
	QueryParams asQueryParams();

	/**
	 * Adds a virtual column to this select. This column can be used in
	 * <code>where</code> and <code>orderBy</code> methods. This column won't be
	 * included in result of {@link #asTable()} function.
	 *
	 * @param alias  name of virtual column.
	 * @param entity SQLite entity of this virtual column
	 * @return this object
	 */
	Interface addVirtualColumn(String alias, String entity);
}
