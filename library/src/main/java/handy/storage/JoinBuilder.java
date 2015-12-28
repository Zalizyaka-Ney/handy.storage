package handy.storage;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import handy.storage.api.JoinType;
import handy.storage.api.Model;
import handy.storage.base.QueryParams;
import handy.storage.exception.IllegalUsageException;

/**
 * Builds new table obtained by join of two existing tables. Set the contraction
 * of the join by calling one of {@link #onColumnsEquality(String, String)},
 * {@link #onColumnsEquality(String[], String[])}, {@link #onReference()},
 * {@link #using(String...)} methods. Only one contraction can be set. To build
 * the result table use methods {@link #asReadableTable(Class)},
 * {@link #asReadableTable(String, Class)}, {@link #asTable()},
 * {@link #asTable(String)}.
 */
public class JoinBuilder {

	private final Table leftTable;
	private final Table rightTable;
	private final JoinType joinType;
	private String[] usingColumns;
	private String[] onColumnsFromLeftTable;
	private String[] onColumnsFromRightTable;

	private boolean constraintIsSet = false;

	JoinBuilder(Table leftTable, Table rightTable, JoinType joinType) {
		if (TextUtils.isEmpty(leftTable.getTableName()) || TextUtils.isEmpty(rightTable.getTableName())) {
			throw new IllegalArgumentException("can't join table with empty name");
		}
		if (leftTable.getTableName().equals(rightTable.getTableName())) {
			throw new IllegalArgumentException("can't join tables with the same names");
		}
		this.leftTable = leftTable;
		this.rightTable = rightTable;
		this.joinType = joinType;
	}

	private void onConstraintSet() {
		if (constraintIsSet) {
			throw new IllegalUsageException("you have already set the way to join these tables");
		}
		constraintIsSet = true;
	}

	/**
	 * Joins the tables by "USING" keyword, i.e. by equality of the
	 * <code>columns</code> in the both tables. Each passed column must exist in
	 * the both tables. If you want want to join tables by all common columns,
	 * see "natural" {@link JoinType}'s.
	 */
	public JoinBuilder using(String... columns) {
		onConstraintSet();
		checkColumnsExistInBothTables(columns);
		usingColumns = columns;
		return this;
	}

	/**
	 * Joins the tables by equality of the columns from the left table to the
	 * correspond columns from the right table.
	 */
	public JoinBuilder onColumnsEquality(String[] columnsFromLeftTable, String[] columnsFromRightTable) {
		onConstraintSet();
		if (columnsFromLeftTable.length != columnsFromRightTable.length) {
			throw new IllegalArgumentException("arrays should have the same length");
		}
		TableInfo leftTableInfo = leftTable.getTableInfo();
		TableInfo rightTableInfo = rightTable.getTableInfo();
		for (int i = 0; i < columnsFromLeftTable.length; i++) {
			checkReferencesUsage(
					leftTableInfo.getColumnInfo(columnsFromLeftTable[i]),
					rightTableInfo.getColumnInfo(columnsFromRightTable[i]));
		}
		this.onColumnsFromLeftTable = columnsFromLeftTable;
		this.onColumnsFromRightTable = columnsFromRightTable;
		return this;
	}

	/**
	 * Joins the tables by equality of the column from the left table to the
	 * column from the right table.
	 */
	public JoinBuilder onColumnsEquality(String columnFromLeftTable, String columnFromRightTable) {
		return onColumnsEquality(new String[] {columnFromLeftTable }, new String[] {columnFromRightTable });
	}

	/**
	 * Joins the tables on reference from the left table to the right table
	 * declared during database creation. The left table should have exactly one
	 * reference to the right table.
	 */
	public JoinBuilder onReference() {
		onConstraintSet();
		ColumnInfo leftColumn = null;
		for (ColumnInfo column : leftTable.getTableInfo().getColumns()) {
			Class<? extends Model> rightTableModelClass = rightTable.getTableInfo().getOriginClass();
			if (column.isReferenceToTable() && column.getReferencedTable().equals(rightTableModelClass)) {
				if (leftColumn != null) {
					throw new IllegalUsageException("the left table has more than one reference to the right table");
				}
				leftColumn = column;
			}
		}
		if (leftColumn == null) {
			throw new IllegalUsageException("the left table has no reference to the right table");
		}
		onColumnsFromLeftTable = new String[] {leftColumn.getName() };
		onColumnsFromRightTable = new String[] {leftColumn.getReference().getForeignColumn().getName() };
		return this;
	}

	private void checkColumnsExistInBothTables(String... columns) {
		TableInfo leftTableInfo = leftTable.getTableInfo();
		TableInfo rightTableInfo = rightTable.getTableInfo();
		for (String column : columns) {
			ColumnInfo columnFromLeftTable = leftTableInfo.getColumnInfo(column);
			ColumnInfo columnFromRightTable = rightTableInfo.getColumnInfo(column);
			if (columnFromLeftTable == null || columnFromRightTable == null) {
				throw new IllegalArgumentException("can't find column " + column + " in on one of the joining tables");
			}
			checkReferencesUsage(columnFromLeftTable, columnFromRightTable);
		}
	}

	/**
	 * Builds a new table obtained by joining two tables and sets an alias for
	 * it.
	 * 
	 * @param tableName
	 *            alias for the new table
	 */
	public Table asTable(String tableName) {
		TableInfo joinTableIfo = TableInfoFactory.createJoinTableInfo(
				tableName,
				getResultTableEntity(),
				leftTable.getTableInfo(),
				rightTable.getTableInfo(),
				getUnionColumns());
		return new Table(joinTableIfo, leftTable.getDatabaseAdapter(), leftTable.getDatabaseCore(), QueryParams.DEFAULT_FACTORY);
	}

	/**
	 * Builds a new table obtained by joining two tables. The built table has no
	 * alias.
	 */
	public Table asTable() {
		return asTable("");
	}

	/**
	 * Builds a new readable table obtained by joining two tables. The built
	 * table has no alias.
	 * 
	 * @param modelClass
	 *            class of model to read from the table
	 */
	public <T extends Model> ReadableTable<T> asReadableTable(Class<T> modelClass) {
		return asReadableTable("", modelClass);
	}

	/**
	 * Builds a new readable table obtained by joining two tables and sets an
	 * alias for it.
	 * 
	 * @param tableName
	 *            alias for the new table
	 * @param modelClass
	 *            class of model to read from the table
	 */
	public <T extends Model> ReadableTable<T> asReadableTable(String tableName, Class<T> modelClass) {
		return asTable(tableName).asReadableTable(modelClass);
	}

	private String getResultTableEntity() {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		sb.append(leftTable.getTableEntity());
		sb.append(' ');
		sb.append(getJoinString());
		sb.append(' ');
		sb.append(rightTable.getTableEntity());
		if (usingColumns != null && usingColumns.length > 0) {
			sb.append(" USING (");
			sb.append(TextUtils.join(", ", usingColumns));
			sb.append(')');
		}
		if (onColumnsFromLeftTable != null && onColumnsFromRightTable != null) {
			List<String> conditions = new ArrayList<>(onColumnsFromLeftTable.length);
			for (int i = 0; i < onColumnsFromLeftTable.length; i++) {
				conditions.add(leftTable.getTableInfo().getName() + '.' + onColumnsFromLeftTable[i]
						+ " = " + rightTable.getTableInfo().getName() + '.' + onColumnsFromRightTable[i]);
			}
			sb.append(" ON (");
			sb.append(TextUtils.join(" AND ", conditions));
			sb.append(')');
		}
		sb.append(')');
		return sb.toString();

	}

	private String getJoinString() {
		switch (joinType) {
			case CROSS_INNER:
				return "CROSS JOIN";
			case INNER:
				return "JOIN";
			case LEFT_OUTER:
				return "LEFT OUTER JOIN";
			case NATURAL_INNER:
				return "NATURAL JOIN";
			case NATURAL_LEFT_OUTER:
				return "NATURAL LEFT OUTER JOIN";
			default:
				throw new RuntimeException("join is not implemented for type " + joinType);
		}

	}

	private List<String> getUnionColumns() {
		if (isJoinNatural()) {
			return getAllCommonColumns();
		} else if (usingColumns != null) {
			return Arrays.asList(usingColumns);
		} else if (onColumnsFromLeftTable != null && onColumnsFromRightTable != null) {
			List<String> result = new ArrayList<>();
			for (int i = 0; i < onColumnsFromLeftTable.length; i++) {
				if (onColumnsFromLeftTable[i].equals(onColumnsFromRightTable[i])) {
					result.add(onColumnsFromLeftTable[i]);
				}
			}
			return result;
		} else {
			return Collections.emptyList();
		}
	}

	private List<String> getAllCommonColumns() {
		List<String> result = new ArrayList<>();
		TableInfo rightTableInfo = rightTable.getTableInfo();
		for (ColumnInfo column : leftTable.getTableInfo().getColumns()) {
			String columnName = column.getName();
			if (rightTableInfo.getColumnInfo(columnName) != null) {
				result.add(columnName);
			}
		}
		return result;
	}

	private boolean isJoinNatural() {
		return joinType == JoinType.NATURAL_INNER || joinType == JoinType.NATURAL_LEFT_OUTER;
	}

	private void checkReferencesUsage(ColumnInfo columnFromLeftTable, ColumnInfo columnFromRightTable) {
		boolean success;

		if (columnFromLeftTable != null && columnFromRightTable != null) {
			boolean isLeftColumnReference = columnFromLeftTable.isReferenceToTable();
			boolean isRightColumnReference = columnFromRightTable.isReferenceToTable();
			if (isLeftColumnReference && isRightColumnReference) {
				success = columnFromLeftTable.getReference().equals(columnFromRightTable.getReference());
			} else {
				success = (!isLeftColumnReference) && (!isRightColumnReference);
			}
		} else {
			if (columnFromLeftTable != null) {
				success = !columnFromLeftTable.isReferenceToTable();
			} else {
				success = columnFromRightTable == null || !columnFromRightTable.isReferenceToTable();
			}
		}
		if (!success) {
			throw new IllegalUsageException("invalid usage of references in join");
		}
	}

}
