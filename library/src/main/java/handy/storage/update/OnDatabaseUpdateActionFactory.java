package handy.storage.update;

import handy.storage.DatabaseSchemaEditor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Creates standard {@link OnDatabaseUpdateAction} realizations.
 */
public final class OnDatabaseUpdateActionFactory {

	private OnDatabaseUpdateActionFactory() {

	}

	/**
	 * Creates a {@link OnDatabaseUpdateAction} realization which does nothing.
	 */
	public static OnDatabaseUpdateAction emptyAction() {
		return new OnDatabaseUpdateAction() {

			@Override
			public void execute(DatabaseSchemaEditor schemaEditor, SQLiteDatabase db) {
				// do nothing
			}
		};
	}

	/**
	 * Creates a {@link OnDatabaseUpdateAction} realization which deletes all
	 * tables and than creates all registered tables. All records in the
	 * database will be cleared.
	 */
	public static OnDatabaseUpdateAction recreateTablesAction() {
		return new OnDatabaseUpdateAction() {

			@Override
			public void execute(DatabaseSchemaEditor schemaEditor, SQLiteDatabase db) {
				schemaEditor.recreateAllTables();
			}
		};
	}
	
	/**
	 * Creates a {@link OnDatabaseUpdateAction} realization which creates all
	 * tables that are registered but don't exist at the moment of execution.
	 * This realization doesn't delete anything from the database.
	 */
	public static OnDatabaseUpdateAction createNewTablesAction() {
		return new OnDatabaseUpdateAction() {

			@Override
			public void execute(DatabaseSchemaEditor schemaEditor, SQLiteDatabase db) {
				schemaEditor.createAllRegisteredTables();
			}
		};
	}

	/**
	 * Creates a {@link OnDatabaseUpdateAction} realization which sequentially
	 * executes raw SQL commands.
	 * 
	 * @param queries
	 *            ordered list of raw SQL commands
	 */
	public static OnDatabaseUpdateAction sqlQueriesAction(final String... queries) {
		return new OnDatabaseUpdateAction() {

			@Override
			public void execute(DatabaseSchemaEditor schemaEditor, SQLiteDatabase db) {
				if (queries != null) {
					for (String sql : queries) {
						db.execSQL(sql);
					}
				}
			}
		};
	}


}
