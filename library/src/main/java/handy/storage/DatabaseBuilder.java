package handy.storage;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import handy.storage.annotation.AutoIncrement;
import handy.storage.annotation.PrimaryKey;
import handy.storage.api.Model;
import handy.storage.exception.InvalidDatabaseSchemaException;
import handy.storage.update.OnDatabaseUpdatePolicy;
import handy.storage.update.OnDatabaseUpdatePolicyFactory;

/**
 * Configures and creates {@link Database} instances. This doesn't implies any
 * heavy operations, so it can be used in UI thread.
 */
public class DatabaseBuilder {

	private final Context context;
	private final String databaseName;
	private final int databaseVersion;
	private final DatabaseConfiguration configuration;
	private final DataAdapters dataAdapters;

	private OnDatabaseUpdatePolicy dbUpdatePolicy;
	private List<Class<? extends Model>> dbTableModels = new ArrayList<>();


	DatabaseBuilder(Context context, String dbName, int dbVersion, HandyStorage.Configuration baseConfiguration, DataAdapters dataAdapters) {
		this.context = context.getApplicationContext();
		this.dataAdapters = dataAdapters;
		if (TextUtils.isEmpty(dbName)) {
			throw new IllegalArgumentException("empty database name");
		}
		if (MetaInfoDatabase.META_INFO_DATABASE_NAME.equals(dbName)) {
			throw new IllegalArgumentException("database name \"" + MetaInfoDatabase.META_INFO_DATABASE_NAME + "\" is reserved by the framework");
		}
		databaseName = dbName;
		if (dbVersion < 1) {
			throw new IllegalArgumentException("invalid database version");
		}
		databaseVersion = dbVersion;
		configuration = new DatabaseConfiguration(baseConfiguration);
	}

	/**
	 * Sets the actions to execute on database updates. For some common on
	 * update policies there are
	 * {@link OnDatabaseUpdatePolicyFactory#createNewTablesPolicy()} ,
	 * {@link OnDatabaseUpdatePolicyFactory#emptyPolicy()},
	 * {@link OnDatabaseUpdatePolicyFactory#recreateTablesPolicy()} factory methods. If
	 * the on update policy is not set, the
	 * {@link OnDatabaseUpdatePolicyFactory#recreateTablesPolicy()} is used.
	 */
	public DatabaseBuilder setOnDatabaseUpdatePolicy(OnDatabaseUpdatePolicy policy) {
		dbUpdatePolicy = policy;
		return this;
	}

	/**
	 * <p>
	 * Sets whether the framework should check if the database schema (table
	 * declarations, used enum values, etc.) is changed without increasing the
	 * database version. If this happen, a runtime exception will be thrown
	 * during the database initialisation. Repackaging of model classes doesn't
	 * matter for this check.
	 * </p>
	 * <p>
	 * It is not recommended to enable this option in release builds.
	 * </p>
	 * <p>
	 * This setting is turned off by default.
	 * </p>
	 */
	public DatabaseBuilder setCheckTablesChanges(boolean checkTablesChanges) {
		configuration.setCheckTablesChanges(checkTablesChanges);
		return this;
	}

	/**
	 * Registers a database table.
	 */
	public DatabaseBuilder addTable(Class<? extends Model> modelClass) {
		dbTableModels.add(modelClass);
		return this;
	}

	/**
	 * <p>
	 * Sets whether the framework should require declaring string constants for
	 * all column names (to avoid mistakes with typos in column names). These constants must be non-private, have value matching
	 * corresponding column's name and have name obtained by capitalising the
	 * column name's elements with optional word "COLUMN" in the end (for
	 * example, for column "myColumn1" it should be
	 * "MY_COLUMN_1" or "MY_COLUMN_1_COLUMN").
	 * </p>
	 * <p>
	 * This option is turned off by default.
	 * </p>
	 */
	public DatabaseBuilder setEnforceColumnNameConstants(boolean enforceColumnNameConstants) {
		configuration.setEnforceColumnNameConstants(enforceColumnNameConstants);
		return this;
	}

	/**
	 * <p>
	 * Sets the default value for all database tables setting, determining
	 * whether rowid for inserted object should be set as value of "id" field of
	 * this object after insertion ("id" field is a column declared as
	 * <code>long</code> and annotated with {@link PrimaryKey} and
	 * {@link AutoIncrement}). This setting can be changed independently for a
	 * concrete table instance by calling
	 * {@link WritableTable#setIdOnInsert(boolean)}.
	 * </p>
	 * <p>
	 * By default the framework always sets back rowids after insertion.
	 * </p>
	 */
	public DatabaseBuilder setIdOnInsertByDefault(boolean set) {
		configuration.setIdOnInsertByDefault(set);
		return this;
	}

	/**
	 * Builds the {@link Database} instance. The database initialisation
	 * (parsing models, check for declaration errors) will be postponed until
	 * the first call of {@link Database} methods.
	 */
	public Database build() {
		if (dbTableModels.isEmpty()) {
			throw new InvalidDatabaseSchemaException("no tables registered");
		}
		DatabaseInfo databaseInfo = new DatabaseInfo(
			databaseName,
			databaseVersion,
			dbUpdatePolicy != null ? dbUpdatePolicy : OnDatabaseUpdatePolicyFactory.recreateTablesPolicy(),
			dbTableModels);
		DatabaseCore schema = new DatabaseCore(
			databaseInfo,
			configuration,
			dataAdapters);
		return new Database(context, schema);
	}

}
