package handy.storage;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import handy.storage.api.Model;
import handy.storage.log.DatabaseLog;

/**
 * Implements base operations with database schema.
 */
public final class DatabaseSchemaEditor {

	private final SQLiteDatabase database;
	private final List<TableInfo> registeredTables;

	DatabaseSchemaEditor(SQLiteDatabase database, List<TableInfo> registeredTables) {
		this.database = database;
		this.registeredTables = registeredTables;
	}

	/**
	 * Execute the create statement for the registered table (the "IF EXISTS"
	 * keyword is used).
	 */
	public void createTable(String name) {
		for (TableInfo tableInfo : registeredTables) {
			if (tableInfo.getName().equals(name)) {
				createTable(tableInfo);
				return;
			}
		}
		throw new IllegalArgumentException("Table with name " + name + " hasn't been registered");
	}

	/**
	 * Execute the create statement for the registered table (the "IF EXISTS"
	 * keyword is used).
	 */
	public void createTable(Class<? extends Model> modelClass) {
		for (TableInfo tableInfo : registeredTables) {
			if (tableInfo.getOriginClass().equals(modelClass)) {
				createTable(tableInfo);
				return;
			}
		}
		throw new IllegalArgumentException("Table for class " + modelClass.getName() + " hasn't been registered");
	}

	/**
	 * Deletes the table and then creates it again.
	 */
	public void recreateTable(String name) {
		deleteTable(name);
		createTable(name);
	}

	/**
	 * Deletes the table and then creates it again.
	 */
	public void recreateTable(Class<? extends Model> modelClass) {
		for (TableInfo tableInfo : registeredTables) {
			if (tableInfo.getOriginClass().equals(modelClass)) {
				deleteTable(tableInfo.getName());
				createTable(tableInfo);
				return;
			}
		}
		throw new IllegalArgumentException("Table for class " + modelClass.getName() + " hasn't been registered");
	}

	/**
	 * Deletes the table.
	 */
	public void deleteTable(String name) {
		String sql = "DROP TABLE IF EXISTS " + name;
		DatabaseLog.d(sql);
		try {
			database.execSQL(sql);
		} catch (Exception e) {
			DatabaseLog.logException(e);
			DatabaseLog.e("can't drop table " + name);
		}
	}

	/**
	 * Deletes all tables in this database.
	 */
	public void deleteAllTables() {
		Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table'", null);
		try {
			int nameColumnId = cursor.getColumnIndex("name");
			while (cursor.moveToNext()) {
				String name = cursor.getString(nameColumnId);
				boolean tableIsService = "android_metadata".equals(name) || name.startsWith("sqlite_");
				if (!tableIsService) {
					deleteTable(name);
				}
			}
		} finally {
			cursor.close();
		}

	}

	/**
	 * Deletes all tables in the database and then creates all registered
	 * tables.
	 */
	public void recreateAllTables() {
		deleteAllTables();
		createAllRegisteredTables();
	}

	/**
	 * Creates all registered tables.
	 */

	public void createAllRegisteredTables() {
		for (TableInfo tableInfo : registeredTables) {
			createTable(tableInfo);
		}
	}

	private void createTable(TableInfo tableInfo) {
		String createQuery = tableInfo.getCreateQuery();
		DatabaseLog.d(createQuery);
		database.execSQL(createQuery);
	}

}
