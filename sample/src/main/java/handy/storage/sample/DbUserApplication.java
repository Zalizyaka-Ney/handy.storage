package handy.storage.sample;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import handy.storage.Database;
import handy.storage.HandyStorage;
import handy.storage.log.AndroidLogger;
import handy.storage.log.DatabaseLog;
import handy.storage.sample.model.Building;
import handy.storage.sample.model.Department;
import handy.storage.sample.model.Employee;
import handy.storage.sample.model.Task;
import handy.storage.sample.model.TaskListEntry;

/**
 * Test application.
 */
public class DbUserApplication extends Application {

	private Database database;

	@Override
	public void onCreate() {
		super.onCreate();
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
			.detectAll()
			.penaltyLog()
			.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			.detectLeakedSqlLiteObjects()
			.detectLeakedClosableObjects()
			.penaltyLog()
			.build());
		long start = System.currentTimeMillis();
		DatabaseLog.setLogger(new AndroidLogger(Log.WARN));
		DatabaseLog.setTimingEnabled(true);
		database = createDatabase();
		startService(new Intent(this, TestService.class));
		Log.i("sample", "app created in " + (System.currentTimeMillis() - start));
	}

	public Database getDatabase() {
		return database;
	}

	private Database createDatabase() {
		HandyStorage handyStorage = new HandyStorage.Builder()
			.setUseColumnsFromSuperclasses(true)
			.setObjectCreator(Department.class, new Department.Creator())
			.build();
		return handyStorage.newDatabase(this, "sample_db", getDatabaseVersion())
			// settings
			.setCheckTablesChanges(BuildConfig.DEBUG)
			.setIdOnInsertByDefault(true)
				// tables
			.addTable(Building.class)
			.addTable(Department.class)
			.addTable(Employee.class)
			.addTable(Task.class)
			.addTable(TaskListEntry.class)

			.build();
	}

	private int getDatabaseVersion() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final String version_key = "dbver";
		int version = prefs.getInt(version_key, 1);
		version++;
		prefs.edit().putInt(version_key, version).apply();
		return version;
	}

}
