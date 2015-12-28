package handy.storage.base;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Looper;

import java.util.List;

import handy.storage.exception.IllegalUsageException;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;


/**
 * Provides access to the raw database.
 */
public final class DatabaseAdapter {

	private final DatabaseAccess databaseAccess;

	private boolean closed = false;

	public DatabaseAdapter(SQLiteOpenHelper openHelper) {
		databaseAccess = new DatabaseAccess(openHelper);
	}

	/**
	 * Call this method to ensure that the database is ready to use (created and
	 * updated).
	 */
	public void prepare() {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			sqlite.prepare();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Starts a transaction and return an object to control its lifecycle (set
	 * successful and end it). You must always call
	 * {@link TransactionControl#end()} in <code>finally</code> block;
	 *
	 * @throws OperationException if any error happened
	 */
	public TransactionControl startTransaction() throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			sqlite.beginTransaction();
			return new TransactionControl();
		} catch (OperationException e) {
			releaseDataAccess();
			throw e;
		}
		// access will be released in endTransaction()
	}

	private void endTransaction() throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = continueDataAccess(false);
			sqlite.endTransaction();
		} finally {
			releaseDataAccess();
		}
	}

	private void setTransactionSuccessful() throws OperationException {
		SQLiteDatabaseAdapter sqlite = continueDataAccess(true);
		sqlite.setTransactionSuccessful();
	}

	/**
	 * Performs query to database.
	 *
	 * @param queryParams encapsulates query parameters
	 * @return {@link Cursor} instance, result of this query. Returns
	 * <code>null</code> if any error.
	 * @throws OperationException if any error happened
	 */
	public Cursor performQuery(QueryParams queryParams) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			return sqlite.performQuery(queryParams);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Convenient method for inserting a row into the database.
	 *
	 * @param table  table's name
	 * @param values values
	 * @return id of inserted row
	 * @throws OperationException if any error happen
	 */
	public long insert(String table, ContentValues values) throws OperationException {
		return insert(table, values, OnConflictStrategy.DEFAULT);
	}

	/**
	 * Convenient method for inserting rows into the database in one
	 * transaction.
	 *
	 * @param table            table's name
	 * @param valuesCollection values
	 * @return id of inserted row
	 * @throws OperationException if any error happen
	 */
	public List<Long> insert(String table, List<? extends ContentValues> valuesCollection) throws OperationException {
		return insert(table, valuesCollection, OnConflictStrategy.DEFAULT);
	}

	/**
	 * Convenient method for inserting a row into the database. Replace values
	 * on conflicts.
	 *
	 * @param table  table's name
	 * @param values values
	 * @throws OperationException if any error happen
	 */
	public long insertOrReplace(String table, ContentValues values) throws OperationException {
		return insert(table, values, OnConflictStrategy.REPLACE);
	}

	/**
	 * Convenient method for inserting rows into the database in one
	 * transaction. Replace values on conflicts.
	 *
	 * @param table            table's name
	 * @param valuesCollection values
	 * @throws OperationException if any error happen
	 */
	public List<Long> insertOrReplace(String table, List<? extends ContentValues> valuesCollection) throws OperationException {
		return insert(table, valuesCollection, OnConflictStrategy.REPLACE);
	}

	/**
	 * Convenient method for inserting a row into the database.
	 *
	 * @throws OperationException if any error happen
	 **/
	public long insert(String table, ContentValues values, OnConflictStrategy onConflictStrategy) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			return sqlite.insert(table, values, onConflictStrategy);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Convenient method for inserting rows into the database in one
	 * transaction.
	 *
	 * @throws OperationException if any error happen
	 **/
	public List<Long> insert(String table, List<? extends ContentValues> valuesCollection, OnConflictStrategy onConflictStrategy) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			return sqlite.insert(table, valuesCollection, onConflictStrategy);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Executes update operation.
	 *
	 * @return number of changed rows
	 * @throws OperationException if any error happen
	 */
	public int update(String table, ContentValues values, String whereClause, String... whereArgs) throws OperationException {
		return update(table, values, OnConflictStrategy.DEFAULT, whereClause, whereArgs);
	}

	/**
	 * Convenient method for updating rows in the database.
	 *
	 * @throws OperationException if any error happen
	 */
	public int update(String table, ContentValues values, OnConflictStrategy onConflictStrategy, String whereClause, String... whereArgs) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			return sqlite.update(table, values, onConflictStrategy, whereClause, whereArgs);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Convenient method for deleting rows in the database.
	 *
	 * @throws OperationException if any error happen
	 */
	public int remove(String table, String whereClause, String... whereArgs) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			return sqlite.remove(table, whereClause, whereArgs);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Convenient method for counting rows in the database.
	 *
	 * @throws OperationException if any error happen
	 */
	public int count(String table, String whereClause, String... whereArgs) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			return sqlite.count(table, whereClause, whereArgs);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Runs the provided SQL and returns a Cursor over the result set.
	 *
	 * @throws OperationException if any error happen
	 */
	public Cursor rawQuery(String sql, String... selectionArgs) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			return sqlite.rawQuery(sql, selectionArgs);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Executes a query that is NOT a SELECT query and doesn't return any
	 * results.
	 *
	 * @param sql           SQL query
	 * @param selectionArgs selection arguments for the query
	 * @throws OperationException if any error happen
	 */
	public void executeSql(String sql, String... selectionArgs) throws OperationException {
		try {
			SQLiteDatabaseAdapter sqlite = acquireDataAccess();
			sqlite.executeSql(sql, selectionArgs);
		} finally {
			releaseDataAccess();
		}
	}

	/**
	 * Closes encapsulated databases.
	 */
	public void close() {
		if (!closed) {
			try {
				SQLiteDatabaseAdapter sqlite = acquireDataAccess();
				sqlite.close();
			} catch (OperationException e) {
				DatabaseLog.logException(e);
			} finally {
				closed = true;
				releaseDataAccess();
			}
		}
	}

	private void releaseDataAccess() {
		databaseAccess.releaseDataAccess();
	}

	private void checkCallPermit() {
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			throw new IllegalUsageException("you can't use DataAdapter in UI thread");
		}
		if (closed) {
			throw new IllegalUsageException("DataAdapter has been closed");
		}
	}

	private SQLiteDatabaseAdapter acquireDataAccess() throws OperationException {
		checkCallPermit();
		SQLiteDatabaseAdapter adapter = databaseAccess.acquireDataAccess();
		adapter.checkTransactionValidity();
		return adapter;
	}

	private SQLiteDatabaseAdapter continueDataAccess(boolean checkTransaction) throws OperationException {
		checkCallPermit();
		SQLiteDatabaseAdapter adapter = databaseAccess.continueLastDataAccess();
		if (adapter == null) {
			throw new IllegalStateException("No opened transaction.");
		}
		if (checkTransaction) {
			adapter.checkTransactionValidity();
		}
		return adapter;
	}

	/**
	 * Allows to complete the transaction.
	 */
	public class TransactionControl {

		/**
		 * Marks the transaction as successful.
		 *
		 * @throws OperationException if any error happen
		 */
		public void setSuccessful() throws OperationException {
			setTransactionSuccessful();
		}

		/**
		 * Ends the transaction.
		 *
		 * @throws OperationException if any error happen
		 */
		public void end() throws OperationException {
			endTransaction();
		}
	}

}
