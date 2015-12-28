package handy.storage;

import android.content.Context;
import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import handy.storage.DatabaseCore.TablesFactory;
import handy.storage.api.Model;
import handy.storage.api.Transaction;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.DatabaseAdapter.TransactionControl;
import handy.storage.exception.IllegalUsageException;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;
import handy.storage.log.PerformanceTimer;

/**
 * <p>
 * Base class for access to the database.
 * </p>
 */
public class Database {

	private static final Set<String> DATABASES = Collections.synchronizedSet(new HashSet<>());

	private final DatabaseCore core;
	private final DatabaseAdapter databaseAdapter;
	private final Context appContext;

	private boolean initialized = false;

	Database(Context context, DatabaseCore schema) {
		String databaseName = schema.getDatabaseInfo().getDatabaseName();
		if (DATABASES.contains(databaseName)) {
			throw new IllegalStateException(String.format("Database \"%s\" is already opened.", databaseName));
		}
		DATABASES.add(databaseName);
		this.appContext = context.getApplicationContext();
		this.core = schema;
		databaseAdapter = new DatabaseAdapter(new DbOpenHelper(appContext, schema));
	}

	private synchronized void initialize() {
		DatabaseLog.i("initializing the database");
		PerformanceTimer.startInterval("initialize database");
		core.prepareTables();
		if (core.getConfiguration().checkTablesChanges()) {
			PerformanceTimer.startInterval("check changes");
			MetaInfoDatabase.onCheckChanges(appContext, core);
			PerformanceTimer.endInterval();
		}
		databaseAdapter.prepare();
		core.initTablesFactory(databaseAdapter);
		initialized = true;
		PerformanceTimer.endInterval();
	}

	private void ensureInitialized() {
		checkThread();
		if (!initialized) {
			synchronized (this) {
				if (!initialized) {
					initialize();
				}
			}
		}
	}

	private static void checkThread() {
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			throw new IllegalUsageException("you can't use Database in UI thread");
		}
	}

	/**
	 * Return the {@link DatabaseAdapter} instance used by this database. Can't
	 * be called from UI thread.
	 */
	public DatabaseAdapter getDatabaseAdapter() {
		ensureInitialized();
		return databaseAdapter;
	}

	/**
	 * Returns a new {@link WritableTable} instance representing a database
	 * table. <code>modelClass</code> must be registered as a database table
	 * during the database creation. Can't be called from UI thread.
	 *
	 * @param modelClass model's class
	 */
	public <T extends Model> WritableTable<T> getTable(Class<T> modelClass) {
		ensureInitialized();
		return core.getTablesFactory().createTable(modelClass);
	}

	/**
	 * <p>
	 * Performs the transaction. The transaction is assumed successful if no
	 * exception was thrown during it. If the transaction throws any exception,
	 * it will be thrown through this method.
	 * </p>
	 * <p>
	 * Note: it is not recommended to suppress any exception during the
	 * transaction, because some framework's methods might start their own
	 * transaction and SQLite assumes the transaction successful only if all
	 * nested transaction also were successful. So, it is possible the
	 * situation, when no exception was thrown from transaction, but it failed
	 * silently.
	 * </p>
	 *
	 * @throws OperationException exception thrown by the transaction or an exception thrown
	 *                            during beginning/ending the transaction
	 */
	public void performTransaction(Transaction transaction) throws OperationException {
		ensureInitialized();
		TransactionControl transactionControl = databaseAdapter.startTransaction();
		try {
			transaction.performQueries(this);
			transactionControl.setSuccessful();
		} finally {
			transactionControl.end();
		}
	}

	/**
	 * Deletes all content of this database. Can't be called from UI thread.
	 */
	public void clear() {
		ensureInitialized();
		List<TableInfo> tables = core.getTables();
		TablesFactory factory = core.getTablesFactory();
		try {
			for (TableInfo table : tables) {
				factory.createTable(table.getOriginClass()).deleteAll();
			}
		} catch (OperationException e) {
			DatabaseLog.logException(e);
		}
	}

	/**
	 * Closes this database instance, it can't be used after this call.
	 */
	public void close() {
		if (initialized) {
			databaseAdapter.close();
		}
		DATABASES.remove(core.getDatabaseInfo().getDatabaseName());
	}

	/**
	 * Deletes the database and all stored info about it.
	 */
	public static void deleteDatabase(Context context, String databaseName) {
		checkThread();
		MetaInfoDatabase.deleteDatabaseInfo(context, databaseName);
		context.deleteDatabase(databaseName);
	}

}
