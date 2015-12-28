package handy.storage;

import handy.storage.log.DatabaseLog;
import handy.storage.log.PerformanceTimer;
import handy.storage.update.OnDatabaseUpdateAction;
import handy.storage.update.OnDatabaseUpdatePolicy;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper for database engine.
 */
class DbOpenHelper extends SQLiteOpenHelper {

	private final OnDatabaseUpdatePolicy onUpdatePolicy;
	private final DatabaseCore schema;

	DbOpenHelper(Context context, DatabaseCore schema) {
		super(context, schema.getDatabaseInfo().getDatabaseName(), null, schema.getDatabaseInfo().getDatabaseVersion());
		onUpdatePolicy = schema.getDatabaseInfo().getOnDatabaseUpdatePolicy();
		this.schema = schema;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		PerformanceTimer.startInterval("creating the database");
		createSchemaEditor(db).createAllRegisteredTables();
		DatabaseLog.i(String.format("database \"%s\" created", schema.getDatabaseInfo().getDatabaseName()));
		PerformanceTimer.endInterval();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		PerformanceTimer.startInterval("updating the database");
		DatabaseLog.i(String.format("updating database \"%s\" from version %d to %d",
			schema.getDatabaseInfo().getDatabaseName(), oldVersion, newVersion));
		DatabaseSchemaEditor schemaEditor = createSchemaEditor(db);
		List<OnDatabaseUpdateAction> actions = onUpdatePolicy.getOnUpdateActions(oldVersion, newVersion);
		for (OnDatabaseUpdateAction action : actions) {
			action.execute(schemaEditor, db);
		}
		PerformanceTimer.endInterval();
	}
	
	private DatabaseSchemaEditor createSchemaEditor(SQLiteDatabase db) {
		return new DatabaseSchemaEditor(db, schema.getTables());
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

}
