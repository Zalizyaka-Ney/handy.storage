package handy.storage;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import handy.storage.api.Model;
import handy.storage.exception.InvalidDatabaseSchemaException;
import handy.storage.log.DatabaseLog;
import handy.storage.util.ClassCast;

/**
 * Creates tables for the database.
 */
class TablesCreator {

	private final DatabaseConfiguration config;
	private final DataAdapters dataAdapters;

	TablesCreator(DatabaseConfiguration config, DataAdapters dataAdapters) {
		this.config = config;
		this.dataAdapters = dataAdapters;
	}

	List<TableInfo> parseTables(Collection<Class<? extends Model>> registeredModels) {
		Map<Class<?>, TableInfo> databaseTables = parseModelsData(registeredModels);
		List<Class<?>> orderedTables = orderTables(databaseTables);
		List<TableInfo> tables = new ArrayList<>(orderedTables.size());
		for (Class<?> tableClass : orderedTables) {
			tables.add(databaseTables.get(tableClass));
		}
		resolveForeignKeys(tables, databaseTables);
		return tables;
	}

	private void resolveForeignKeys(List<TableInfo> orderedTables, Map<Class<?>, TableInfo> databaseTables) {
		for (TableInfo table : orderedTables) {
			for (ColumnInfo column : table.getColumns()) {
				if (column.isReferenceToTable()) {
					bindReference(column, databaseTables);
				} else if (column.isForeignKey()) {
					bindForeignKey(column, databaseTables);
				}
			}
		}
	}

	private void bindForeignKey(ColumnInfo column, Map<Class<?>, TableInfo> databaseTables) {
		TableInfo referencedTable = databaseTables.get(column.getReferencedTable());
		if (referencedTable == null) {
			throw new InvalidDatabaseSchemaException("Error in ForeignKey declaration: can't find referenced table "
				+ column.getReferencedTable());
		}
		String foreignColumnName = column.getReference().getForeignColumnName();
		ColumnInfo referencedColumn = referencedTable.getColumnInfo(foreignColumnName);
		if (referencedColumn == null) {
			throw new InvalidDatabaseSchemaException("Error in ForeignKey declaration: can't find referenced column "
				+ foreignColumnName);
		}
		if (!ClassCast.isValueAssignable(referencedColumn.getFieldType(), column.getFieldType())) {
			throw new InvalidDatabaseSchemaException(String.format(
				"Error in ForeignKey declaration: column has type %s, but referenced column's type is %s",
				column.getFieldType().getName(), referencedColumn.getFieldType().getName()));
		}
		if (!referencedColumn.isPrimaryKeyFlagSet() && !referencedColumn.isUniqueFlagSet()) {
			throw new InvalidDatabaseSchemaException(String.format(
				"Error in ForeignKey declaration: referenced column \"%s\" must be PrimaryKey or Unique",
				foreignColumnName));
		}
		ColumnInfo.bindForeignColumn(column, referencedColumn, referencedTable.getName());
	}

	private void bindReference(ColumnInfo column, Map<Class<?>, TableInfo> databaseTables) {
		TableInfo referencedTable = databaseTables.get(column.getReferencedTable());
		ColumnInfo uniqueColumn = referencedTable.getReferenceableColumn();
		if (uniqueColumn == null) {
			throw new InvalidDatabaseSchemaException("class " + referencedTable.getName()
				+ " should have a Unique (or PrimaryKey), NotNull column");
		}
		ColumnInfo.bindForeignColumn(column, uniqueColumn, referencedTable.getName());
	}

	private Map<Class<?>, TableInfo> parseModelsData(Collection<Class<? extends Model>> registeredModels) {
		int size = registeredModels.size();
		Set<String> tableNames = new HashSet<>(size);
		Map<Class<?>, TableInfo> databaseTables = new HashMap<>(size);
		for (Class<? extends Model> modelClass : registeredModels) {
			addTable(databaseTables, tableNames, TableParser.parseTableFromIModel(modelClass, config, dataAdapters));
		}
		return databaseTables;
	}

	private void addTable(Map<Class<?>, TableInfo> databaseTables, Set<String> tableNames, TableInfo tableInfo) {
		String tableName = tableInfo.getName();
		if (!tableNames.contains(tableName)) {
			databaseTables.put(tableInfo.getOriginClass(), tableInfo);
			tableNames.add(tableName);
		} else {
			throw new InvalidDatabaseSchemaException("duplicate table " + tableName);
		}
	}

	private Set<Class<?>> getReferencedModels(
		List<String> callingQueue,
		Class<?> modelClass,
		Map<Class<?>, TableInfo> allTables) {

		String modelClassName = modelClass.getName();
		Set<Class<?>> models = new HashSet<>();
		boolean cyclicRef = callingQueue.contains(modelClassName);
		callingQueue.add(modelClassName);
		if (cyclicRef) {
			throw new InvalidDatabaseSchemaException("found a cyclic reference: " + TextUtils.join(" -> ", callingQueue));
		}
		TableInfo tableInfo = allTables.get(modelClass);
		if (tableInfo == null) {
			throw new InvalidDatabaseSchemaException("Referenced table " + modelClass.getName() + " is not registered as a table");
		}
		for (ColumnInfo column : tableInfo.getColumns()) {
			if (column.isForeignKey()) {
				Class<?> referencedTable = column.getReferencedTable();
				models.add(referencedTable);

				Set<Class<?>> nestedReferences = getReferencedModels(new LinkedList<>(callingQueue), referencedTable, allTables);
				models.addAll(nestedReferences);
			}
		}
		return models;
	}

	private List<Class<?>> orderTables(final Map<Class<?>, TableInfo> allTables) {
		Collection<Class<?>> tableClasses = allTables.keySet();
		for (Class<?> tableClass : tableClasses) {
			Set<Class<?>> references = getReferencedModels(new LinkedList<>(), tableClass, allTables);
			if (!references.isEmpty()) {
				DatabaseLog.d(tableClass.getName() + " depends on " + TextUtils.join(", ", references));
			}
		}
		final Map<Class<?>, Integer> dependencyLevels = new HashMap<>();
		for (Class<?> modelClass : tableClasses) {
			int dependencyLevel = getDependencyLevel(modelClass, allTables);
			DatabaseLog.d("dependency level for " + modelClass.getName() + " is " + dependencyLevel);
			dependencyLevels.put(modelClass, dependencyLevel);
		}
		List<Class<?>> sortedTableClasses = new ArrayList<>(tableClasses);
		Collections.sort(sortedTableClasses, (lhs, rhs) -> {
			int ldl = dependencyLevels.get(lhs);
			int rdl = dependencyLevels.get(rhs);
			int result = ldl - rdl;
			if (result == 0) {
				result = allTables.get(lhs).getName().compareTo(allTables.get(rhs).getName()); // sort by table name
			}
			return result;
		});
		DatabaseLog.d("creating the tables in the order:");
		int i = 1;
		for (Class<?> tableClass : sortedTableClasses) {
			DatabaseLog.d(String.format("%d) %s", i++, tableClass.getName()));
		}
		return sortedTableClasses;
	}

	private int getDependencyLevel(Class<?> modelClass, Map<Class<?>, TableInfo> allTables) {
		int result = 0;
		for (ColumnInfo column : allTables.get(modelClass).getColumns()) {
			if (column.isForeignKey()) {
				int columnDependencyLevel = 1 + getDependencyLevel(column.getReferencedTable(), allTables);
				result = Math.max(result, columnDependencyLevel);
			}
		}
		return result;
	}

}
