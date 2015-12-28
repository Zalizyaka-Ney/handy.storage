package handy.storage.update;

import handy.storage.DatabaseSchemaEditor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Action on database update.
 */
public interface OnDatabaseUpdateAction {
	
	/**
	 * Callback called on database update.
	 * 
	 * @param schemaEditor
	 *            helper for editing database schema
	 * @param db
	 *            raw database object
	 */
	void execute(DatabaseSchemaEditor schemaEditor, SQLiteDatabase db);

}
