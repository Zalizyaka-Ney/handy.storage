package handy.storage.update;

import handy.storage.DatabaseBuilder;

import java.util.List;

/**
 * Handles database updates. Use
 * {@link DatabaseBuilder#setOnDatabaseUpdatePolicy(OnDatabaseUpdatePolicy)} to
 * register it in the database framework.
 */
public interface OnDatabaseUpdatePolicy {

	/**
	 * This method should return an ordered list of
	 * {@link OnDatabaseUpdateAction} objects to execute on database update.
	 * 
	 * @param oldVersion
	 *            previous database version
	 * @param newVersion
	 *            new database version
	 */
	List<OnDatabaseUpdateAction> getOnUpdateActions(int oldVersion, int newVersion);

}
