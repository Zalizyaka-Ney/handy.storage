package handy.storage;

import android.text.TextUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import handy.storage.ColumnInfo.ColumnId;
import handy.storage.TableInfo.Builder;
import handy.storage.api.ColumnType;
import handy.storage.base.QueryParams;
import handy.storage.exception.InvalidDatabaseSchemaException;
import handy.storage.log.DatabaseLog;

/**
 * Creates {@link TableInfo instances for special cases.}
 */
final class TableInfoFactory {

	private TableInfoFactory() {
	}

	static TableInfo createJoinTableInfo(String tableName, String joinedEntity, TableInfo leftTable, TableInfo rightTable,
			Collection<String> unionColumns) {

		TableInfo.Builder builder = new Builder(tableName, addAliasIfNeeded(joinedEntity, tableName));
		addNotUnionColumns(leftTable, unionColumns, builder);
		addNotUnionColumns(rightTable, unionColumns, builder);
		addUnionColumns(leftTable, rightTable, unionColumns, builder);
		return builder.build();
	}

	private static void addNotUnionColumns(TableInfo table, Collection<String> unionColumns, TableInfo.Builder builder) {
		for (ColumnInfo column : table.getColumns()) {
			if (!unionColumns.contains(column.getName())) {
				ColumnId columnId = column.getColumnId();
				ColumnId newColumnId = new ColumnId(columnId.getName(), columnId.getTable(), null);
				ColumnInfo newTableColumn = new ColumnInfo.Builder(newColumnId, column.getType())
						.setFieldType(column.getFieldType())
						.build();
				ColumnInfo.copyReferenceInfo(column, newTableColumn);
				builder.addColumn(newTableColumn);
			}
		}
	}

	private static void addUnionColumns(TableInfo left, TableInfo right, Collection<String> unionColumns, TableInfo.Builder builder) {
		String leftTableName = left.getName();
		String rightTableName = right.getName();
		for (String columnName : unionColumns) {
			ColumnInfo column = left.getColumnInfo(columnName);
			ColumnInfo joinColumn = new ColumnInfo.Builder(new ColumnId(columnName), column.getType())
					.setOwnerTables(leftTableName, rightTableName)
					.setFieldType(column.getFieldType())
					.build();
			ColumnInfo.copyReferenceInfo(column, joinColumn);
			builder.addColumn(joinColumn);
		}
	}

	static TableInfo getTableInfoForProjection(TableInfo entityTableInfo, TableInfo projectionTableInfo) {
		boolean entityIsSelect = entityTableInfo.isSelect();
		TableInfo.Builder tableBuilder = new TableInfo.Builder(entityTableInfo.getName(), entityTableInfo.getEntity());
		tableBuilder.setIsSelect(entityIsSelect);
		Map<ColumnInfo, ColumnInfo> columnsMap = checkProjectionColumns(entityTableInfo, projectionTableInfo);

		// TODO: reuse table info from projection?
		for (ColumnInfo projectionColumn : projectionTableInfo.getColumns()) {
			ColumnInfo entityColumn = columnsMap.get(projectionColumn);
			ColumnId columnId;
			if (entityColumn != null) {
				columnId = entityColumn.getColumnId();
			} else {
				columnId = projectionColumn.getColumnId();
				if (entityIsSelect) {
					columnId = columnId.withTableName("");
				}
			}
			ColumnInfo.Builder columnBuilder = new ColumnInfo.Builder(columnId, null)
					.setField(projectionColumn.getField())
					.setFieldType(projectionColumn.getFieldType()) // if field is null
					.setEntity(projectionColumn.getEntity());
			if (entityColumn != null) {
				columnBuilder.setAliases(entityColumn.getAliases());
				columnBuilder.setFlags(entityColumn.getFlags());
			}

			ColumnInfo resultColumn = columnBuilder.build();
			ColumnInfo.copyReferenceInfo(entityColumn, resultColumn); // this rewrites field's type
			tableBuilder.addColumn(resultColumn);
		}
		return tableBuilder.build();
	}

	private static Map<ColumnInfo, ColumnInfo> checkProjectionColumns(TableInfo entityTableInfo, TableInfo projectionTableInfo) {
		boolean entityIsSelect = entityTableInfo.isSelect();
		Map<ColumnInfo, ColumnInfo> map = new HashMap<>();
		for (ColumnInfo projectionColumn : projectionTableInfo.getColumns()) {
			if (projectionColumn.isAliasForEntity()) {
				continue;
			}

			ColumnId projectionColumnId = projectionColumn.getColumnId();
			ColumnInfo entityColumn = entityTableInfo.getColumnInfo(projectionColumnId.getTable(), projectionColumnId.getName());
			if (entityColumn == null) {
				entityColumn = getColumnByName(projectionColumnId.getName(), entityTableInfo);
			}
			if (entityColumn == null) {
				throw new InvalidDatabaseSchemaException("Can't resolve column " + projectionColumnId.getName());
			}
			String projectionColumnTable = projectionColumnId.getTable();
			if (entityIsSelect && !TextUtils.isEmpty(projectionColumnTable) && !projectionColumnTable.equals(entityColumn.getColumnId().getTable())) {
					DatabaseLog.w("Invalid reference to " + projectionColumnId.getFullName()
					+ ". Can't use reference to table in table obtained from a SelectOperation operation.");
			}

			map.put(projectionColumn, entityColumn);
		}
		return map;
	}

	private static ColumnInfo getColumnByName(String columnName, TableInfo tableInfo) {
		ColumnInfo columnInfo = null;
		for (ColumnInfo column : tableInfo.getColumns()) {
			if (column.getName().equals(columnName)) {
				if (columnInfo != null) {
					throw new InvalidDatabaseSchemaException("Can't resolve column " + columnName
							+ ". There are more than one columns with this name");
				} else {
					columnInfo = column;
				}
			}
		}
		return columnInfo;
	}

	static TableInfo createSelectionTableInfo(String tableName, List<ColumnInfo> columns, String rawSelectionQuery) {
		TableInfo.Builder tableInfoBuilder = new Builder(tableName, addAliasIfNeeded("(" + rawSelectionQuery + ")", tableName));
		tableInfoBuilder.setIsSelect(true);
		for (ColumnInfo column : columns) {
			ColumnId originalColumnId = column.getColumnId();
			ColumnId columnId = new ColumnId(originalColumnId.getName(), tableName, originalColumnId.getFieldName());
			ColumnInfo.Builder columnBuilder = new ColumnInfo.Builder(columnId, column.getType());
			columnBuilder.setFlags(column.getFlags());
			ColumnInfo newColumn = columnBuilder.build();
			ColumnInfo.copyReferenceInfo(column, newColumn);
			tableInfoBuilder.addColumn(newColumn);
		}
		return tableInfoBuilder.build();
	}

	static String addAliasIfNeeded(String entity, String alias) {
		if (TextUtils.isEmpty(alias)) {
			return entity;
		} else {
			return entity + " AS " + alias;
		}
	}

	static TableInfo createAliasTableInfo(TableInfo originalTableInfo, String tableAlias) {
		String newTableEntity = addAliasIfNeeded(originalTableInfo.getEntity(), tableAlias);
		TableInfo.Builder tableInfoBuilder = new Builder(tableAlias, newTableEntity);
		//tableInfoBuilder.setIsSelect(originalTableInfo.isSelect());
		for (ColumnInfo column : originalTableInfo.getColumns()) {
			ColumnId originalColumnId = column.getColumnId();
			ColumnId columnId = new ColumnId(originalColumnId.getName(), tableAlias, originalColumnId.getFieldName());
			ColumnInfo newColumn = cloneColumn(column, columnId);
			tableInfoBuilder.addColumn(newColumn);
		}
		return tableInfoBuilder.build();
	}

	private static ColumnInfo cloneColumn(ColumnInfo column, ColumnId columnId) {
		ColumnInfo.Builder columnBuilder = new ColumnInfo.Builder(columnId, column.getType());
		columnBuilder.setField(column.getField());
		columnBuilder.setFieldType(column.getFieldType());
		columnBuilder.setFlags(column.getFlags());
		//columnBuilder.setOnConflict(column.getOnConflict());
		ColumnInfo newColumn = columnBuilder.build();
		ColumnInfo.copyReferenceInfo(column, newColumn);
		return newColumn;
	}
	
	static TableInfo createSingleColumnTableInfo(TableInfo originalTableInfo, boolean distinct, String column, Expression selection) {
		ColumnInfo columnInfo = originalTableInfo.getColumnInfo(column);
		if (columnInfo == null) {
			DatabaseLog.d("didn't find a column " + column);
			columnInfo = new ColumnInfo.Builder(new ColumnId(column), ColumnType.TEXT).build();
		}
		String newTableEntity = "(" + new QueryParams()
			.columns(columnInfo.getEntityDeclaration())
			.from(originalTableInfo.getEntity())
			.where(selection != null ? selection.toString() : null)
			.distinct(distinct)
			.toRawSqlQuery()
			+ ")";
		TableInfo.Builder tableInfoBuilder = new Builder("", newTableEntity);
		tableInfoBuilder.setIsSelect(true);
		ColumnInfo newColumn = cloneColumn(columnInfo, new ColumnId(column));
		ColumnInfo.copyReferenceInfo(columnInfo, newColumn);
		tableInfoBuilder.addColumn(newColumn);
		return tableInfoBuilder.build();
	}

}
