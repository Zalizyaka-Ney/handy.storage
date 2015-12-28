package handy.storage.base;

import android.database.sqlite.SQLiteDatabase;

import handy.storage.WritableTable;

/**
 * Behaviour on conflict during insertion/update operation. See <a
 * href="http://sqlite.org/lang_conflict.html"
 * >http://sqlite.org/lang_conflict.html</a> for details.
 */
public enum OnConflictStrategy {

	/**
	 * An equivalent of {@link SQLiteDatabase#CONFLICT_NONE}. The behaviour is
	 * the same as for {@link OnConflictStrategy#ABORT}
	 */
	DEFAULT(SQLiteDatabase.CONFLICT_NONE),

	/**
	 * An equivalent of {@link SQLiteDatabase#CONFLICT_ROLLBACK}. When an
	 * applicable constraint violation occurs, the ROLLBACK resolution algorithm
	 * aborts the current SQL statement with an SQLITE_CONSTRAINT error and
	 * rolls back the current transaction.
	 */
	ROLLBACK(SQLiteDatabase.CONFLICT_ROLLBACK),

	/**
	 * An equivalent of {@link SQLiteDatabase#CONFLICT_ABORT}. When an
	 * applicable constraint violation occurs, the ABORT resolution algorithm
	 * aborts the current SQL statement with an SQLITE_CONSTRAINT error and
	 * backs out any changes made by the current SQL statement; but changes
	 * caused by prior SQL statements within the same transaction are preserved
	 * and the transaction remains active. This is the default behavior and the
	 * behavior specified by the SQL standard.
	 */
	ABORT(SQLiteDatabase.CONFLICT_ABORT),

	/**
	 * An equivalent of {@link SQLiteDatabase#CONFLICT_FAIL}. When an applicable
	 * constraint violation occurs, the FAIL resolution algorithm aborts the
	 * current SQL statement with an SQLITE_CONSTRAINT error. But the FAIL
	 * resolution does not back out prior changes of the SQL statement that
	 * failed nor does it end the transaction. For example, if an UPDATE
	 * statement encountered a constraint violation on the 100th row that it
	 * attempts to update, then the first 99 row changes are preserved but
	 * changes to rows 100 and beyond never occur.
	 */
	FAIL(SQLiteDatabase.CONFLICT_FAIL),

	/**
	 * <p>
	 * An equivalent of {@link SQLiteDatabase#CONFLICT_IGNORE}. When an
	 * applicable constraint violation occurs, the IGNORE resolution algorithm
	 * skips the one row that contains the constraint violation and continues
	 * processing subsequent rows of the SQL statement as if nothing went wrong.
	 * Other rows before and after the row that contained the constraint
	 * violation are inserted or updated normally. No error is returned when the
	 * IGNORE conflict resolution algorithm is used.
	 * </p>
	 * 
	 * <p>
	 * If you use this algorithm, {@link WritableTable}'s <code>insert</code>
	 * methods can return <code>-1</code> as rowid.
	 */
	IGNORE(SQLiteDatabase.CONFLICT_IGNORE),

	/**
	 * An equivalent of {@link SQLiteDatabase#CONFLICT_REPLACE}. When a UNIQUE
	 * or PRIMARY KEY constraint violation occurs, the REPLACE algorithm deletes
	 * pre-existing rows that are causing the constraint violation prior to
	 * inserting or updating the current row and the command continues executing
	 * normally.
	 */
	REPLACE(SQLiteDatabase.CONFLICT_REPLACE);

	private final int algorithm;

	OnConflictStrategy(int algorithm) {
		this.algorithm = algorithm;
	}

	int getAlgorithmId() {
		return algorithm;
	}

}
