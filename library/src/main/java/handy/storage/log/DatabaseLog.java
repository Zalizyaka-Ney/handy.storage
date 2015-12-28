package handy.storage.log;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;

/** Logs database operations. */
public final class DatabaseLog {
	
	private DatabaseLog() {
	}

	private static Logger logger = new AndroidLogger(Log.WARN);
	
	/**
	 * Sets a {@link Logger} object that will be used for printing logs. By default, the framework prints only warnings and errors in the
	 * Android log. Pass <code>null</code> to disable all log messages.
	 */
	public static void setLogger(Logger logger) {
		DatabaseLog.logger = logger;
	}
	
	/**
	 * Enables/disables measuring time spent for base framework's operations.
	 *
	 * @param enabled is enabled
	 */
	public static void setTimingEnabled(boolean enabled) {
		PerformanceTimer.setEnabled(enabled);
	}

	/**
	 * Logs a debug message
	 */
	public static void d(String message) {
		if (logger != null) {
			logger.d(message);
		}
	}
	
	/**
	 * Logs an informational message.
	 */
	public static void i(String message) {
		if (logger != null) {
			logger.i(message);
		}
	}
	
	/**
	 * Logs a warning.
	 */
	public static void w(String message) {
		if (logger != null) {
			logger.w(message);
		}
	}
	
	/**
	 * Logs an error.
	 */
	public static void e(String message) {
		if (logger != null) {
			logger.e(message);
		}
	}
	
	/**
	 * Logs an exception.
	 */
	public static void logException(Throwable e) {
		if (logger != null) {
			logger.logException(e);
		}
	}
	
	/**
	 * Prints the content of this cursor using {@link #i(String)} method.
	 *
	 * @param cursor cursor
	 * @param message additional message
	 */
	public static void dumpCursor(Cursor cursor, String message) {
		i(">>>>> Dumping a cursor, " + message);
		if (cursor != null) {
			int startPos = cursor.getPosition();

			cursor.moveToPosition(-1);
			while (cursor.moveToNext()) {
				dumpCurrentRow(cursor);
			}
			cursor.moveToPosition(startPos);
		}
		i("<<<<<");
	}

	/**
	 * Prints the the current row of this cursor using {@link #i(String)}
	 * method.
	 *
	 * @param cursor cursor
	 */
	public static void dumpCurrentRow(Cursor cursor) {
		String[] cols = cursor.getColumnNames();
		i("" + cursor.getPosition() + " {");
		int length = cols.length;
		for (int i = 0; i < length; i++) {
			String value;
			try {
				value = cursor.getString(i);
			} catch (SQLiteException e) {
				// assume that if the getString threw this exception then the column is not
				// representable by a string, e.g. it is a BLOB.
				value = "<unprintable>";
			}
			i("   " + cols[i] + '=' + value);
		}
		i("}");
	}

}
