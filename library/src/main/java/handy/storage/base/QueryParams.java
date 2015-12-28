package handy.storage.base;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import handy.storage.log.DatabaseLog;
import handy.storage.util.Factory;

/**
 * Encapsulates query parameters.
 */
public class QueryParams implements Cloneable {

	private String mTableName;
	private String[] mColumns = null;
	private String mSelection = null;
	private String[] mSelectionArgs = null;
	private String mHaving = null;
	private boolean mDistinct = false;
	private List<String> orderBy = new ArrayList<>();
	private List<String> groupBy = new ArrayList<>();
	private int queryLimit = -1;
	private int queryOffset = -1;

	String getTableName() {
		return mTableName;
	}

	/**
	 * Sets the table name to query from.
	 *
	 * @param tableName table to query from
	 * @return this instance
	 */
	public QueryParams from(String tableName) {
		mTableName = tableName;
		return this;
	}

	String[] getColumns() {
		return mColumns;
	}

	/**
	 * Sets columns to query.
	 *
	 * @param columns column names to query
	 * @return this instance
	 */
	public QueryParams columns(String... columns) {
		mColumns = columns;
		return this;
	}

	String getSelection() {
		return mSelection;
	}

	/**
	 * Set a selection filter.
	 *
	 * @param selection selection pattern
	 * @param selectionArgs selection arguments
	 * @return this instance
	 */
	public QueryParams where(String selection, String... selectionArgs) {
		mSelection = selection;
		mSelectionArgs = selectionArgs;
		return this;
	}

	String[] getSelectionArgs() {
		return mSelectionArgs;
	}

	String getGroupBy() {
		return joinColumns(groupBy);
	}

	boolean isDistinct() {
		return mDistinct;
	}

	/**
	 * Sets whether this query should select only distinct values.
	 *
	 * @param distinct if the query should be distinct
	 * @return this instance
	 */
	public QueryParams distinct(boolean distinct) {
		mDistinct = distinct;
		return this;
	}

	/**
	 * Adds columns to group by.
	 * 
	 * @param groupByColumns
	 *            names of columns
	 * @return this instance
	 */
	public QueryParams groupBy(String... groupByColumns) {
		groupBy.addAll(Arrays.asList(groupByColumns));
		return this;
	}

	String getHaving() {
		return mHaving;
	}

	/**
	 * Sets having expression
	 *
	 * @param having having string
	 * @return this instance
	 */
	public QueryParams having(String having) {
		mHaving = having;
		return this;
	}

	String getOrderBy() {
		return joinColumns(orderBy);
	}

	/**
	 * Adds a column to order by.
	 *
	 * @param column column's name
	 * @param order ascending or descending
	 * @return this instance
	 */
	public QueryParams orderBy(String column, Order order) {
		orderBy.add(column + (order == Order.DESCENDING ? " DESC" : " ASC"));
		return this;
	}

	/**
	 * Adds a column to order by in ascending order.
	 *
	 * @param column column's name
	 * @return this instance
	 */
	public QueryParams orderBy(String column) {
		return orderBy(column, Order.ASCENDING);
	}

	String getLimit() {
		if (queryLimit >= 0) {
			if (queryOffset >= 0) {
				return String.valueOf(queryOffset) + ", " + String.valueOf(queryLimit);
			} else {
				return String.valueOf(queryLimit);
			}
		} else {
			return null;
		}
	}

	/**
	 * Sets a selection's limit.
	 *
	 * @param limit maximum size of result
	 * @return this instance
	 */
	public QueryParams limit(int limit) {
		queryLimit = limit;
		return this;
	}

	/**
	 * Sets a selection's offset. Have no effect if a limit is not set.
	 *
	 * @param offset number of items from beginning to skip
	 * @return this instance
	 */
	public QueryParams offset(int offset) {
		queryOffset = offset;
		return this;
	}

	/**
	 * Returns SQL select query corresponding to this query parameters.
	 */
	public String toRawSqlQuery() {
		StringBuilder sb = new StringBuilder("SELECT ");
		if (isDistinct()) {
			sb.append("DISTINCT ");
		}
		if (mColumns == null || mColumns.length == 0) {
			sb.append('*');
		} else {
			sb.append(TextUtils.join(", ", mColumns));
		}
		addClause(sb, "FROM", getTableName());
		if (mSelectionArgs != null && mSelectionArgs.length > 0) {
			throw new RuntimeException("can't create sql query with no inlined arguments");
		}
		addClause(sb, "WHERE", getSelection());
		addClause(sb, "GROUP BY", getGroupBy());
		addClause(sb, "HAVING", getHaving());
		addClause(sb, "ORDER BY", getOrderBy());
		addClause(sb, "LIMIT", getLimit());
		return sb.toString();
	}

	private static void addClause(StringBuilder sb, String operator, String value) {
		if (!TextUtils.isEmpty(value)) {
			sb.append(' ');
			sb.append(operator);
			sb.append(' ');
			sb.append(value);
		}
	}
	
	/**
	 * Clones the object.
	 */
	@Override
	public QueryParams clone() {
		try {
			QueryParams clone = (QueryParams) super.clone();
			clone.mColumns = cloneStringArray(mColumns);
			clone.mSelectionArgs = cloneStringArray(mSelectionArgs);
			clone.groupBy = new ArrayList<>(groupBy);
			clone.orderBy = new ArrayList<>(orderBy);
			return clone;
		} catch (CloneNotSupportedException e) {
			DatabaseLog.logException(e);
			throw new RuntimeException("something is totally wrong with cloning");
		}
	}

	private static String[] cloneStringArray(String[] array) {
		if (array == null) {
			return null;
		} else {
			String[] arrayCopy = new String[array.length];
			System.arraycopy(array, 0, arrayCopy, 0, array.length);
			return arrayCopy;
		}
	}


	private static String joinColumns(List<String> columns) {
		if (!columns.isEmpty()) {
			return TextUtils.join(", ", columns);
		} else {
			return null;
		}
	}

	public static final Factory<QueryParams> DEFAULT_FACTORY = QueryParams::new;


	public static Factory<QueryParams> cloneObjectFactory(QueryParams params) {
		QueryParams sample = params.clone();
		return sample::clone;
	}

}