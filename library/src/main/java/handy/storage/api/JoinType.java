package handy.storage.api;

/**
 * Type of tables join.
 */
public enum JoinType {

	/**
	 * "JOIN". This is the default type of join. It creates a new result table
	 * by combining column values of two tables (table1 and table2) based upon
	 * the join-predicate. The query compares each row of table1 with each row
	 * of table2 to find all pairs of rows which satisfy the join-predicate.
	 * When the join-predicate is satisfied, column values for each matched pair
	 * of rows of A and B are combined into a result row.
	 */
	INNER,

	/**
	 * "NATURAL JOIN". This is an {@link #INNER} join which automatically uses
	 * all the matching column names for the join.
	 */
	NATURAL_INNER,

	/**
	 * "CROSS JOIN". This join operation matches every row of the first table
	 * with every row of the second table.
	 */
	CROSS_INNER,

	/**
	 * "LEFT OUTER JOIN". Returns all values from the left table, even if there
	 * is no match with the right table. In such rows there will be NULL values.
	 * In other words, left outer join returns all the values from the left
	 * table, plus matched values from the right table.
	 */
	LEFT_OUTER,

	/**
	 * "NATURAL LEFT OUTER JOIN". This is an {@link #LEFT_OUTER} join which
	 * automatically uses all the matching column names for the join.
	 */
	NATURAL_LEFT_OUTER

}
