package handy.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import handy.storage.base.DatabaseAdapter;
import handy.storage.base.QueryParams;
import handy.storage.exception.InvalidDatabaseSchemaException;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;

/**
 * Manages databases' meta info.
 */
final class MetaInfoDatabase {

	static final String META_INFO_DATABASE_NAME = "handy.storage.inner";
	private static final int META_INFO_DATABASE_VERSION = 1;

	private static final String TABLE_VERSIONS = "versions";
	private static final String TABLE_DECLARATIONS = "table_declarations";
	private static final String TABLE_ENUMS = "enums";
	private static final String[] ALL_TABLES = {TABLE_VERSIONS, TABLE_DECLARATIONS, TABLE_ENUMS};

	private static final String DATABASE = "database_name";
	private static final String VERSION = "version";
	private static final String TABLE = "table_name";
	private static final String DECLARATION = "declaration";
	private static final String COLUMN = "column";
	private static final String ENUM_VALUES = "enum_values";

	private static SQLiteOpenHelper databaseOpenHelper;

	static void onCheckChanges(Context context, DatabaseCore databaseCore) {
		DatabaseFingerprint databaseFingerprint = getDatabaseFingerprint(databaseCore);
		String databaseName = databaseCore.getDatabaseInfo().getDatabaseName();
		int databaseVersion = databaseCore.getDatabaseInfo().getDatabaseVersion();
		executeTask(context, databaseAdapter -> {
			Integer previousVersion = getPreviousDatabaseVersion(databaseAdapter, databaseName);
			if (previousVersion != null && previousVersion == databaseVersion) {
				checkChanges(databaseAdapter, databaseName, databaseFingerprint);
			}
			saveDatabaseInfo(databaseAdapter, databaseName, databaseVersion, databaseFingerprint);
		});
	}

	private static void checkChanges(DatabaseAdapter databaseAdapter, String databaseName, DatabaseFingerprint databaseFingerprint) throws OperationException {
		DatabaseFingerprint previousDatabaseFingerprint = getPreviousDatabaseFingerprint(databaseAdapter, databaseName);
		if (!compareTableDeclarations(previousDatabaseFingerprint.tableDeclarations, databaseFingerprint.tableDeclarations)
			|
			!compareEnumValues(previousDatabaseFingerprint.enumValues, databaseFingerprint.enumValues)) {

			throw new InvalidDatabaseSchemaException("database tables has been changed but version is not incremented");
		}
	}

	private static void saveDatabaseInfo(DatabaseAdapter databaseAdapter, String databaseName, int databaseVersion, DatabaseFingerprint databaseFingerprint) throws OperationException {
		deleteDatabaseInfo(databaseAdapter, databaseName);
		saveDatabaseVersion(databaseAdapter, databaseName, databaseVersion);
		for (String table : databaseFingerprint.tableDeclarations.keySet()) {
			saveTableDeclaration(databaseAdapter, databaseName, table, databaseFingerprint.tableDeclarations.get(table));
		}
		for (String column : databaseFingerprint.enumValues.keySet()) {
			saveEnumValues(databaseAdapter, databaseName, column, databaseFingerprint.enumValues.get(column));
		}
	}

	private static void saveDatabaseVersion(DatabaseAdapter databaseAdapter, String databaseName, int databaseVersion) throws OperationException {
		ContentValues cv = new ContentValues();
		cv.put(DATABASE, databaseName);
		cv.put(VERSION, databaseVersion);
		databaseAdapter.insertOrReplace(TABLE_VERSIONS, cv);
	}

	private static void saveTableDeclaration(DatabaseAdapter databaseAdapter, String databaseName, String table, String declaration) throws OperationException {
		ContentValues cv = new ContentValues();
		cv.put(DATABASE, databaseName);
		cv.put(TABLE, table);
		cv.put(DECLARATION, declaration);
		databaseAdapter.insertOrReplace(TABLE_DECLARATIONS, cv);
	}

	private static void saveEnumValues(DatabaseAdapter databaseAdapter, String databaseName, String column, String enumValues) throws OperationException {
		ContentValues cv = new ContentValues();
		cv.put(DATABASE, databaseName);
		cv.put(COLUMN, column);
		cv.put(ENUM_VALUES, enumValues);
		databaseAdapter.insertOrReplace(TABLE_ENUMS, cv);
	}

	private static void deleteDatabaseInfo(DatabaseAdapter databaseAdapter, String databaseName) throws OperationException {
		for (String table : ALL_TABLES) {
			databaseAdapter.remove(table, DATABASE + " = ?", databaseName);
		}
	}

	private static Integer getPreviousDatabaseVersion(DatabaseAdapter databaseAdapter, String databaseName) throws OperationException {
		QueryParams queryParams = new QueryParams().from(TABLE_VERSIONS).columns(VERSION, DATABASE).where(DATABASE + " = ?", databaseName).limit(1);
		Cursor cursor = databaseAdapter.performQuery(queryParams);
		try {
			if (cursor.moveToFirst()) {
				return cursor.getInt(0);
			}
		} finally {
			cursor.close();
		}
		return null;
	}

	private static DatabaseFingerprint getPreviousDatabaseFingerprint(DatabaseAdapter databaseAdapter, String databaseName) throws OperationException {
		return new DatabaseFingerprint(
			readMapFromDatabase(databaseAdapter, databaseName, TABLE_DECLARATIONS, TABLE, DECLARATION),
			readMapFromDatabase(databaseAdapter, databaseName, TABLE_ENUMS, COLUMN, ENUM_VALUES)
		);
	}

	private static Map<String, String> readMapFromDatabase(DatabaseAdapter databaseAdapter, String databaseName, String table, String columnKey, String columnValue) throws OperationException {
		QueryParams queryParams = new QueryParams().from(table).columns(columnKey, columnValue, DATABASE).where(DATABASE + " = ?", databaseName);
		Cursor cursor = databaseAdapter.performQuery(queryParams);
		Map<String, String> result = new HashMap<>();
		try {
			while (cursor.moveToNext()) {
				result.put(cursor.getString(0), cursor.getString(1));
			}
		} finally {
			cursor.close();
		}
		return result;
	}

	private static boolean compareTableDeclarations(Map<String, String> previous, Map<String, String> current) {
		boolean result = true;
		for (String table : previous.keySet()) {
			String currentDeclaration = current.get(table);
			if (currentDeclaration == null) {
				result = false;
				DatabaseLog.e(String.format("table \"%s\" is not registered", table));
			} else if (!currentDeclaration.equals(previous.get(table))) {
				result = false;
				DatabaseLog.e("declaration of table \"" + table + "\" doesn't match the previous declaration:");
				DatabaseLog.e("was: " + previous.get(table));
				DatabaseLog.e("now: " + currentDeclaration);
			}
		}
		if (!result || current.size() > previous.size()) {
			for (String table : current.keySet()) {
				if (!previous.containsKey(table)) {
					result = false;
					DatabaseLog.e("table \"" + table + "\" wasn't declared earlier");
				}
			}
		}
		return result;
	}

	private static boolean compareEnumValues(Map<String, String> previous, Map<String, String> current) {
		boolean result = true;
		for (String column : previous.keySet()) {
			String currentValues = current.get(column);
			if (currentValues == null) {
				result = false;
				DatabaseLog.e(column + " used to be an enum, but now it's not an enum");
			} else if (!currentValues.equals(previous.get(column))) {
				result = false;
				DatabaseLog.e("values of \"" + column + "\" have been changed");
				DatabaseLog.e("were: " + previous.get(column));
				DatabaseLog.e("now: " + currentValues);
			}
		}
		if (!result || current.size() > previous.size()) {
			for (String column : current.keySet()) {
				if (!previous.containsKey(column)) {
					result = false;
					DatabaseLog.e("column \"" + column + "\" didn't use to be an enum, but it is now");
				}
			}
		}
		return result;
	}

	private static DatabaseAdapter getDataAdapter(Context context) {
		if (databaseOpenHelper == null) {
			databaseOpenHelper = new SQLiteOpenHelper(context, META_INFO_DATABASE_NAME, null, META_INFO_DATABASE_VERSION) {
				@Override
				public void onCreate(SQLiteDatabase database) {
					database.execSQL("CREATE TABLE " + TABLE_VERSIONS
						+ " (" + VERSION + " INTEGER NOT NULL, " + DATABASE + " TEXT PRIMARY KEY NOT NULL)");
					database.execSQL("CREATE TABLE " + TABLE_DECLARATIONS
						+ " (" + DATABASE + " TEXT NOT NULL, " + TABLE + " TEXT NOT NULL, " + DECLARATION + " TEXT NOT NULL, "
						+ "PRIMARY KEY (" + DATABASE + ", " + TABLE + "))");
					database.execSQL("CREATE TABLE " + TABLE_ENUMS
						+ " (" + DATABASE + " TEXT NOT NULL, " + COLUMN + " TEXT NOT NULL, " + ENUM_VALUES + " TEXT NOT NULL, "
						+ "PRIMARY KEY (" + DATABASE + ", " + COLUMN + "))");
				}

				@Override
				public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
				}
			};
		}
		return new DatabaseAdapter(databaseOpenHelper);
	}

	static void deleteDatabaseInfo(Context context, String databaseName) {
		executeTask(context, databaseAdapter -> deleteDatabaseInfo(databaseAdapter, databaseName));
	}

	private static synchronized void executeTask(Context context, Task task) {
		DatabaseAdapter databaseAdapter = getDataAdapter(context);
		try {
			DatabaseAdapter.TransactionControl transactionControl = databaseAdapter.startTransaction();
			try {
				task.run(databaseAdapter);
				transactionControl.setSuccessful();
			} finally {
				transactionControl.end();
			}
		} catch (OperationException e) {
			DatabaseLog.logException(e);
		} finally {
			databaseAdapter.close();
		}
	}

	private static DatabaseFingerprint getDatabaseFingerprint(DatabaseCore databaseCore) {
		Map<String, String> tableDeclarations = new HashMap<>();
		Map<String, String> enumValues = new HashMap<>();
		for (TableInfo tableInfo : databaseCore.getTables()) {
			tableDeclarations.put(tableInfo.getName(), tableInfo.getCreateQuery());
			for (ColumnInfo column : tableInfo.getColumns()) {
				Class<?> fieldType = column.getFieldType();
				if (fieldType != null) {
					if (fieldType.isEnum()) {
						enumValues.put(column.getFullName(), TextUtils.join(",", fieldType.getEnumConstants()));
					}
				}
			}
		}
		return new DatabaseFingerprint(tableDeclarations, enumValues);
	}

	/**
	 * Some operation with meta info.
	 */
	private interface Task {
		void run(DatabaseAdapter databaseAdapter) throws OperationException;
	}

	/**
	 * Key database schema values.
	 */
	private static final class DatabaseFingerprint {
		private Map<String, String> tableDeclarations;
		private Map<String, String> enumValues;

		private DatabaseFingerprint(Map<String, String> tableDeclarations, Map<String, String> enumValues) {
			this.tableDeclarations = tableDeclarations;
			this.enumValues = enumValues;
		}
	}

	private MetaInfoDatabase() {
	}
}
