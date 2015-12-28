package handy.storage;

/**
 * Configuration for the database.
 */
class DatabaseConfiguration extends HandyStorage.Configuration {

	private boolean checkTablesChanges = false;
	private boolean enforceColumnNameConstants = false;
	private boolean setIdOnInsertByDefault = true;

	DatabaseConfiguration(HandyStorage.Configuration configuration) {
		super(configuration);
	}

	boolean enforceColumnNameConstants() {
		return enforceColumnNameConstants;
	}

	void setEnforceColumnNameConstants(boolean enforceColumnNameConstants) {
		this.enforceColumnNameConstants = enforceColumnNameConstants;
	}

	boolean checkTablesChanges() {
		return checkTablesChanges;
	}

	void setCheckTablesChanges(boolean checkTablesChanges) {
		this.checkTablesChanges = checkTablesChanges;
	}

	boolean setIdOnInsertByDefault() {
		return setIdOnInsertByDefault;
	}

	void setIdOnInsertByDefault(boolean set) {
		this.setIdOnInsertByDefault = set;
	}

}
