package handy.storage;

import java.util.List;

import handy.storage.api.Model;
import handy.storage.update.OnDatabaseUpdatePolicy;

/**
 * Information about database.
 */
class DatabaseInfo {

	private final String databaseName;
	private final int databaseVersion;
	private final OnDatabaseUpdatePolicy onUpdatePolicy;
	private final List<Class<? extends Model>> registeredModels;

	DatabaseInfo(String databaseName, int databaseVersion, OnDatabaseUpdatePolicy onUpdatePolicy,
			List<Class<? extends Model>> registeredModels) {

		this.databaseName = databaseName;
		this.databaseVersion = databaseVersion;
		this.onUpdatePolicy = onUpdatePolicy;
		this.registeredModels = registeredModels;
	}

	String getDatabaseName() {
		return databaseName;
	}

	int getDatabaseVersion() {
		return databaseVersion;
	}

	OnDatabaseUpdatePolicy getOnDatabaseUpdatePolicy() {
		return onUpdatePolicy;
	}

	List<Class<? extends Model>> getRegisteredModels() {
		return registeredModels;
	}

}
