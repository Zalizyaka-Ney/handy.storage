package handy.storage.base;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import handy.storage.exception.ConstraintFailedException;
import handy.storage.exception.OperationException;
import handy.storage.exception.UnableToOpenDatabaseException;
import handy.storage.log.DatabaseLog;

/**
 * Wraps SQLite operations.
 */
class SQLiteDatabaseAdapter {

	private final LinkedList<Boolean> transactionStatuses = new LinkedList<>();
	private final SQLiteOpenHelper openHelper;

	private boolean transactionCorrupted = false;
	private boolean nestedTransactionFailed = false;

	SQLiteDatabaseAdapter(SQLiteOpenHelper openHelper) {
		this.openHelper = openHelper;
	}

	private SQLiteDatabase getReadableDatabase() throws UnableToOpenDatabaseException {
		try {
			return openHelper.getReadableDatabase();
		} catch (SQLiteException e) {
			throw new UnableToOpenDatabaseException(e);
		}
	}

	private SQLiteDatabase getWritableDatabase() throws UnableToOpenDatabaseException {
		try {
			return openHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			throw new UnableToOpenDatabaseException(e);
		}
	}

	private OperationException wrapException(Exception e) {
		if (e instanceof SQLiteConstraintException) {
			return new ConstraintFailedException(e);
		} else {
			return new OperationException(e);
		}
	}

	void executeSql(String sql, String... selectionArgs) throws OperationException {
		SQLiteDatabase database = getWritableDatabase();
		beginTransaction(database);
		try {
			database.execSQL(sql, selectionArgs);
			setTransactionSuccessful(database);
		} catch (Exception e) {
			throw wrapException(e);
		} finally {
			endTransaction(database);
		}
	}

	Cursor rawQuery(String sql, String... selectionArgs) throws OperationException {
		SQLiteDatabase database = getWritableDatabase();
		Cursor result;
		beginTransaction(database);
		try {
			result = database.rawQuery(sql, selectionArgs);
			setTransactionSuccessful(database);
		} catch (Exception e) {
			throw wrapException(e);
		} finally {
			endTransaction(database);
		}
		return result;
	}

	int count(String table, String whereClause, String... whereArgs) throws OperationException {
		DatabaseLog.i("counting rows in the table \"" + table + "\" where \"" + whereClause + "\", "
			+ "arguments are " + Arrays.toString(whereArgs));
		SQLiteDatabase database = getReadableDatabase();
		int count = 0;
		beginTransaction();
		try {
			String selection = !TextUtils.isEmpty(whereClause) ? " where " + whereClause : "";
			String query = "SELECT COUNT(*) FROM " + table + selection;
			count = (int) DatabaseUtils.longForQuery(database, query, whereArgs);
			setTransactionSuccessful(database);
		} catch (Exception e) {
			throw wrapException(e);
		} finally {
			endTransaction(database);
		}
		DatabaseLog.i("result is " + count);
		return count;
	}

	int remove(String table, String whereClause, String... whereArgs) throws OperationException {
		DatabaseLog.i("removing from the table \"" + table + "\" where \"" + whereClause + "\", "
			+ "arguments are " + Arrays.toString(whereArgs));
		SQLiteDatabase database = getWritableDatabase();
		int deleted;
		beginTransaction(database);
		try {
			deleted = database.delete(table, whereClause, whereArgs);
			setTransactionSuccessful(database);
		} catch (Exception e) {
			throw wrapException(e);
		} finally {
			endTransaction(database);
		}
		DatabaseLog.i("removed " + deleted + " rows");
		return deleted;
	}

	int update(String table, ContentValues values, OnConflictStrategy onConflictStrategy, String whereClause, String... whereArgs) throws OperationException {
		DatabaseLog.i("updating in the table \"" + table + "\" where \"" + whereClause + "\", "
			+ "arguments are " + Arrays.toString(whereArgs));
		SQLiteDatabase database = getWritableDatabase();
		int updated;
		beginTransaction(database);
		try {
			updated = database.updateWithOnConflict(table, values, whereClause, whereArgs, onConflictStrategy.getAlgorithmId());
			setTransactionSuccessful(database);
		} catch (Exception e) {
			throw wrapException(e);
		} finally {
			endTransaction(database);
		}
		DatabaseLog.i("updated " + updated + " rows");
		return updated;
	}
	
	List<Long> insert(String table, List<? extends ContentValues> valuesCollection, OnConflictStrategy onConflictStrategy) throws OperationException {
		SQLiteDatabase database = getWritableDatabase();
		try {
			List<Long> ids = new ArrayList<>(valuesCollection.size());
			beginTransaction(database);
			try {
				for (ContentValues cv : valuesCollection) {
					long id = database.insertWithOnConflict(table, null, cv, onConflictStrategy.getAlgorithmId());
					if (id == -1 && onConflictStrategy != OnConflictStrategy.IGNORE) {
						throw new OperationException("can't insert values into the database");
					}
					ids.add(id);
				}
				setTransactionSuccessful(database);
			} finally {
				endTransaction(database);
			}
			return ids;
		} catch (Exception e) {
			throw wrapException(e);
		} 

	}

	private void endTransaction(SQLiteDatabase database) throws OperationException {
		Boolean status = transactionStatuses.removeFirst();
		if (!status) {
			nestedTransactionFailed = true;
		}
		if (!transactionCorrupted) {
			try {
				database.endTransaction();
			} catch (Exception e) {
				// this is a workaround against https://code.google.com/p/android/issues/detail?id=74751 bug
				transactionCorrupted = true;
				closeDatabase(database);
				throw wrapException(e);
			}
		}
	}

	long insert(String table, ContentValues values, OnConflictStrategy onConflictStrategy) throws OperationException {
		long id = -1;
		SQLiteDatabase database = getWritableDatabase();
		beginTransaction(database);
		try {
			id = database.insertWithOnConflict(table, null, values, onConflictStrategy.getAlgorithmId());
			setTransactionSuccessful(database);
		} catch (Exception e) {
			throw wrapException(e);
		} finally {
			endTransaction(database);
		}

		if (id == -1 && onConflictStrategy != OnConflictStrategy.IGNORE) {
			throw new OperationException("can't insert values into the database");
		}
		return id;
	}

	Cursor performQuery(QueryParams queryParams) throws OperationException {
		DatabaseLog.i("performing a query");
		DatabaseLog.i("query: distinct = " + queryParams.isDistinct());
		DatabaseLog.i("query: table = " + queryParams.getTableName());
		DatabaseLog.i("query: columns = " + Arrays.toString(queryParams.getColumns()));
		DatabaseLog.i("query: selection = " + queryParams.getSelection());
		DatabaseLog.i("query: selection args = " + Arrays.toString(queryParams.getSelectionArgs()));
		DatabaseLog.i("query: groupBy = " + queryParams.getGroupBy());
		DatabaseLog.i("query: having = " + queryParams.getHaving());
		DatabaseLog.i("query: orderBy = " + queryParams.getOrderBy());
		DatabaseLog.i("query: limit = " + queryParams.getLimit());
		SQLiteDatabase database = getReadableDatabase();
		Cursor result;
		beginTransaction(database);
		try {
			result = database.query(
					queryParams.isDistinct(),
					queryParams.getTableName(),
					queryParams.getColumns(),
					queryParams.getSelection(),
					queryParams.getSelectionArgs(),
					queryParams.getGroupBy(),
					queryParams.getHaving(),
					queryParams.getOrderBy(),
					queryParams.getLimit());

			setTransactionSuccessful(database);
		} catch (Exception e) {
			throw wrapException(e);
		} finally {
			endTransaction(database);
		}
		return result;
	}

	void prepare() throws UnableToOpenDatabaseException {
		getWritableDatabase();
	}

	void beginTransaction() throws OperationException {
		try {
			beginTransaction(getWritableDatabase());
		} catch (Exception e) {
			throw wrapException(e);
		}
	}

	private void beginTransaction(SQLiteDatabase database) {
		transactionStatuses.add(0, Boolean.FALSE);
		database.beginTransaction();
	}

	void endTransaction() throws OperationException {
		endTransaction(getWritableDatabase());
	}

	void setTransactionSuccessful() throws OperationException {
		if (transactionStatuses.size() == 1 && nestedTransactionFailed) {
			throw new OperationException("Can't set the transaction successful: one of the nested transactions has failed.");
		}
		try {
			setTransactionSuccessful(getWritableDatabase());
		} catch (Exception e) {
			throw wrapException(e);
		}
	}

	private void setTransactionSuccessful(SQLiteDatabase database) {
		transactionStatuses.set(0, Boolean.TRUE);
		database.setTransactionSuccessful();
	}

	void close() throws OperationException {
		closeDatabase(getWritableDatabase());
	}

	void checkTransactionValidity() throws OperationException {
		if (transactionCorrupted) {
			throw new OperationException("Transaction is corrupted!");
		}
	}

	private void closeDatabase(SQLiteDatabase database) throws OperationException {
		try {
			database.close();
		} catch (Exception e) {
			throw wrapException(e);
		}
	}

}
