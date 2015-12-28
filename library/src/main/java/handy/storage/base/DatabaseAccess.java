package handy.storage.base;

import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages access to the database.
 */
class DatabaseAccess {

	private final SQLiteOpenHelper openHelper;
	private final Lock lock = new ReentrantLock();

	private SQLiteDatabaseAdapter currentDataAccess;
	private int count = 0;

	DatabaseAccess(SQLiteOpenHelper openHelper) {
		this.openHelper = openHelper;
	}

	SQLiteDatabaseAdapter acquireDataAccess() {
		lock.lock();
		count++;
		if (currentDataAccess == null) {
			currentDataAccess = new SQLiteDatabaseAdapter(openHelper);
		}
		return currentDataAccess;
	}

	void releaseDataAccess() {
		if (count > 0) {
			count--;
			if (count == 0) {
				currentDataAccess = null;
			}
			lock.unlock();
		}
	}

	SQLiteDatabaseAdapter continueLastDataAccess() {
		return currentDataAccess;
	}

}
