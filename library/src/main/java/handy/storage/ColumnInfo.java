package handy.storage;

import android.text.TextUtils;

import java.lang.reflect.Field;

import handy.storage.api.Action;
import handy.storage.api.ColumnType;
import handy.storage.api.Model;
import handy.storage.api.Value;
import handy.storage.base.OnConflictStrategy;
import handy.storage.exception.IllegalUsageException;
import handy.storage.util.ClassCast;

/**
 * Describes database field.
 */
final class ColumnInfo {

	static final int NOT_NULL = 1;
	static final int UNIQUE = 2;
	static final int PRIMARY_KEY = 4;
	private static final int AUTO_INCREMENT = 8; // hidden, because it should be used only for private key field
	static final int PRIMARY_KEY_AUTO_INCREMENT = AUTO_INCREMENT | PRIMARY_KEY;

	private Field field;
	private Class<?> fieldType;
	private final ColumnId columnId;
	private ColumnId[] aliases;
	private ColumnType columnType;
	private int flags;
	private String entity;
	private boolean isReference = false;
	private ReferenceInfo referenceToTable;

	private OnConflictStrategy onConflictStrategy = OnConflictStrategy.DEFAULT;

	private ColumnInfo(ColumnId columnId, ColumnType columnType) {
		this.columnId = columnId;
		this.columnType = columnType;
	}

	boolean isReferenceToTable() {
		return isReference;
	}

	Class<? extends Model> getReferencedTable() {
		return referenceToTable.modelClass;
	}

	ReferenceInfo getReference() {
		return referenceToTable;
	}

	String getDescription() {
		return "name = " + getAllAliasesString()
				+ ", fieldType = " + (fieldType != null ? fieldType.getName() : "null")
				+ ", field is null = " + (field == null)
				+ ", flags = " + flags
				+ ", is reference = " + (referenceToTable != null);
	}

	String getEntity() {
		return entity;
	}

	private String getAllAliasesString() {
		StringBuilder sb = new StringBuilder(getFullName());
		if (aliases.length > 1) {
			sb.append(" (");
			for (int i = 1; i < aliases.length; i++) {
				if (i > 1) {
					sb.append(", ");
				}
				sb.append(aliases[i].toString());
			}
			sb.append(")");
		}
		return sb.toString();
	}

	ColumnId[] getAliases() {
		return aliases;
	}

	boolean isAliasForEntity() {
		return !TextUtils.isEmpty(entity);
	}

	@Override
	public String toString() {
		return "Column [name=" + columnId + ", type=" + columnType + ", flags=" + flags + ", entity = " + entity + "]";
	}

	Class<?> getFieldType() {
		return fieldType;
	}

	OnConflictStrategy getOnConflictStrategy() {
		return onConflictStrategy;
	}

	String getName() {
		return columnId.getName();
	}

	ColumnId getColumnId() {
		return columnId;
	}

	String getEntityDeclaration() {
		if (TextUtils.isEmpty(entity)) {
			return getDetailedColumnId().getFullName();
		} else {
			return entity + " AS " + getName();
		}
	}

	String getFullName() {
		if (TextUtils.isEmpty(entity)) {
			return getDetailedColumnId().getFullName();
		} else {
			return getName();
		}
	}

	private ColumnId getDetailedColumnId() {
		for (int i = 1; i < aliases.length; i++) {
			ColumnId alias = aliases[i];
			if (!TextUtils.isEmpty(alias.getTable())) {
				return alias;
			}
		}
		return columnId;
	}

	ColumnType getType() {
		return columnType;
	}

	int getFlags() {
		return flags;
	}

	static boolean isFlagPresent(int flags, int flag) {
		return (flag & flags) == flag;
	}

	boolean isFlagSet(int flag) {
		return isFlagPresent(flags, flag);
	}

	boolean isPrimaryKeyFlagSet() {
		return isFlagSet(PRIMARY_KEY);
	}

	boolean isNotNullFlagSet() {
		return isFlagSet(NOT_NULL);
	}

	boolean isUniqueFlagSet() {
		return isFlagSet(UNIQUE);
	}

	boolean isAutoIncrementFlagSet() {
		return isFlagSet(AUTO_INCREMENT);
	}

	Field getField() {
		return field;
	}

	boolean isForeignKey() {
		return referenceToTable != null;
	}

	ReferenceInfo getForeignKeyInfo() {
		return referenceToTable;
	}

	static void bindForeignColumn(ColumnInfo columnInfo, ColumnInfo foreignColumn, String foreignTableName) {
		columnInfo.referenceToTable.setForeignColumn(foreignColumn);
		columnInfo.referenceToTable.foreignTableName = foreignTableName;
		columnInfo.columnType = foreignColumn.columnType;
		columnInfo.fieldType = foreignColumn.fieldType;
	}

	static void copyReferenceInfo(ColumnInfo fromColumn, ColumnInfo toColumn) {
		if (fromColumn != null && fromColumn.isReference) {
			toColumn.referenceToTable = new ReferenceInfo(fromColumn.referenceToTable);
			toColumn.fieldType = fromColumn.fieldType;
			toColumn.isReference = true;
		} 
	}

	static ColumnInfo createVirtualColumn(String table, String alias, String entity) {
		return new ColumnInfo.Builder(new ColumnId(alias, table), ColumnType.TEXT).setEntity(entity).build();
	}

	static ColumnInfo createQueryColumnInfo(TableInfo tableInfo, Value value, Class<?> valueClass, ColumnType columnType) {
		String alias = value.getAlias();
		String entity = value.getEntity();
		String columnName = value.getName();
		ColumnInfo originalColumn = tableInfo.getColumnInfo(columnName);
		if (originalColumn != null && !ClassCast.isValueAssignable(originalColumn.getField().getType(), valueClass)) {
			throw new IllegalUsageException(String.format("wrong type for column '%s', expected %s, got %s",
				columnName, originalColumn.getField().getType().getName(), valueClass.getName())
			);
		}

		ColumnId columnId = TextUtils.isEmpty(alias)
			? originalColumn != null ? originalColumn.getColumnId() : new ColumnId(columnName)
			: new ColumnId(alias);
		ColumnInfo columnInfo = new ColumnInfo.Builder(columnId, columnType)
			.setFieldType(valueClass)
			.setEntity(columnName.equals(entity) ? null : entity)
			.build();
		if (originalColumn != null && originalColumn.isReferenceToTable()) {
			ColumnInfo.copyReferenceInfo(originalColumn, columnInfo);
		}
		return columnInfo;
	}


	/**
	 * Values which identify the column.
	 */
	static class ColumnId {

		private final String name;
		private final String table;
		private final String fieldName;

		ColumnId(String name) {
			this(name, "");
		}

		ColumnId(String name, String table) {
			this(name, table, null);
		}

		ColumnId(String name, String table, String fieldName) {
			this.name = name;
			this.table = table;
			this.fieldName = fieldName;
		}

		String getFieldName() {
			return fieldName;
		}

		String getName() {
			return name;
		}

		String getFullName() {
			return Table.fullColumnName(table, name);
		}

		@Override
		public String toString() {
			return getFullName();
		}

		String getTable() {
			return table;
		}

		boolean isTableSet() {
			return !TextUtils.isEmpty(table);
		}

		ColumnId withTableName(String tableName) {
			return new ColumnId(name, tableName, fieldName);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((table == null) ? 0 : table.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ColumnId other = (ColumnId) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (TextUtils.isEmpty(table)) {
				if (!TextUtils.isEmpty(other.table)) {
					return false;
				}
			} else if (!table.equals(other.table)) {
				return false;
			}
			return true;
		}

	}

	/**
	 * Builds ColumnInfo.
	 */
	static class Builder {

		private final ColumnInfo columnInfo;

		Builder(ColumnId columnId, ColumnType type) {
			columnInfo = new ColumnInfo(columnId, type);
		}

		ColumnInfo build() {
			if (columnInfo.aliases == null) {
				columnInfo.aliases = new ColumnId[] {columnInfo.columnId };
			}
			return columnInfo;
		}

		Builder setField(Field field) {
			columnInfo.fieldType = field != null ? field.getType() : null;
			columnInfo.field = field;
			return this;
		}

		Builder setFieldType(Class<?> fieldType) {
			columnInfo.fieldType = fieldType;
			return this;
		}

		Builder setOnConflictStrategy(OnConflictStrategy onConflictAction) {
			columnInfo.onConflictStrategy = onConflictAction;
			return this;
		}

		Builder setFlags(int flags) {
			columnInfo.flags = flags;
			return this;
		}

		Builder setReferencedTo(Class<? extends Model> referenceToTable, Action onUpdateAction, Action onDeleteAction) {
			setForeignKeyTo(referenceToTable, "", onUpdateAction, onDeleteAction);
			columnInfo.isReference = true;
			return this;
		}

		Builder setForeignKeyTo(Class<? extends Model> referenceToTable, String column, Action onUpdateAction, Action onDeleteAction) {
			columnInfo.referenceToTable = new ReferenceInfo(referenceToTable, onUpdateAction, onDeleteAction);
			columnInfo.referenceToTable.foreignColumnName = column;
			return this;
		}

		Builder setOwnerTables(String... tables) {
			ColumnId[] aliases = new ColumnId[tables.length + 1];
			ColumnId baseColumnId = columnInfo.columnId;
			aliases[0] = baseColumnId;
			for (int i = 0; i < tables.length; i++) {
				aliases[i + 1] = baseColumnId.withTableName(tables[i]);
			}
			columnInfo.aliases = aliases;
			return this;
		}

		Builder setEntity(String entity) {
			columnInfo.entity = entity;
			return this;
		}

		Builder setAliases(ColumnId[] aliases) {
			columnInfo.aliases = aliases;
			return this;
		}
	}

	/**
	 * Reference to another table.
	 */
	static class ReferenceInfo {
		private Class<? extends Model> modelClass;
		private String foreignColumnName;
		private ColumnInfo foreignColumn;
		private String foreignTableName;
		private Action onUpdateAction;
		private Action onDeleteAction;

		ReferenceInfo(Class<? extends Model> modelClass, Action onUpdateAction, Action onDeleteAction) {
			this.modelClass = modelClass;
			this.onUpdateAction = onUpdateAction;
			this.onDeleteAction = onDeleteAction;
		}

		ReferenceInfo(ReferenceInfo anotherReference) {
			modelClass = anotherReference.modelClass;
			foreignColumn = anotherReference.foreignColumn;
			foreignTableName = anotherReference.foreignTableName;
			onDeleteAction = anotherReference.onDeleteAction;
			onUpdateAction = anotherReference.onUpdateAction;
		}

		Class<? extends Model> getModelClass() {
			return modelClass;
		}

		ColumnInfo getForeignColumn() {
			return foreignColumn;
		}

		String getForeignTableName() {
			return foreignTableName;
		}

		Action getOnUpdateAction() {
			return onUpdateAction;
		}

		Action getOnDeleteAction() {
			return onDeleteAction;
		}

		String getForeignColumnName() {
			return foreignColumnName;
		}

		private void setForeignColumn(ColumnInfo foreignColumn) {
			this.foreignColumn = foreignColumn;
			foreignColumnName = foreignColumn.getName();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + foreignColumn.getName().hashCode();
			result = prime * result + foreignTableName.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ReferenceInfo other = (ReferenceInfo) obj;
			if (!foreignColumn.getName().equals(other.foreignColumn.getName())) {
				return false;
			} else if (!modelClass.equals(other.modelClass)) {
				return false;
			}
			return true;
		}

	}

}
