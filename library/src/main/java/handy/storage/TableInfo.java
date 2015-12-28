package handy.storage;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import handy.storage.ColumnInfo.ColumnId;
import handy.storage.ColumnInfo.ReferenceInfo;
import handy.storage.annotation.CompositeUnique;
import handy.storage.api.Action;
import handy.storage.api.Model;
import handy.storage.base.OnConflictStrategy;
import handy.storage.exception.InvalidDatabaseSchemaException;
import handy.storage.log.DatabaseLog;

/**
 * Describes database table.
 */
final class TableInfo {

	private Class<? extends Model> originClass;
	private String entity;
	private String name;
	private List<ColumnInfo> columnInfos;
	private Map<String, ColumnInfo> columnMap = new HashMap<>();
	private Set<String> ambiguousColumnNames = new HashSet<>();
	private List<String> primaryKeyColumns;
	private Set<UniqueRestriction> compositeUniques = Collections.emptySet();
	private OnConflictStrategy primaryKeySetOnConflict;
	private boolean isSelect = false;

	private TableInfo() {
	}

	void logItself() {
		DatabaseLog.i("Table info");
		DatabaseLog.i("Table info: name = " + name);
		DatabaseLog.i("Table info: entity = " + entity);
		DatabaseLog.i("Table info: origin class = " + (originClass == null ? "is null" : originClass.getName()));
		DatabaseLog.i("Table info: isSelect = " + isSelect);
		int index = 1;
		for (ColumnInfo column : columnInfos) {
			DatabaseLog.i("Table info: column#" + (index++) + " - " + column.getDescription());
		}
	}

	boolean isSelect() {
		return isSelect;
	}

	Class<? extends Model> getOriginClass() {
		return originClass;
	}

	String getEntity() {
		return entity;
	}

	String getName() {
		return name;
	}

	List<ColumnInfo> getColumns() {
		return columnInfos;
	}

	ColumnInfo getColumnInfo(String column) {
		String columnName = column.toLowerCase();
		if (ambiguousColumnNames.contains(columnName)) {
			throw new IllegalArgumentException("Ambiguous column name: " + column);
		}
		return columnMap.get(columnName);
	}

	ColumnInfo getColumnInfo(String table, String column) {
		return getColumnInfo(Table.fullColumnName(table, column));
	}

	String getCreateQuery() {
		List<String> columnDescriptions = new ArrayList<>(columnInfos.size());
		for (ColumnInfo columnInfo : columnInfos) {
			columnDescriptions.add(columnDescription(columnInfo));
		}
		if (!primaryKeyColumns.isEmpty()) {
			columnDescriptions.add(primaryKeySetDescription());
		}
		for (UniqueRestriction restriction : compositeUniques) {
			columnDescriptions.add(restriction.toDeclarationString());
		}
		for (ColumnInfo column : columnInfos) {
			if (column.isForeignKey()) {
				columnDescriptions.add(referenceDescription(column));
			}
		}
		return "CREATE TABLE IF NOT EXISTS " + name + " (" + TextUtils.join(", ", columnDescriptions) + ')';
	}

	private String referenceDescription(ColumnInfo column) {
		ReferenceInfo ref = column.getForeignKeyInfo();
		StringBuilder sb = new StringBuilder("FOREIGN KEY(");
		sb.append(column.getName());
		sb.append(") REFERENCES ");
		sb.append(ref.getForeignTableName());
		sb.append('(');
		sb.append(ref.getForeignColumn().getName());
		sb.append(')');
		appendAction(sb, "ON UPDATE", ref.getOnUpdateAction());
		appendAction(sb, "ON DELETE", ref.getOnDeleteAction());
		sb.append(" DEFERRABLE INITIALLY DEFERRED");
		return sb.toString();
	}

	private String primaryKeySetDescription() {
		StringBuilder privateKeyDefinition = new StringBuilder()
			.append("PRIMARY KEY (")
			.append(TextUtils.join(", ", primaryKeyColumns))
			.append(')');
		appendOnConflict(privateKeyDefinition, primaryKeySetOnConflict);
		return privateKeyDefinition.toString();
	}

	private void appendAction(StringBuilder sb, String when, Action action) {
		if (!action.isDefault()) {
			sb.append(' ');
			sb.append(when);
			sb.append(' ');
			sb.append(action.toSqliteSyntax());
		}
	}

	private String columnDescription(ColumnInfo columnInfo) {
		StringBuilder columnDescription = new StringBuilder(columnInfo.getName())
				.append(' ')
				.append(columnInfo.getType().name());
		if (columnInfo.isPrimaryKeyFlagSet()) {
			columnDescription.append(" PRIMARY KEY");
			OnConflictStrategy onConflict = columnInfo.getOnConflictStrategy();
			appendOnConflict(columnDescription, onConflict);
			if (columnInfo.isAutoIncrementFlagSet()) {
				columnDescription.append(" AUTOINCREMENT");
			}
		}
		if (columnInfo.isNotNullFlagSet()) {
			columnDescription.append(" NOT NULL");
		}
		if (columnInfo.isUniqueFlagSet()) {
			columnDescription.append(" UNIQUE");
			OnConflictStrategy onConflict = columnInfo.getOnConflictStrategy();
			appendOnConflict(columnDescription, onConflict);
		}
		return columnDescription.toString();
	}

	private static void appendOnConflict(StringBuilder stringBuilder, OnConflictStrategy onConflict) {
		if (onConflict != OnConflictStrategy.DEFAULT) {
			stringBuilder.append(" ON CONFLICT ");
			stringBuilder.append(onConflict.name());
		}
	}

	String getDeleteQuery() {
		return "DROP TABLE IF EXISTS " + name;
	}

	ColumnInfo getReferenceableColumn() {
		ColumnInfo rowidColumn = null;
		boolean hasRestriction = !compositeUniques.isEmpty();
		for (ColumnInfo column : columnInfos) {
			boolean isUnique = column.isUniqueFlagSet();
			if (column.isNotNullFlagSet()) {
				if (column.isPrimaryKeyFlagSet() && !column.isAutoIncrementFlagSet()) {
					return column;
				}
				if (isUnique) {
					return column;
				}
			}
			hasRestriction |= isUnique;
			if (column.isFlagSet(ColumnInfo.PRIMARY_KEY_AUTO_INCREMENT)) {
				rowidColumn = column;
			}
		}
		if (rowidColumn != null && hasRestriction) {
			// TODO: maybe throw an exception in this case?
			DatabaseLog.w("The only column in table " + getName() + " that can be used as a foreign key is rowid (PRIMARY KEY AUTOINCREMENT). "
					+ "The table also has an unique column or a set of columns, so rowid can change unexpectedly during insertOrReplace operation. "
					+ "Please, review your table's schema to avoid possible issues.");
		}
		if (rowidColumn == null) {
			DatabaseLog.e("Can't find column in table " + getName() + " that can be used as a foreign key. "
					+ "Please, declare some column as PRIMARY KEY NOT NULL, UNIQUE NOT NULL or PRIMARY KEY AUTOINCREMENT (not recommended).");
		}
		return rowidColumn;
	}

	ColumnInfo getRowIdColumn() {
		return getFirstColumnWithFlag(ColumnInfo.PRIMARY_KEY_AUTO_INCREMENT);
	}

	ColumnInfo getPrimaryKeyColumn() {
		return getFirstColumnWithFlag(ColumnInfo.PRIMARY_KEY);
	}

	private ColumnInfo getFirstColumnWithFlag(int flag) {
		for (ColumnInfo column : columnInfos) {
			if (column.isFlagSet(flag)) {
				return column;
			}
		}
		return null;
	}

	ColumnInfo getUniqueColumn() {
		ColumnInfo result = null;
		for (ColumnInfo column : columnInfos) {
			if (column.isPrimaryKeyFlagSet()) {
				return column;
			}
			if (column.isUniqueFlagSet()) {
				result = column;
			}
		}
		return result;
	}

	/**
	 * Helps build {@link TableInfo} object.
	 */
	static final class Builder {

		private final TableInfo tableInfo = new TableInfo();

		private Map<ColumnId, ColumnInfo> columnInfos = new HashMap<>();

		Builder(String tableName) {
			this(tableName, tableName);
		}

		Builder(String tableName, String tableEntity) {
			tableInfo.name = tableName;
			tableInfo.entity = tableEntity;
			tableInfo.primaryKeyColumns = new ArrayList<>();
			tableInfo.primaryKeySetOnConflict = OnConflictStrategy.DEFAULT;
		}

		Builder setOriginClass(Class<? extends Model> modelClass) {
			tableInfo.originClass = modelClass;
			return this;
		}

		Builder addColumn(ColumnInfo columnInfo) {
			ColumnId columnId = columnInfo.getColumnId();
			if (columnInfos.containsKey(columnInfo.getColumnId())) {
				throw new InvalidDatabaseSchemaException("column " + columnId.getFullName()
						+ " has been already added to table definition");
			}
			columnInfos.put(columnId, columnInfo);
			return this;
		}

		Builder setPrimaryKeyColumns(OnConflictStrategy onConflictStrategy, String... keyColumns) {
			checkRestrictionColumns(keyColumns);
			tableInfo.primaryKeySetOnConflict = onConflictStrategy;
			tableInfo.primaryKeyColumns.clear();
			tableInfo.primaryKeyColumns.addAll(Arrays.asList(keyColumns));
			return this;
		}

		Builder setCompositeUniques(CompositeUnique[] compositeUniques) {
			tableInfo.compositeUniques = new TreeSet<>();
			if (compositeUniques.length > 0) {
				for (CompositeUnique compositeUnique : compositeUniques) {
					checkRestrictionColumns(compositeUnique.columns());
					UniqueRestriction restriction = new UniqueRestriction(compositeUnique);
					if (tableInfo.compositeUniques.contains(restriction)) {
						TableParser.throwDeclarationException(tableInfo.originClass,
							"Duplicate " + CompositeUnique.class.getName() + " declaration.");
					}
					tableInfo.compositeUniques.add(restriction);
				}
			}
			return this;
		}

		private void checkRestrictionColumns(String... columns) {
			if (columns.length < 1) {
				TableParser.throwDeclarationException(tableInfo.originClass, "No columns declared in CompositeUnique or CompositePrimaryKey.");
			}
			for (String column : columns) {
				if (!columnInfos.containsKey(new ColumnId(column, tableInfo.name))) {
					TableParser.throwDeclarationException(tableInfo.originClass, "Can't find column \"" + column
							+ "\" declared in CompositeUnique or CompositePrimaryKey");
				}
			}
		}

		Builder setIsSelect(boolean isSelect) {
			tableInfo.isSelect = isSelect;
			return this;
		}

		TableInfo buildDatabaseTable() {
			checkGeneralErrors();
			ensureValidPrimaryKey();
			return build();
		}

		TableInfo build() {
			if (columnInfos.isEmpty()) {
				TableParser.throwDeclarationException(tableInfo.originClass, "No columns in the table.");
			}
			Collection<ColumnInfo> tableColumns = columnInfos.values();
			tableInfo.columnInfos = new ArrayList<>(tableColumns);
			for (ColumnInfo column : tableColumns) {
				String simpleName = column.getName().toLowerCase();
				if (tableInfo.columnMap.containsKey(simpleName)) {
					tableInfo.ambiguousColumnNames.add(simpleName);
				}
				tableInfo.columnMap.put(simpleName, column);
				for (ColumnId alias : column.getAliases()) {
					tableInfo.columnMap.put(alias.getFullName().toLowerCase(), column);
				}
			}

			return tableInfo;
		}

		private void ensureValidPrimaryKey() {
			int primaryKeyCount = 0;
			boolean primaryKeyAllowed = tableInfo.primaryKeyColumns.isEmpty();
			for (ColumnInfo columnInfo : columnInfos.values()) {
				if (columnInfo.isPrimaryKeyFlagSet()) {
					if (!primaryKeyAllowed) {
						TableParser.throwDeclarationException(tableInfo.originClass, "Found PRIMARY KEY flag for a database column, "
							+ "but it is already set by CompositePrimaryKey annotation.");
					}
					primaryKeyCount++;
					if (primaryKeyCount > 1) {
						TableParser.throwDeclarationException(tableInfo.originClass, "More than one primary key.");
					}
				}
			}
		}

		private void checkGeneralErrors() {
			if (TextUtils.isEmpty(tableInfo.name)) {
				TableParser.throwDeclarationException(tableInfo.originClass, "Empty table name.");
			}
			if (columnInfos.isEmpty()) {
				TableParser.throwDeclarationException(tableInfo.originClass, "No columns in the table.");
			}
		}
	}

	/**
	 * Represents a table restriction declared with {@link CompositeUnique}
	 * annotation.
	 */
	private static class UniqueRestriction implements Comparable<UniqueRestriction> {
		private final String columns;
		private final OnConflictStrategy onConflictStrategy;

		UniqueRestriction(CompositeUnique annotation) {
			List<String> columnsList = new ArrayList<>(Arrays.asList(annotation.columns()));
			Collections.sort(columnsList);
			columns = TextUtils.join(", ", columnsList);
			onConflictStrategy = annotation.onConflictStrategy();
		}

		@Override
		public int compareTo(UniqueRestriction o) {
			return columns.compareTo(o.columns);
		}

		@Override
		public int hashCode() {
			return columns.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			UniqueRestriction other = (UniqueRestriction) obj;
			return columns.equals(other.columns);
		}

		String toDeclarationString() {
			StringBuilder uniqueSetDefinition = new StringBuilder()
				.append("UNIQUE (")
				.append(columns)
				.append(')');
			appendOnConflict(uniqueSetDefinition, onConflictStrategy);
			return uniqueSetDefinition.toString();
		}

	}

}
