package handy.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import handy.storage.api.Model;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.QueryParams;
import handy.storage.log.PerformanceTimer;
import handy.storage.util.Factory;

/**
 * Holds main database objects.
 */
class DatabaseCore {

	private final DatabaseInfo databaseInfo;
	private List<TableInfo> databaseTables;
	private final DatabaseConfiguration configuration;
	private final DataAdapters dataAdapters;
	private TablesFactory tablesFactory;

	DatabaseCore(DatabaseInfo databaseInfo, DatabaseConfiguration configuration, DataAdapters dataAdapters) {
		this.databaseInfo = databaseInfo;
		this.configuration = configuration;
		this.dataAdapters = dataAdapters;
	}

	DatabaseInfo getDatabaseInfo() {
		return databaseInfo;
	}

	TablesFactory getTablesFactory() {
		return tablesFactory;
	}

	DatabaseConfiguration getConfiguration() {
		return configuration;
	}

	DataAdapters getDataAdapters() {
		return dataAdapters;
	}

	List<TableInfo> getTables() {
		return databaseTables;
	}

	boolean hasTable(Class<? extends Model> modelClass) {
		for (TableInfo table : databaseTables) {
			if (modelClass.equals(table.getOriginClass())) {
				return true;
			}
		}
		return false;
	}

	TableInfo getTableInfo(Class<? extends Model> modelClass) {
		for (TableInfo table : databaseTables) {
			if (modelClass.equals(table.getOriginClass())) {
				return table;
			}
		}
		return null;
	}

	TableInfo getTableInfo(String tableName) {
		for (TableInfo table : databaseTables) {
			if (tableName.equals(table.getName())) {
				return table;
			}
		}
		return null;
	}

	void initTablesFactory(DatabaseAdapter databaseAdapter) {
		tablesFactory = new TablesFactory(databaseAdapter);
	}

	void prepareTables() {
		PerformanceTimer.startInterval("parse table models");
		TablesCreator tablesCreator = new TablesCreator(configuration, dataAdapters);
		List<TableInfo> tables = tablesCreator.parseTables(databaseInfo.getRegisteredModels());
		databaseTables = new ArrayList<>(tables.size());
		for (TableInfo table : tables) {
			databaseTables.add(table);
		}
		PerformanceTimer.endInterval();
	}

	/**
	 * Create table instances.
	 */
	final class TablesFactory {

		private final DatabaseAdapter databaseAdapter;
		private final Map<Class<?>, TableInfo> tableInfoCache = new HashMap<>();
		private final Map<Class<?>, TableInfo> projectionInfoCache = new HashMap<>();

		private TablesFactory(DatabaseAdapter databaseAdapter) {
			this.databaseAdapter = databaseAdapter;
		}

		private TableInfo getInfoForTable(Class<? extends Model> modelClass) {
			TableInfo result = tableInfoCache.get(modelClass);
			if (result == null) {
				result = getTableInfo(modelClass);
				tableInfoCache.put(modelClass, result);
			}
			return result;
		}

		private TableInfo getInfoForProjection(Class<? extends Model> modelClass) {
			TableInfo result = projectionInfoCache.get(modelClass);
			if (result == null) {
				result = TableParser.parseProjectionTableInfo(modelClass, configuration);
				projectionInfoCache.put(modelClass, result);
			}
			return result;
		}

		<T extends Model> WritableTable<T> createTable(Class<T> modelClass) {
			if (hasTable(modelClass)) {
				TableInfo tableInfo = getInfoForTable(modelClass);
				return new WritableTable<>(modelClass, tableInfo, databaseAdapter, DatabaseCore.this);
			} else {
				throw new IllegalArgumentException(modelClass.getName() + " is not registered as a table");
			}
		}

		<T extends Model> ReadableTable<T> createProjection(Class<T> modelClass, TableInfo baseTableInfo, Factory<QueryParams> queryParamsFactory) {
			TableInfo modelTableInfo = getInfoForProjection(modelClass);
			return new ReadableTable<>(
				modelClass,
				TableInfoFactory.getTableInfoForProjection(baseTableInfo, modelTableInfo),
				databaseAdapter,
				DatabaseCore.this,
				queryParamsFactory
			);
		}

		<T extends Model> ReadableTable<T> createProjection(Class<T> modelClass, TableInfo baseTableInfo) {
			return createProjection(modelClass, baseTableInfo, QueryParams.DEFAULT_FACTORY);
		}

	}

}
